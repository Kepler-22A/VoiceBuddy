package com.englishvoicebuddy.network

import android.util.Base64
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

sealed class QwenEvent {
    data class TranscriptDelta(val text: String) : QwenEvent()
    data class TranscriptDone(val text: String) : QwenEvent()
    data class UserTranscript(val text: String) : QwenEvent()
    data class AudioDelta(val data: ByteArray) : QwenEvent()
    data class AudioDone(val transcript: String) : QwenEvent()
    data class Error(val message: String) : QwenEvent()
    data object Connected : QwenEvent()
    data object Disconnected : QwenEvent()
    data object ResponseCreated : QwenEvent()  // 服务端开始生成响应
    data object ResponseDone : QwenEvent()     // 服务端响应完成
}

class QwenClient(
    private val apiKey: String,
    private val instructions: String,
    private val logFile: File,
    model: String = "qwen3.5-omni-plus-realtime",
) {
    private val url = "wss://dashscope.aliyuncs.com/api-ws/v1/realtime?model=$model"

    private var ws: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    val events = Channel<QwenEvent>(UNLIMITED)

    private fun flog(msg: String) {
        try {
            val ts = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
            logFile.appendText("$ts [WS] $msg\n")
        } catch (_: Exception) {}
    }

    fun connect() {
        flog("connecting to $url")
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .build()

        ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                flog("onOpen")
                events.trySend(QwenEvent.Connected)

                val sessionConfig = """{"type":"session.update","session":{"modalities":["text","audio"],"voice":"Ethan","instructions":${JSONObject.quote(instructions)},"input_audio_format":"pcm16","output_audio_format":"pcm16","input_audio_transcription":{"model":null},"turn_detection":null}}"""
                webSocket.send(sessionConfig)
                flog("session.update sent")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val event = JSONObject(text)
                    val type = event.getString("type")
                    if (type != "response.audio_transcript.delta" && type != "response.audio.delta") {
                        flog("recv: $type")
                    }

                    when (type) {
                        "response.audio_transcript.delta" ->
                            events.trySend(QwenEvent.TranscriptDelta(event.getString("delta")))
                        "response.audio_transcript.done" ->
                            events.trySend(QwenEvent.TranscriptDone(event.getString("transcript")))
                        "conversation.item.input_audio_transcription.completed" ->
                            events.trySend(QwenEvent.UserTranscript(event.getString("transcript")))
                        "response.audio.delta" -> {
                            val bytes = Base64.decode(event.getString("delta"), Base64.DEFAULT)
                            events.trySend(QwenEvent.AudioDelta(bytes))
                        }
                        "response.done" -> {
                            events.trySend(QwenEvent.AudioDone(""))
                        }
                        "session.created", "session.updated" -> {}
                        "response.created" -> events.trySend(QwenEvent.ResponseCreated)
                        "response.done" -> events.trySend(QwenEvent.ResponseDone)
                        "error" -> {
                            val full = text.take(300)
                            flog("SERVER ERROR: $full")
                            val msg = event.optJSONObject("error")?.let { err ->
                                "${err.optString("type", "")}: ${err.optString("message", err.toString())}"
                            } ?: "未知错误"
                            events.trySend(QwenEvent.Error(msg))
                        }
                        else -> flog("unhandled: $type")
                    }
                } catch (e: Exception) {
                    flog("parse error: ${e.message} raw=${text.take(100)}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                flog("onFailure: ${t.message} code=${response?.code} body=${response?.body?.string()?.take(200)}")
                events.trySend(QwenEvent.Error(t.message ?: "连接失败"))
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                flog("onClosed code=$code reason=$reason")
                events.trySend(QwenEvent.Disconnected)
            }
        })
    }

    fun sendAudio(data: ByteArray) {
        val event = JSONObject().apply {
            put("type", "input_audio_buffer.append")
            put("audio", Base64.encodeToString(data, Base64.NO_WRAP))
        }
        ws?.send(event.toString())
    }

    fun commitAndRespond() {
        flog("commitAndRespond")
        ws?.send("""{"type":"input_audio_buffer.commit"}""")
        ws?.send("""{"type":"response.create","response":{}}""")
    }

    fun cancelResponse() {
        ws?.send("""{"type":"response.cancel"}""")
    }

    fun disconnect() {
        flog("disconnect")
        ws?.close(1000, "user disconnect")
        ws = null
    }
}
