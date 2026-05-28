# 音色切换功能 — 设计文档

## 背景

- 音色在 `QwenClient.kt:65` 硬编码为 `"Ethan"`，用户无法切换
- 音色配置 54 种，定义在 `qwen-omni-voices.json`（项目根目录）
- 默认音色应为 `"Tina"`（JSON `default_voice` 字段）

## 硬约束

- **ChatScreen.kt 中气泡逻辑（ChatList / UserBubble / AiBubble / scroll 行为）不许动**
- **ChatScreen.kt 中麦克风按钮逻辑（MicButton / startRecording / stopRecording）不许动**
- **ChatViewModel.kt 中录音、音频播放、消息流逻辑不许动**
- 只增不改：新增代码用追加方式，修改现有代码只改必要的参数传递链

## 改动明细

### 1. 数据模型 — 新增 VoiceConfig

```kotlin
// 新文件: data/VoiceConfig.kt
data class VoiceConfig(
    val voice: String,       // API 用的 voice ID, e.g. "Tina"
    val name: String,        // 显示名, e.g. "甜甜 Tina"
    val description: String, // 一句话描述
    val accent: String?,     // 口音标签, nullable
)
```

### 2. PromptStore.kt — 新增 voice 持久化

| 操作 | 代码 |
|------|------|
| 常量 | `KEY_VOICE = "voice"`, `DEFAULT_VOICE = "Tina"` |
| 读 | `fun getVoice(): String` 从 SharedPreferences 读，默认 Tina |
| 写 | `fun saveVoice(voice: String)` |

### 3. QwenClient.kt — voice 参数化

- 构造函数加 `voice: String` 参数（放在 `model` 之后）
- `connect()` 中 session config 的 `"voice":"Ethan"` 改为 `"voice":${JSONObject.quote(voice)}`
- 其他 WebSocket 逻辑不变

### 4. ChatViewModel.kt — 音色列表加载 + 状态

新增：
- `loadVoices()`: 从 `assets/qwen-omni-voices.json` 读取并解析为 `List<VoiceConfig>`
- `getVoice()` / `getVoices()` 公开访问器

修改：
- `saveSettings(apiKey, model, voice, prompt)` 签名加 voice → 调 `promptStore.saveVoice(voice)`
- `connectToQwen()` 传 `promptStore.getVoice()` 给 QwenClient

不变：
- 录音逻辑、音频播放、消息处理、`handleEvent` 全部不动

### 5. ApiSettingsSheet.kt — 音色选择器 UI

修改：
- `MODELS` 列表去掉 `"qwen3.5-omni-flash-realtime"`（用户要求）

新增 UI：
- 音色行（收起态）：显示当前音色 name + description + ▼
- 点击展开为内嵌滚动列表（在弹框内部展开，不弹新弹窗）
- 列表每行：name 加粗 + description + accent 蓝色标签（如有）
- 选中项橙色背景 + ✓ 标记
- 点击某行 → 更新选中 + 自动收起列表

回调签名变更：
- `onSave: (apiKey, model) -> Unit` → `onSave: (apiKey, model, voice) -> Unit`
- 新增 `voices: List<VoiceConfig>`、`currentVoice: String` 参数

### 6. ChatScreen.kt — 数据流

`ApiSettingsSheet` 调用处传参更新（只改 Sheet 调用那几行）：
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

气泡逻辑和 MicButton 逻辑完全不动。

### 7. assets — 音色 JSON

将 `qwen-omni-voices.json` 拷贝到 `app/src/main/assets/qwen-omni-voices.json`（Android assets 目录）。

## 数据流

```
assets/qwen-omni-voices.json
    → ChatViewModel.loadVoices()
        → ApiSettingsSheet (展示列表)
            → 用户选择 voice
                → ChatViewModel.saveSettings(apiKey, model, voice, prompt)
                    → PromptStore.saveVoice(voice)
                        → connectToQwen()
                            → QwenClient(voice=...) → WebSocket session.update
```
