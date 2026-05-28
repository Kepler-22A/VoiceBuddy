# 音色切换功能 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 ApiSettingsSheet 中添加音色选择器，从硬编码 Ethan 改为 54 种可选音色

**Architecture:** 自底向上：VoiceConfig 数据类 → PromptStore 持久化 → QwenClient 参数化 → ChatViewModel 加载/传递 → ApiSettingsSheet UI → ChatScreen 接线。每层只新增必要接口，不动现有逻辑。

**Tech Stack:** Kotlin, Jetpack Compose, Material3, SharedPreferences, OkHttp WebSocket, org.json

---

### Task 1: 新增 VoiceConfig 数据类

**Files:**
- Create: `app/src/main/java/com/englishvoicebuddy/data/VoiceConfig.kt`

- [ ] **Step 1: 创建 VoiceConfig.kt**

```kotlin
package com.englishvoicebuddy.data

data class VoiceConfig(
    val voice: String,
    val name: String,
    val description: String,
    val accent: String?,
)
```

- [ ] **Step 2: 验证编译**

Run: `./gradlew :app:compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

---

### Task 2: 拷贝音色 JSON 到 assets

**Files:**
- Create: `app/src/main/assets/qwen-omni-voices.json`

- [ ] **Step 1: 创建 assets 目录并复制文件**

Run: `mkdir -p "D:\code\VoiceBuddy\app\src\main\assets" && cp "D:\code\VoiceBuddy\qwen-omni-voices.json" "D:\code\VoiceBuddy\app\src\main\assets\qwen-omni-voices.json"`

- [ ] **Step 2: 验证文件存在**

Run: `ls "D:\code\VoiceBuddy\app\src\main\assets\qwen-omni-voices.json"`
Expected: 输出文件路径

---

### Task 3: PromptStore 添加 voice 存储

**Files:**
- Modify: `app/src/main/java/com/englishvoicebuddy/data/PromptStore.kt`

- [ ] **Step 1: 在 companion object 中新增 voice 常量**

在 `KEY_MODEL = "model"` 之后添加：

```kotlin
private const val KEY_VOICE = "voice"
val DEFAULT_VOICE = "Tina"
```

- [ ] **Step 2: 新增 getVoice() 方法**

在 `getModel()` 方法之后添加：

```kotlin
fun getVoice(): String = prefs.getString(KEY_VOICE, null) ?: DEFAULT_VOICE
```

- [ ] **Step 3: 新增 saveVoice() 方法**

在 `saveModel()` 方法之后添加：

```kotlin
fun saveVoice(voice: String) {
    prefs.edit().putString(KEY_VOICE, voice).apply()
}
```

- [ ] **Step 4: 验证编译**

Run: `./gradlew :app:compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

---

### Task 4: QwenClient voice 参数化

**Files:**
- Modify: `app/src/main/java/com/englishvoicebuddy/network/QwenClient.kt`

- [ ] **Step 1: 构造函数添加 voice 参数**

将构造函数签名改为：

```kotlin
class QwenClient(
    private val apiKey: String,
    private val instructions: String,
    private val logFile: File,
    model: String = "qwen3.5-omni-plus-realtime",
    private val voice: String = "Tina",
) {
```

- [ ] **Step 2: session config 改用 voice 参数**

将 `connect()` 方法中第 65 行的：

```kotlin
val sessionConfig = """{"type":"session.update","session":{"modalities":["text","audio"],"voice":"Ethan","instructions":${JSONObject.quote(instructions)},"input_audio_format":"pcm16","output_audio_format":"pcm16","input_audio_transcription":{"model":null},"turn_detection":null}}"""
```

改为：

```kotlin
val sessionConfig = """{"type":"session.update","session":{"modalities":["text","audio"],"voice":${JSONObject.quote(voice)},"instructions":${JSONObject.quote(instructions)},"input_audio_format":"pcm16","output_audio_format":"pcm16","input_audio_transcription":{"model":null},"turn_detection":null}}"""
```

- [ ] **Step 3: 验证编译**

Run: `./gradlew :app:compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

---

### Task 5: ChatViewModel 音色加载与传递

**Files:**
- Modify: `app/src/main/java/com/englishvoicebuddy/viewmodel/ChatViewModel.kt`

- [ ] **Step 1: 添加 import**

在文件顶部 import 区域添加：

```kotlin
import com.englishvoicebuddy.data.VoiceConfig
import org.json.JSONArray
import org.json.JSONObject
```

- [ ] **Step 2: 在类体中新增 voices 列表和访问器**

在 `private val promptStore = PromptStore(application)` 之后添加：

```kotlin
private val _voices: List<VoiceConfig> = loadVoices()
fun getVoices(): List<VoiceConfig> = _voices
fun getVoice(): String = promptStore.getVoice()
```

- [ ] **Step 3: 新增 loadVoices() 方法**

在 `getModel()` 方法之后添加：

```kotlin
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
```

- [ ] **Step 4: 修改 saveSettings() 签名并保存 voice**

将 `saveSettings` 方法签名和实现改为：

```kotlin
fun saveSettings(apiKey: String, model: String, voice: String, prompt: String) {
    promptStore.saveApiKey(apiKey)
    promptStore.saveModel(model)
    promptStore.saveVoice(voice)
    promptStore.savePrompt(prompt)
    client?.disconnect()
    connectToQwen()
}
```

- [ ] **Step 5: connectToQwen() 传 voice 给 QwenClient**

将 `connectToQwen()` 中 `QwenClient` 构造调用改为：

```kotlin
client = QwenClient(apiKey, promptStore.getPrompt(), logFile, promptStore.getModel(), promptStore.getVoice()).also { c ->
```

- [ ] **Step 6: 验证编译**

Run: `./gradlew :app:compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

---

### Task 6: ApiSettingsSheet 音色选择器 UI

**Files:**
- Modify: `app/src/main/java/com/englishvoicebuddy/ui/ApiSettingsSheet.kt`

- [ ] **Step 1: 添加 import**

```kotlin
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.englishvoicebuddy.data.VoiceConfig
```

- [ ] **Step 2: 修改 MODELS 列表，去掉 qwen3-omni-flash-realtime**

将：

```kotlin
private val MODELS = listOf(
    "qwen3.5-omni-plus-realtime",
    "qwen3.5-omni-flash-realtime",
    "qwen3-omni-flash-realtime",
)
```

改为：

```kotlin
private val MODELS = listOf(
    "qwen3.5-omni-plus-realtime",
    "qwen3.5-omni-flash-realtime",
)
```

- [ ] **Step 3: 修改函数签名，添加 voices 和 currentVoice 参数**

```kotlin
@Composable
fun ApiSettingsSheet(
    apiKey: String,
    model: String,
    voices: List<VoiceConfig>,
    currentVoice: String,
    onSave: (apiKey: String, model: String, voice: String) -> Unit,
) {
```

- [ ] **Step 4: 在函数体内新增 voice 状态变量**

在 `var modelExpanded by remember { mutableStateOf(false) }` 之后添加：

```kotlin
var editingVoice by remember { mutableStateOf(currentVoice) }
var voiceExpanded by remember { mutableStateOf(false) }
val selectedVoice = voices.find { it.voice == editingVoice }
```

- [ ] **Step 5: 在模型选择器之后、按钮行之前插入音色选择器 UI**

在模型下拉菜单的闭合 `}` 之后、`Spacer(Modifier.height(16.dp))` 之前插入：

```kotlin

        Spacer(Modifier.height(14.dp))

        Text("音色", fontSize = 11.sp, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(BgWarm).border(1.dp, if (voiceExpanded) Orange else TextSecondary.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                .clickable { voiceExpanded = !voiceExpanded }
                .padding(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(selectedVoice?.name ?: editingVoice, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    selectedVoice?.description?.let {
                        Text(it, fontSize = 10.sp, color = TextSecondary, maxLines = 1)
                    }
                }
                Text("▼", fontSize = 10.sp, color = TextSecondary)
            }
        }
        if (voiceExpanded) {
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(10.dp))
                    .background(BgWarm).border(1.dp, Orange.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            ) {
                LazyColumn {
                    items(voices) { v ->
                        val isSelected = v.voice == editingVoice
                        Box(
                            Modifier.fillMaxWidth()
                                .background(if (isSelected) BgWarm else BgWarm.copy(alpha = 0.5f))
                                .clickable { editingVoice = v.voice; voiceExpanded = false }
                                .padding(horizontal = 12.dp, vertical = 9.dp)
                        ) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(v.name, fontSize = 12.sp, color = TextPrimary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                    Row {
                                        Text(v.description, fontSize = 10.sp, color = TextSecondary, maxLines = 1)
                                        v.accent?.let { acc ->
                                            Spacer(Modifier.width(4.dp))
                                            Text("· $acc", fontSize = 10.sp, color = Orange.copy(alpha = 0.7f))
                                        }
                                    }
                                }
                                if (isSelected) Text("✓", fontSize = 12.sp, color = Orange, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
```

- [ ] **Step 6: 修改保存按钮回调，传递 voice**

将"保存"按钮的点击回调从：

```kotlin
.clickable { onSave(editingKey, editingModel) }
```

改为：

```kotlin
.clickable { onSave(editingKey, editingModel, editingVoice) }
```

- [ ] **Step 7: 验证编译**

Run: `./gradlew :app:compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

---

### Task 7: ChatScreen 接线

**Files:**
- Modify: `app/src/main/java/com/englishvoicebuddy/ui/ChatScreen.kt`

- [ ] **Step 1: 修改 ApiSettingsSheet 调用，添加 voice 参数**

将第 118-126 行的 `ApiSettingsSheet` 调用替换为：

```kotlin
            ApiSettingsSheet(
                apiKey = viewModel.getApiKey(),
                model = viewModel.getModel(),
                voices = viewModel.getVoices(),
                currentVoice = viewModel.getVoice(),
                onSave = { apiKey, model, voice ->
                    viewModel.saveSettings(apiKey, model, voice, viewModel.getPrompt())
                    showApiSettings = false
                },
            )
```

**注意：气泡逻辑（ChatList / UserBubble / AiBubble / scroll 行为）和 MicButton 逻辑完全不动。**

- [ ] **Step 2: 验证编译**

Run: `./gradlew :app:compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

---

### Task 8: 全量编译验证

- [ ] **Step 1: 完整编译**

Run: `./gradlew :app:assembleDebug 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 检查编译产物**

Run: `ls "D:\code\VoiceBuddy\app\build\outputs\apk\debug\"*.apk`
Expected: 输出 APK 文件路径
