package com.englishvoicebuddy.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.englishvoicebuddy.data.PromptStore
import com.englishvoicebuddy.data.VoiceConfig
import com.englishvoicebuddy.engine.AudioEngine
import com.englishvoicebuddy.network.QwenClient
import com.englishvoicebuddy.network.QwenEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class ChatMessage {
    data class User(val text: String) : ChatMessage()
    data class AiStreaming(val text: String) : ChatMessage()
}

enum class MicState { IDLE, RECORDING, DISABLED }

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val audioEngine = AudioEngine()
    private val promptStore = PromptStore(application)
    private val _voices: List<VoiceConfig> = loadVoices()
    private var client: QwenClient? = null
    private val logFile = File(application.filesDir, "debug.log").also { it.writeText("") }

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _micState = MutableStateFlow(MicState.IDLE)
    val micState: StateFlow<MicState> = _micState

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected

    private val _showScrollBadge = MutableStateFlow(false)
    val showScrollBadge: StateFlow<Boolean> = _showScrollBadge

    private var recordingJob: Job? = null
    private val audioChunks = mutableListOf<ByteArray>()

    // 音频播放器
    private var audioQueue = Channel<ByteArray>(Channel.UNLIMITED)
    private var audioPlayerJob: Job? = null
    private var audioQueueClosed = false

    // AI 文本缓冲 + 定时刷新
    private var pendingTranscript = ""
    private var lastTranscriptFlush = 0L

    // 本轮 AI 气泡在消息列表中的位置（-1 = 未创建）
    private var currentAiIndex = -1
    // 用户气泡占位符位置（-1 = 未创建）。stopRecording 时立刻插入，保证用户消息最先上屏
    private var placeholderIndex = -1

    private var isAiResponding = false
    private var lastRecordingStart = 0L

    init {
        flog("init hasKey=${hasApiKey()}")
        if (hasApiKey()) connectToQwen()
    }

    private fun flog(msg: String) {
        try {
            val ts = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
            logFile.appendText("$ts $msg\n")
        } catch (_: Exception) {}
    }

    fun hasApiKey(): Boolean = promptStore.getApiKey() != null
    fun getApiKey(): String = promptStore.getApiKey().orEmpty()
    fun getPrompt(): String = promptStore.getPrompt()
    fun getModel(): String = promptStore.getModel()
    fun getVoice(): String = promptStore.getVoice()
    fun getVoices(): List<VoiceConfig> = _voices

    fun connect() { client?.disconnect(); connectToQwen() }

    private fun loadVoices(): List<VoiceConfig> {
        return try {
            val json = getApplication<Application>().assets
                .open("qwen-omni-voices.json")
                .bufferedReader()
                .use { it.readText() }
            val arr = JSONObject(json).getJSONArray("voices")
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                VoiceConfig(
                    voice = obj.getString("voice"),
                    name = obj.getString("name"),
                    description = obj.getString("description"),
                    accent = obj.optString("accent", null)?.takeIf { it.isNotEmpty() && it != "null" },
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun connectToQwen() {
        val apiKey = promptStore.getApiKey() ?: run { _connected.value = false; return }
        try {
            client = QwenClient(apiKey, promptStore.getPrompt(), logFile, promptStore.getModel(), promptStore.getVoice()).also { c ->
                c.connect()
                viewModelScope.launch { for (event in c.events) handleEvent(event) }
            }
        } catch (e: Exception) {
            _connected.value = false
        }
    }

    private fun handleEvent(event: QwenEvent) {
        try {
            when (event) {
                is QwenEvent.Connected -> _connected.value = true
                is QwenEvent.Disconnected -> _connected.value = false

                is QwenEvent.TranscriptDelta -> {
                    pendingTranscript += event.text
                    val now = System.currentTimeMillis()
                    if (now - lastTranscriptFlush >= 100) {
                        lastTranscriptFlush = now
                        flushAiText()
                    }
                }

                is QwenEvent.TranscriptDone -> {
                    flushAiText()
                }

                is QwenEvent.UserTranscript -> {
                    flushAiText()
                    val list = _messages.value.toMutableList()
                    val userMsg = ChatMessage.User(event.text)
                    if (placeholderIndex >= 0 && placeholderIndex < list.size) {
                        list[placeholderIndex] = userMsg
                        placeholderIndex = -1
                    } else {
                        list.add(userMsg)
                    }
                    _messages.value = list
                }

                is QwenEvent.AudioDelta -> {
                    audioQueue.trySend(event.data)
                    ensureAudioPlayer()
                }

                is QwenEvent.AudioDone -> {
                    if (!audioQueueClosed) {
                        audioQueueClosed = true
                        audioQueue.close()
                    }
                }

                is QwenEvent.Error -> {
                    val msg = event.message
                    if (msg.contains("none active response") || msg.contains("no active response")) return
                    stopAudioPlayer()
                    val list = _messages.value.toMutableList()
                    list.add(ChatMessage.AiStreaming("[${msg}]"))
                    _messages.value = list
                    _micState.value = MicState.IDLE
                    isAiResponding = false
                }

                else -> {} // ResponseCreated, ResponseDone 等不再需要
            }
        } catch (e: Exception) {
            flog("handleEvent: ${e.message}")
            _micState.value = MicState.IDLE
            isAiResponding = false
        }
    }

    private fun flushAiText() {
        val text = pendingTranscript
        if (text.isEmpty()) return
        pendingTranscript = ""
        val list = _messages.value.toMutableList()
        if (currentAiIndex >= 0 && currentAiIndex < list.size) {
            val existing = list[currentAiIndex]
            if (existing is ChatMessage.AiStreaming) {
                list[currentAiIndex] = ChatMessage.AiStreaming(existing.text + text)
            } else {
                list.add(ChatMessage.AiStreaming(text))
                currentAiIndex = list.lastIndex
            }
        } else {
            list.add(ChatMessage.AiStreaming(text))
            currentAiIndex = list.lastIndex
        }
        _messages.value = list
    }

    private fun ensureAudioPlayer() {
        if (audioPlayerJob?.isActive == true) return
        audioPlayerJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                audioEngine.initPlayer()
                for (chunk in audioQueue) audioEngine.playChunk(chunk)
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) flog("player: ${e.message}")
            } finally {
                kotlinx.coroutines.delay(150) // 等 AudioTrack 硬件缓冲区排空
                try { audioEngine.release() } catch (_: Exception) {}
                audioPlayerJob = null
                _micState.value = MicState.IDLE
                isAiResponding = false
            }
        }
    }

    private fun stopAudioPlayer() {
        audioPlayerJob?.cancel()
        audioPlayerJob = null
        if (!audioQueueClosed) { audioQueueClosed = true; audioQueue.cancel() }
    }

    private fun rebuildAudioQueue() {
        stopAudioPlayer()
        audioQueue = Channel(Channel.UNLIMITED)
        audioQueueClosed = false
    }

    // ── 公开接口 ──

    fun startRecording() {
        val now = System.currentTimeMillis()
        if (now - lastRecordingStart < 300) return
        lastRecordingStart = now

        // 打断：取消服务端响应，停止播放，重置状态
        if (isAiResponding) {
            flushAiText()
            try { client?.cancelResponse() } catch (_: Exception) {}
            stopAudioPlayer()
            rebuildAudioQueue()
            isAiResponding = false
            _micState.value = MicState.IDLE
        }

        if (_micState.value != MicState.IDLE) return

        if (!hasApiKey()) {
            val list = _messages.value.toMutableList()
            list.add(ChatMessage.AiStreaming("请先点击右上角齿轮设置 API Key"))
            _messages.value = list
            return
        }

        _micState.value = MicState.RECORDING
        audioChunks.clear()
        pendingTranscript = ""
        currentAiIndex = -1
        placeholderIndex = -1

        recordingJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                audioEngine.startRecording().collect { chunk ->
                    audioChunks.add(chunk)
                    client?.sendAudio(chunk)
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) flog("rec: ${e.message}")
            }
        }
    }

    fun stopRecording() {
        if (_micState.value != MicState.RECORDING) return

        recordingJob?.cancel()
        recordingJob = null

        if (client != null && audioChunks.isNotEmpty()) {
            // 立刻插入用户气泡占位符（比任何服务端事件都早）
            val list = _messages.value.toMutableList()
            list.add(ChatMessage.User("..."))
            placeholderIndex = list.lastIndex
            _messages.value = list

            rebuildAudioQueue()
            currentAiIndex = -1
            isAiResponding = true
            _micState.value = MicState.DISABLED
            try { client?.commitAndRespond() } catch (_: Exception) {}
        } else {
            rebuildAudioQueue()
            _micState.value = MicState.IDLE
        }
    }

    fun onUserScrolledUp() { _showScrollBadge.value = true }
    fun onScrollToBottom() { _showScrollBadge.value = false }

    fun saveSettings(apiKey: String, model: String, voice: String, prompt: String) {
        promptStore.saveApiKey(apiKey)
        promptStore.saveModel(model)
        promptStore.saveVoice(voice)
        promptStore.savePrompt(prompt)
        client?.disconnect()
        connectToQwen()
    }

    fun resetPrompt() {
        promptStore.resetToDefault()
        client?.disconnect()
        connectToQwen()
    }

    override fun onCleared() {
        super.onCleared()
        stopAudioPlayer()
        recordingJob?.cancel()
        try { client?.disconnect() } catch (_: Exception) {}
        try { audioEngine.release() } catch (_: Exception) {}
    }
}
