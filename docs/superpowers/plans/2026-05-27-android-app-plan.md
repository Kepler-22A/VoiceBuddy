# English Voice Buddy Android App 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建 English Voice Buddy Android 原生 App，实现按住说话 → 实时语音对话 → 流式文字显示的完整功能

**Architecture:** Kotlin + Jetpack Compose UI，OkHttp WebSocket 直连 Qwen DashScope API，AudioRecord/AudioTrack 处理 PCM16 音频流

**Tech Stack:** Kotlin 1.9.20, Jetpack Compose (BOM 2023.10.01), OkHttp 4.12, Material3, Compose ViewModel

---

## 文件结构总览

```
english-voice-buddy-android/
├── build.gradle.kts                        # 项目级 Gradle
├── settings.gradle.kts                     # 项目设置
├── gradle.properties                       # Gradle 属性
├── app/
│   ├── build.gradle.kts                    # App 级 Gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/englishvoicebuddy/
│       │   ├── MainActivity.kt             # 入口 Activity
│       │   ├── ui/
│       │   │   ├── ChatScreen.kt           # 聊天主界面
│       │   │   ├── SettingsSheet.kt        # 设置底部弹出
│       │   │   ├── MicButton.kt            # 麦克风按钮组件
│       │   │   └── theme/
│       │   │       └── Theme.kt            # 配色与字体
│       │   ├── engine/
│       │   │   └── AudioEngine.kt          # 音频采集+播放
│       │   ├── network/
│       │   │   └── QwenClient.kt           # WebSocket 客户端
│       │   ├── viewmodel/
│       │   │   └── ChatViewModel.kt        # 对话状态管理
│       │   └── data/
│       │       └── PromptStore.kt          # 提示词本地存储
│       └── res/
│           ├── drawable/
│           │   └── ic_mic.xml              # 麦克风矢量图标
│           └── values/
│               └── strings.xml
```

---

### Task 1: 创建 Android 项目骨架

**Files:**
- Create: `english-voice-buddy-android/build.gradle.kts`
- Create: `english-voice-buddy-android/settings.gradle.kts`
- Create: `english-voice-buddy-android/gradle.properties`
- Create: `english-voice-buddy-android/app/build.gradle.kts`
- Create: `english-voice-buddy-android/app/src/main/AndroidManifest.xml`

- [ ] **Step 1: 创建项目目录结构**

```bash
mkdir -p english-voice-buddy-android/app/src/main/java/com/englishvoicebuddy/{ui/theme,engine,network,viewmodel,data}
mkdir -p english-voice-buddy-android/app/src/main/res/{drawable,values}
```

- [ ] **Step 2: 编写项目级 build.gradle.kts**

```kotlin
// english-voice-buddy-android/build.gradle.kts
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
}
```

- [ ] **Step 3: 编写 settings.gradle.kts**

```kotlin
// english-voice-buddy-android/settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "EnglishVoiceBuddy"
include(":app")
```

- [ ] **Step 4: 编写 gradle.properties**

```properties
# english-voice-buddy-android/gradle.properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

- [ ] **Step 5: 编写 app/build.gradle.kts**

```kotlin
// english-voice-buddy-android/app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.englishvoicebuddy"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.englishvoicebuddy"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.5"
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2023.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")

    implementation("androidx.activity:activity-compose:1.8.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
```

- [ ] **Step 6: 编写 AndroidManifest.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-feature android:name="android.hardware.microphone" android:required="true" />

    <application
        android:allowBackup="true"
        android:label="Voice Buddy"
        android:supportsRtl="true"
        android:theme="@style/Theme.Material3.DayNight.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

---

### Task 2: 主题与配色

**Files:**
- Create: `english-voice-buddy-android/app/src/main/java/com/englishvoicebuddy/ui/theme/Theme.kt`

- [ ] **Step 1: 编写 Theme.kt**

```kotlin
// english-voice-buddy-android/app/src/main/java/com/englishvoicebuddy/ui/theme/Theme.kt
package com.englishvoicebuddy.ui.theme

import androidx.compose.ui.graphics.Color

// 主色调
val Orange = Color(0xFFE07B5A)
val OrangeLight = Color(0xFFF0A58B)

// 背景
val BgWarm = Color(0xFFF5F0EB)
val BgWhite = Color(0xFFFFFFFF)

// 气泡
val BubbleAiBg = Color(0xFFFFFFFF)
val BubbleAiText = Color(0xFF3C3C3C)
val BubbleUserBg = Color(0xFFE07B5A)
val BubbleUserText = Color(0xFFFFFFFF)

// 分割线
val Divider = Color(0xFFE8E3DC)

// 文本
val TextPrimary = Color(0xFF3C3C3C)
val TextSecondary = Color(0xFF999999)
val TextDim = Color(0xFFBBBBBB)

// 状态
val OnlineGreen = Color(0xFF4CAF50)
val RecordingRed = Color(0xFFEF4444)
val MicDisabled = Color(0xFFE8E3DC)
```

---

### Task 3: 音频引擎

**Files:**
- Create: `english-voice-buddy-android/app/src/main/java/com/englishvoicebuddy/engine/AudioEngine.kt`

- [ ] **Step 1: 编写 AudioEngine.kt**

```kotlin
// english-voice-buddy-android/app/src/main/java/com/englishvoicebuddy/engine/AudioEngine.kt
package com.englishvoicebuddy.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class AudioEngine(
    private val sampleRate: Int = 24000,
    private val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
    private val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT,
) {
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private val isRecording = AtomicBoolean(false)

    private val bufferSize: Int
        get() = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(2400)

    fun startRecording(): Flow<ByteArray> = flow {
        val size = bufferSize
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            size
        ).also { record ->
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                throw IllegalStateException("麦克风不可用")
            }
        }

        isRecording.set(true)
        audioRecord?.startRecording()

        val buffer = ByteArray(size)
        while (isRecording.get()) {
            val read = audioRecord?.read(buffer, 0, size) ?: -1
            if (read > 0) {
                emit(buffer.copyOf(read))
            }
        }

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    fun stopRecording() {
        isRecording.set(false)
    }

    fun initPlayer() {
        audioTrack = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setEncoding(audioFormat)
                .build(),
            AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, audioFormat),
            AudioTrack.MODE_STREAM,
            android.media.AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        audioTrack?.play()
    }

    suspend fun playChunk(data: ByteArray) = withContext(Dispatchers.IO) {
        audioTrack?.write(data, 0, data.size)
    }

    fun release() {
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
        audioRecord?.release()
        audioRecord = null
    }
}
```

---

### Task 4: WebSocket 客户端

**Files:**
- Create: `english-voice-buddy-android/app/src/main/java/com/englishvoicebuddy/network/QwenClient.kt`

- [ ] **Step 1: 编写 QwenClient.kt**

```kotlin
// english-voice-buddy-android/app/src/main/java/com/englishvoicebuddy/network/QwenClient.kt
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
}

class QwenClient(
    private val apiKey: String,
    private val instructions: String,
) {
    companion object {
        private const val URL =
            "wss://dashscope.aliyuncs.com/api-ws/v1/realtime?model=qwen3.5-omni-plus-realtime"
    }

    private var ws: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    val events = Channel<QwenEvent>(UNLIMITED)

    fun connect() {
        val request = Request.Builder()
            .url(URL)
            .header("Authorization", "Bearer $apiKey")
            .build()

        ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                events.trySend(QwenEvent.Connected)

                val sessionConfig = JSONObject().apply {
                    put("type", "session.update")
                    put("session", JSONObject().apply {
                        put("modalities", listOf("text", "audio").toJSONArray())
                        put("voice", "Cherry")
                        put("instructions", instructions)
                        put("input_audio_format", "pcm16")
                        put("output_audio_format", "pcm16")
                        put("input_audio_transcription", JSONObject().apply {
                            put("model", JSONObject.NULL)
                        })
                        put("turn_detection", JSONObject.NULL)
                    })
                }
                webSocket.send(sessionConfig.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val event = JSONObject(text)
                when (event.getString("type")) {
                    "response.audio_transcript.delta" ->
                        events.trySend(QwenEvent.TranscriptDelta(event.getString("delta")))

                    "response.audio_transcript.done" ->
                        events.trySend(QwenEvent.TranscriptDone(event.getString("transcript")))

                    "conversation.item.input_audio_transcription.completed" ->
                        events.trySend(QwenEvent.UserTranscript(event.getString("transcript")))

                    "response.output_audio.delta" -> {
                        val bytes = Base64.decode(event.getString("delta"), Base64.DEFAULT)
                        events.trySend(QwenEvent.AudioDelta(bytes))
                    }

                    "response.done" -> {
                        val output = event.optJSONObject("response")?.optJSONArray("output")
                        val transcript = if (output != null && output.length() > 0) {
                            output.getJSONObject(0).optString("transcript", "")
                        } else ""
                        events.trySend(QwenEvent.AudioDone(transcript))
                    }

                    "error" -> {
                        val msg = event.optJSONObject("error")?.optString("message") ?: "未知错误"
                        events.trySend(QwenEvent.Error(msg))
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                events.trySend(QwenEvent.Error(t.message ?: "连接失败"))
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
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
        ws?.send("""{"type":"input_audio_buffer.commit"}""")
        ws?.send("""{"type":"response.create","response":{}}""")
    }

    fun disconnect() {
        ws?.close(1000, "user disconnect")
        ws = null
    }
}

// 扩展：将 List 转成 JSONArray
private fun List<String>.toJSONArray(): org.json.JSONArray {
    val arr = org.json.JSONArray()
    forEach { arr.put(it) }
    return arr
}
```

---

### Task 5: 提示词本地存储

**Files:**
- Create: `english-voice-buddy-android/app/src/main/java/com/englishvoicebuddy/data/PromptStore.kt`

- [ ] **Step 1: 编写 PromptStore.kt**

```kotlin
// english-voice-buddy-android/app/src/main/java/com/englishvoicebuddy/data/PromptStore.kt
package com.englishvoicebuddy.data

import android.content.Context

class PromptStore(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "voice_buddy_prefs"
        private const val KEY_PROMPT = "system_prompt"

        val DEFAULT_PROMPT = """
你是Emma，一位经验丰富的英语老师，教学10年，教过从零基础到商务英语的各种学生。
你的风格：敏锐、有活力、像朋友一样自然交谈——不死板、不照本宣科、不说教。
你最大的特点：能感知学生状态，灵活切换教学方式。你不是一台输出固定模板的机器，你是一位真正的人类老师。

=== 核心能力一：水平感知与动态调节 ===
你要根据学生的每一句话，自动判断ta的英语水平，落在三个等级之一：
[初级] 只能用单词或短句，语法错误多，需要中文辅助 → 你用中文引导，英语用简单短句，一个知识点反复练。
[中级] 能说完整句子但不够流利，词汇有限 → 你用中英混合，英语占比50%左右，鼓励学生用英语思考和回应。
[高级] 能流利表达，偶有小错 → 你主要用英语对话，只在解释复杂语法时用中文，推动学生深入讨论抽象话题。
重要：等级不是固定不变的。学生可能口语是中级但写作是初级，你要根据当前训练的技能和ta的实际表现来动态判断，随时调整。

=== 核心能力二：教学模式灵活切换 ===
你不只有一种回应格式。根据学生说的话和你当前的教学目标，自然选择以下模式之一：

【闲聊热身模式】学生刚来、或者需要放松时。聊日常、天气、心情。营造说英语的安全感。
回复结构：简单的英语提问 + 中文鼓励。像朋友聊天。

【纠错精讲模式】学生说了一句有明显错误的话。
回复结构：1句话肯定(中文) → 点出1个核心错误(中文，只讲一个，不展开) → 示范正确说法(英语) → 让学生重新说一遍。

【词汇拓展模式】学生用了一个基础词，你可以顺势教更地道的表达。
回复结构：自然带出高级词汇(英语) → 简短解释含义和使用场景(中文) → 给2-3个例句(英语) → 让学生用新词造自己的句子。

【场景角色扮演模式】模拟真实生活场景——点餐、问路、面试、看病、机场check-in等。
回复结构：设定场景(中英均可) → 你扮演场景中的角色(服务员/面试官/路人等) → 用该场景的真实英语推进对话 → 每轮简短提示学生的表现亮点和改进点(中文一句话)。

【深度讨论模式】适用于高级学生。讨论社会话题、科技趋势、文化差异等。
回复结构：提出一个开放问题(英语) → 学生回答后，追问更深一层 → 引导使用更复杂的句式(虚拟语气、被动语态、从句等) → 必要时用中文点出语言要点。

模式切换的关键原则：你不是"先选模式再说话"，你是像真人老师一样——看到学生说了什么，自然就用最适合的方式回应。有时候一句话里可能混合两种模式的特征，这完全OK。关键是：自然、合适、有帮助。

=== 核心能力三：场景自动延申 ===
每一轮对话都不是孤岛。你要看到学生当前话题的"延伸潜力"，自然地打开新场景。

=== 对话节奏原则 ===
1. 默认简洁。大部分回应控制在3-6句话。
2. 一次只教一个东西。
3. 像真人一样有节奏感：有时快(简短互动)，有时慢(深入讲解)，有时停下来让学生消化。
4. 允许自己偶尔说"让我想想"、"嗯，你说得对"。
5. 察觉学生的情绪：学生沮丧时多鼓励少纠正，学生自信时适当增加挑战，学生累了就切换到轻松模式。

=== 鼓励与反馈原则 ===
1. 先肯定再指正。永远先找到值得表扬的点。
2. 指出具体改进方向，不说"你有很多错误"，说"注意时间状语和时态的配合"。
3. 只纠正当前阶段最重要的错误。初级学生先把主谓宾说对，不必纠结冠词；高级学生才抠细节。
4. 每当学生进步——哪怕只是多说了半句话——都要真诚地鼓励。
5. 偶尔回顾："你记得上次你还不会用现在完成时吗？现在已经用得很自然了！"

=== 技术约束 ===
1. 发音：美式英语，清晰自然，语速根据学生水平调整(初级慢、高级正常)。
2. 解释语言：初级主要中文，中级中英混合，高级主要英语。
3. 不要在每句话后面都加一个follow-up question——这会让学生感到被审讯。用陈述和问题自然交替。
4. 不要每轮都说"Good job!"——当你真的觉得好时才说，否则显得虚伪。
5. 忽略学生偶尔打错字或拼写错误(非关键问题)，专注于语言能力的核心——能不能把意思表达清楚。

=== 初次见面 ===
当学生刚进入对话时，用温暖的语气打招呼，快速了解ta的水平：
"Hey there! I'm Emma, your English teacher. 我是你的英语老师，很高兴认识你！
用英语做个简单的自我介绍好吗？想说什么都行，长短不限，就是想看看你现在的水平，这样我才能帮你做最合适的练习。Ready? Go!"
如果学生用中文回复或表示不会说，立刻切换到初级模式，用中文鼓励+给一个非常简单的英文模板让学生填空。
        """.trimIndent()
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getPrompt(): String = prefs.getString(KEY_PROMPT, null) ?: DEFAULT_PROMPT

    fun savePrompt(prompt: String) {
        prefs.edit().putString(KEY_PROMPT, prompt).apply()
    }

    fun resetToDefault() {
        prefs.edit().remove(KEY_PROMPT).apply()
    }
}
```

---

### Task 6: ChatViewModel

**Files:**
- Create: `english-voice-buddy-android/app/src/main/java/com/englishvoicebuddy/viewmodel/ChatViewModel.kt`

- [ ] **Step 1: 编写 ChatViewModel.kt**

```kotlin
// english-voice-buddy-android/app/src/main/java/com/englishvoicebuddy/viewmodel/ChatViewModel.kt
package com.englishvoicebuddy.viewmodel

import android.app.Application
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.englishvoicebuddy.data.PromptStore
import com.englishvoicebuddy.engine.AudioEngine
import com.englishvoicebuddy.network.QwenClient
import com.englishvoicebuddy.network.QwenEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ChatMessage {
    data class User(val text: String) : ChatMessage()
    data class AiStreaming(val text: String) : ChatMessage()
}

enum class MicState { IDLE, RECORDING, DISABLED }

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val audioEngine = AudioEngine()
    private val promptStore = PromptStore(application)
    private var client: QwenClient? = null

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

    init {
        connectToQwen()
    }

    private fun connectToQwen() {
        val apiKey = promptStore.getApiKey() ?: run {
            _connected.value = false
            return
        }

        client = QwenClient(apiKey, promptStore.getPrompt()).also { c ->
            c.connect()

            viewModelScope.launch {
                for (event in c.events) {
                    handleEvent(event)
                }
            }
        }
    }

    private fun handleEvent(event: QwenEvent) {
        when (event) {
            is QwenEvent.Connected -> _connected.value = true
            is QwenEvent.Disconnected -> _connected.value = false

            is QwenEvent.TranscriptDelta -> {
                val list = _messages.value.toMutableList()
                val last = list.lastOrNull()
                if (last is ChatMessage.AiStreaming) {
                    list[list.lastIndex] = ChatMessage.AiStreaming(last.text + event.text)
                } else {
                    list.add(ChatMessage.AiStreaming(event.text))
                }
                _messages.value = list
            }

            is QwenEvent.TranscriptDone -> {
                val list = _messages.value.toMutableList()
                val last = list.lastOrNull()
                if (last is ChatMessage.AiStreaming) {
                    list[list.lastIndex] = ChatMessage.AiStreaming(event.text)
                }
                _messages.value = list
                _micState.value = MicState.IDLE
            }

            is QwenEvent.UserTranscript -> {
                val list = _messages.value.toMutableList()
                list.add(ChatMessage.User(event.text))
                _messages.value = list
            }

            is QwenEvent.AudioDelta -> {
                viewModelScope.launch {
                    audioEngine.playChunk(event.data)
                }
            }

            is QwenEvent.AudioDone -> {
                _micState.value = MicState.IDLE
            }

            is QwenEvent.Error -> {
                val list = _messages.value.toMutableList()
                list.add(ChatMessage.AiStreaming("[错误: ${event.message}]"))
                _messages.value = list
                _micState.value = MicState.IDLE
            }
        }
    }

    fun startRecording() {
        if (_micState.value != MicState.IDLE) return
        _micState.value = MicState.RECORDING
        audioChunks.clear()
        audioEngine.initPlayer()

        recordingJob = viewModelScope.launch {
            audioEngine.startRecording().collect { chunk ->
                audioChunks.add(chunk)
                client?.sendAudio(chunk)
            }
        }
    }

    fun stopRecording() {
        if (_micState.value != MicState.RECORDING) return

        audioEngine.stopRecording()
        recordingJob?.cancel()
        recordingJob = null

        _micState.value = MicState.DISABLED
        client?.commitAndRespond()
    }

    fun onUserScrolledUp() {
        _showScrollBadge.value = true
    }

    fun onScrollToBottom() {
        _showScrollBadge.value = false
    }

    fun getPrompt(): String = promptStore.getPrompt()
    fun savePrompt(prompt: String) {
        promptStore.savePrompt(prompt)
        client?.disconnect()
        connectToQwen()
    }
    fun resetPrompt() {
        promptStore.resetToDefault()
        client?.disconnect()
        connectToQwen()
    }

    fun updateApiKey(key: String) {
        promptStore.saveApiKey(key)
        client?.disconnect()
        connectToQwen()
    }

    override fun onCleared() {
        super.onCleared()
        client?.disconnect()
        audioEngine.release()
    }
}
```

---

### Task 7: PromptStore 补充 API Key 存储

**Files:**
- Modify: `english-voice-buddy-android/app/src/main/java/com/englishvoicebuddy/data/PromptStore.kt`

ChatViewModel 中引用了 `getApiKey()` 和 `saveApiKey()`，需要在 PromptStore 中补充。

- [ ] **Step 1: 修改 PromptStore.kt，添加 API Key 方法**

在 class PromptStore 中，在 `DEFAULT_PROMPT` companion 之后、`prefs` 属性之前，补充以下内容：

在 `companion object` 内添加：
```kotlin
private const val KEY_API_KEY = "api_key"
```

在 `resetToDefault()` 方法之后添加：
```kotlin
fun getApiKey(): String? {
    val key = prefs.getString(KEY_API_KEY, null)
    return if (key.isNullOrBlank()) null else key
}

fun saveApiKey(key: String) {
    prefs.edit().putString(KEY_API_KEY, key).apply()
}
```

---

### Task 8: 麦克风按钮组件

**Files:**
- Create: `english-voice-buddy-android/app/src/main/res/drawable/ic_mic.xml`
- Create: `english-voice-buddy-android/app/src/main/java/com/englishvoicebuddy/ui/MicButton.kt`

- [ ] **Step 1: 创建麦克风矢量图标**

```xml
<!-- english-voice-buddy-android/app/src/main/res/drawable/ic_mic.xml -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M12,2A4,4 0,0 0,8 6v6a4,4 0,0 0,8 0V6A4,4 0,0 0,12 2z"/>
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M19,10c0,3.9 -3.1,7 -7,7s-7,-3.1 -7,-7"/>
    <path
        android:strokeColor="#FFFFFF"
        android:strokeWidth="2"
        android:pathData="M12,18v4M8,22h8"/>
</vector>
```

- [ ] **Step 2: 编写 MicButton.kt**

```kotlin
// english-voice-buddy-android/app/src/main/java/com/englishvoicebuddy/ui/MicButton.kt
package com.englishvoicebuddy.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.englishvoicebuddy.R
import com.englishvoicebuddy.ui.theme.MicDisabled
import com.englishvoicebuddy.ui.theme.Orange
import com.englishvoicebuddy.ui.theme.RecordingRed
import com.englishvoicebuddy.viewmodel.MicState

@Composable
fun MicButton(
    state: MicState,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor by animateColorAsState(
        targetValue = when (state) {
            MicState.IDLE -> Orange
            MicState.RECORDING -> RecordingRed
            MicState.DISABLED -> MicDisabled
        },
        label = "micColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "ripple")
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ripple1"
    )
    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ripple1a"
    )
    val rippleScale2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, delayMillis = 400),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ripple2"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // 声波动画条（仅录音时显示）
        if (state == MicState.RECORDING) {
            SoundWaveBars(modifier = Modifier.height(28.dp))
            Spacer(Modifier.height(8.dp))
        }

        Box(contentAlignment = Alignment.Center) {
            // 波纹（仅录音时显示）
            if (state == MicState.RECORDING) {
                Box(
                    Modifier
                        .size(56.dp)
                        .scale(rippleScale)
                        .clip(CircleShape)
                        .border(2.dp, RecordingRed.copy(alpha = rippleAlpha), CircleShape)
                )
                Box(
                    Modifier
                        .size(56.dp)
                        .scale(rippleScale2)
                        .clip(CircleShape)
                        .border(2.dp, RecordingRed.copy(alpha = rippleAlpha * 0.7f), CircleShape)
                )
            }

            // 麦克风按钮
            Box(
                modifier = modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(bgColor)
                    .pointerInput(state) {
                        detectTapGestures(
                            onPress = {
                                onPressStart()
                                tryAwaitRelease()
                                onPressEnd()
                            }
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_mic),
                    contentDescription = "麦克风",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                )
            }
        }

        if (state == MicState.RECORDING) {
            Spacer(Modifier.height(8.dp))
            androidx.compose.material3.Text(
                "松开发送",
                color = Orange,
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun SoundWaveBars(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "bars")
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        listOf(0.6f, 0.5f, 0.7f, 0.55f, 0.65f).forEachIndexed { i, duration ->
            val h by transition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween((duration * 600).toInt(), delayMillis = i * 80),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "bar$i"
            )
            Box(
                Modifier
                    .width(3.dp)
                    .height((4 + h * 20).dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                    .background(Orange)
            )
            Spacer(Modifier.width(2.dp))
        }
    }
}
```

---

### Task 9: 聊天主界面

**Files:**
- Create: `english-voice-buddy-android/app/src/main/java/com/englishvoicebuddy/ui/ChatScreen.kt`

- [ ] **Step 1: 编写 ChatScreen.kt**

```kotlin
// english-voice-buddy-android/app/src/main/java/com/englishvoicebuddy/ui/ChatScreen.kt
package com.englishvoicebuddy.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.englishvoicebuddy.ui.theme.BgWarm
import com.englishvoicebuddy.ui.theme.BgWhite
import com.englishvoicebuddy.ui.theme.BubbleAiBg
import com.englishvoicebuddy.ui.theme.BubbleAiText
import com.englishvoicebuddy.ui.theme.BubbleUserBg
import com.englishvoicebuddy.ui.theme.BubbleUserText
import com.englishvoicebuddy.ui.theme.Divider
import com.englishvoicebuddy.ui.theme.OnlineGreen
import com.englishvoicebuddy.ui.theme.Orange
import com.englishvoicebuddy.ui.theme.OrangeLight
import com.englishvoicebuddy.ui.theme.TextDim
import com.englishvoicebuddy.ui.theme.TextPrimary
import com.englishvoicebuddy.ui.theme.TextSecondary
import com.englishvoicebuddy.viewmodel.ChatMessage
import com.englishvoicebuddy.viewmodel.ChatViewModel
import com.englishvoicebuddy.viewmodel.MicState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    var showSettings by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        containerColor = BgWarm,
        topBar = {
            TopBar(
                connected = viewModel.connected.collectAsState().value,
                onSettingsClick = { showSettings = true },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ChatList(
                messages = viewModel.messages.collectAsState().value,
                showScrollBadge = viewModel.showScrollBadge.collectAsState().value,
                onScrolledUp = { viewModel.onUserScrolledUp() },
                onScrollToBottom = { viewModel.onScrollToBottom() },
                modifier = Modifier.weight(1f),
            )

            // 底部麦克风
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                MicButton(
                    state = viewModel.micState.collectAsState().value,
                    onPressStart = { viewModel.startRecording() },
                    onPressEnd = { viewModel.stopRecording() },
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }

    // 设置页
    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState = sheetState,
            containerColor = BgWhite,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        ) {
            SettingsSheet(
                prompt = viewModel.getPrompt(),
                onSave = { prompt ->
                    viewModel.savePrompt(prompt)
                    showSettings = false
                },
                onReset = {
                    viewModel.resetPrompt()
                    showSettings = false
                },
            )
        }
    }
}

@Composable
private fun TopBar(connected: Boolean, onSettingsClick: () -> Unit) {
    Surface(color = BgWhite) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Emma 头像
            Box(modifier = Modifier.size(40.dp)) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(Orange, OrangeLight)
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("E", color = BubbleUserText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                // 在线指示点
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(OnlineGreen)
                        .align(Alignment.BottomEnd)
                        .padding(2.dp)
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text("Emma", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("在线 · 英语老师", color = TextSecondary, fontSize = 10.sp)
            }

            // 连接状态 + 齿轮
            Text(
                if (connected) "● 已连接" else "● 未连接",
                color = if (connected) OnlineGreen else TextDim,
                fontSize = 10.sp,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "⚙",
                color = TextDim,
                fontSize = 18.sp,
                modifier = Modifier.clickable { onSettingsClick() },
            )
        }
    }
    Divider(color = com.englishvoicebuddy.ui.theme.Divider, thickness = 1.dp)
}

@Composable
private fun ChatList(
    messages: List<ChatMessage>,
    showScrollBadge: Boolean,
    onScrolledUp: () -> Unit,
    onScrollToBottom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // 自动滚到底部（仅当用户在底部时）
    val isAtBottom by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisible >= totalItems - 2
        }
    }

    LaunchedEffect(messages.size) {
        if (isAtBottom) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 14.dp,
                vertical = 12.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            items(messages, key = { messages.indexOf(it) }) { msg ->
                when (msg) {
                    is ChatMessage.User -> UserBubble(msg.text)
                    is ChatMessage.AiStreaming -> AiBubble(msg.text)
                }
            }
        }

        // "回到底部"浮标
        AnimatedVisibility(
            visible = showScrollBadge,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Box(
                Modifier
                    .padding(bottom = 12.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Orange)
                    .clickable {
                        coroutineScope.launch {
                            listState.animateScrollToItem(messages.size - 1)
                            onScrollToBottom()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("↓", color = BubbleUserText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // 监听用户手动上翻
    val isUserScrolling = !isAtBottom
    LaunchedEffect(isUserScrolling) {
        if (isUserScrolling) onScrolledUp()
    }
}

@Composable
private fun UserBubble(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        Box(
            Modifier
                .widthIn(max = 260.dp)
                .clip(RoundedCornerShape(
                    topStart = 16.dp, topEnd = 6.dp, bottomStart = 16.dp, bottomEnd = 16.dp,
                ))
                .background(BubbleUserBg)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(text, color = BubbleUserText, fontSize = 13.sp, lineHeight = 20.sp)
        }
        Spacer(Modifier.width(8.dp))
        // 用户头像
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(TextDim),
            contentAlignment = Alignment.Center,
        ) {
            // 占位：后续替换为 OIP.webp
            Text("我", color = BubbleUserText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AiBubble(text: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // Emma 头像
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(listOf(Orange, OrangeLight))
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text("E", color = BubbleUserText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .widthIn(max = 260.dp)
                .clip(RoundedCornerShape(
                    topStart = 6.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp,
                ))
                .background(BubbleAiBg)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(text, color = BubbleAiText, fontSize = 13.sp, lineHeight = 20.sp)
        }
    }
}
```

---

### Task 10: 设置页 Bottom Sheet

**Files:**
- Create: `english-voice-buddy-android/app/src/main/java/com/englishvoicebuddy/ui/SettingsSheet.kt`

- [ ] **Step 1: 编写 SettingsSheet.kt**

```kotlin
// english-voice-buddy-android/app/src/main/java/com/englishvoicebuddy/ui/SettingsSheet.kt
package com.englishvoicebuddy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.englishvoicebuddy.ui.theme.BgWarm
import com.englishvoicebuddy.ui.theme.BgWhite
import com.englishvoicebuddy.ui.theme.BubbleUserText
import com.englishvoicebuddy.ui.theme.Orange
import com.englishvoicebuddy.ui.theme.OrangeLight
import com.englishvoicebuddy.ui.theme.RecordingRed
import com.englishvoicebuddy.ui.theme.TextPrimary
import com.englishvoicebuddy.ui.theme.TextSecondary

@Composable
fun SettingsSheet(
    prompt: String,
    onSave: (String) -> Unit,
    onReset: () -> Unit,
) {
    var editingPrompt by remember { mutableStateOf(prompt) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
    ) {
        // 拖拽手柄
        Box(
            Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(TextSecondary.copy(alpha = 0.4f))
                .align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(16.dp))

        // 头像 + 标题
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(listOf(Orange, OrangeLight))
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text("E", color = BubbleUserText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Emma", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("英语老师 · 系统提示词", color = TextSecondary, fontSize = 10.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        // 编辑区
        OutlinedTextField(
            value = editingPrompt,
            onValueChange = { editingPrompt = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 11.sp,
                lineHeight = 18.sp,
                color = TextPrimary,
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Orange,
                unfocusedBorderColor = TextSecondary.copy(alpha = 0.2f),
                focusedContainerColor = BgWarm,
                unfocusedContainerColor = BgWarm,
            ),
            shape = RoundedCornerShape(10.dp),
        )

        Spacer(Modifier.height(16.dp))

        // 按钮行
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { onSave(prompt) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
            ) {
                Text("取消", fontSize = 13.sp)
            }
            Spacer(Modifier.width(10.dp))
            Button(
                onClick = { onSave(editingPrompt) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Orange),
            ) {
                Text("保存", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(10.dp))

        TextButton(
            onClick = onReset,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text("恢复默认提示词", color = RecordingRed.copy(alpha = 0.6f), fontSize = 11.sp)
        }
    }
}
```

---

### Task 11: MainActivity 入口

**Files:**
- Create: `english-voice-buddy-android/app/src/main/java/com/englishvoicebuddy/MainActivity.kt`

- [ ] **Step 1: 编写 MainActivity.kt**

```kotlin
// english-voice-buddy-android/app/src/main/java/com/englishvoicebuddy/MainActivity.kt
package com.englishvoicebuddy

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.englishvoicebuddy.ui.ChatScreen
import com.englishvoicebuddy.ui.theme.BgWarm
import com.englishvoicebuddy.viewmodel.ChatViewModel

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // 权限已授予，ViewModel 初始化时会连接
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 请求录音权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = BgWarm,
            ) {
                val vm: ChatViewModel = viewModel()
                ChatScreen(viewModel = vm)
            }
        }
    }
}
```

---

### Task 12: 字符串资源

**Files:**
- Create: `english-voice-buddy-android/app/src/main/res/values/strings.xml`

- [ ] **Step 1: 编写 strings.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Voice Buddy</string>
</resources>
```

---

## 自审清单

1. **Spec coverage**: 
   - 架构 ✓ (Task 1/3/4/6)
   - UI 配色 ✓ (Task 2)
   - 音频引擎 ✓ (Task 3)
   - WebSocket 客户端 ✓ (Task 4)
   - 提示词本地存储 ✓ (Task 5)
   - 聊天界面+气泡+滚动 ✓ (Task 9)
   - 麦克风按钮+动效 ✓ (Task 8)
   - 设置页 Bottom Sheet ✓ (Task 10)
   - 数据流 ✓ (Task 6 handleEvent)
   - API 配置 ✓ (Task 4 connect)

2. **Placeholder scan**: 用户头像在 ChatScreen 中为占位符"我"字，需后续替换为 OIP.webp — 这是已知的待替换项，非遗漏

3. **Type consistency**: ChatMessage 在 Task 6 定义、Task 9 使用，签名一致；MicState 在 Task 6 定义、Task 8/9 使用，签名一致；QwenEvent 在 Task 4 定义、Task 6 使用，签名一致
