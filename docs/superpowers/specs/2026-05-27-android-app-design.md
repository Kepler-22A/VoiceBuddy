# English Voice Buddy — Android App 设计文档

> 日期: 2026-05-27
> 状态: 已确认

## 1. 概述

将现有 Web 版 English Voice Buddy 重构为 Android 原生 App。保留核心的语音对话功能，精简为单一 Teacher 角色，纯客户端直连 Qwen API。

## 2. 技术栈

| 层 | 选型 |
|----|------|
| 语言 | Kotlin |
| UI | Jetpack Compose |
| WebSocket | OkHttp |
| 音频输入 | AudioRecord (PCM16, 24kHz, mono) |
| 音频输出 | AudioTrack (PCM16, 24kHz, mono) |
| 本地存储 | SharedPreferences（提示词） |
| 最低 SDK | API 26 (Android 8.0) |

## 3. 架构

```
┌──────────────────────────┐
│   Jetpack Compose UI     │
│  ChatScreen  SettingsSheet│
├──────────────────────────┤
│   ChatViewModel          │
│   (WS状态, 消息流, 录音状态)│
├──────────┬───────────────┤
│ AudioEngine │ QwenClient │
│ AudioRecord │ OkHttp WS   │
│ AudioTrack  │             │
└──────────┴───────────────┘
      │            │
  [麦克风/扬声器]  [DashScope API]
```

- AudioEngine：麦克风采集 → PCM16 字节流；PCM16 字节流 → 扬声器播放
- QwenClient：OkHttp WebSocket 直连 `wss://dashscope.aliyuncs.com/api-ws/v1/realtime?model=qwen3.5-omni-plus-realtime`
- ChatViewModel：管理消息列表、录音状态、WebSocket 生命周期
- API Key 存在 SharedPreferences，首次启动提示输入

## 4. UI 设计

### 4.1 配色

| 用途 | 色值 |
|------|------|
| 背景 | `#f5f0eb` 暖白 |
| 用户气泡 | `#e07b5a` 陶土橙 |
| AI 气泡背景 | `#ffffff` 白 |
| AI 气泡文字 | `#3c3c3c` 深灰 |
| TopBar/Settings 背景 | `#ffffff` |
| 分割线 | `#e8e3dc` |
| 次要文字 | `#999999` |
| 在线指示 | `#4caf50` 绿 |
| 录音状态 | `#ef4444` 红 |

### 4.2 主界面

```
┌──────────────────────────┐
│ (E) Emma           ⚙   │  TopBar: 头像+姓名+在线状态, 齿轮
│     在线 · 英语老师       │
├──────────────────────────┤
│        14:30             │  时间标签
│                          │
│ (E) ┌─────────────┐     │  AI气泡: 头像左 + 白底 + 圆角
│     │ Hi I'm Emma!│     │  6px 16px 16px 16px
│     └─────────────┘     │
│                          │  ← 间距 22px
│         ┌─────────────┐ │  用户气泡: 头像右 + 橙底白字
│         │ Hello!      │(U)  16px 6px 16px 16px
│         └─────────────┘ │
│                          │
│ (E) ┌─────────────┐     │  流式文本: 光标闪烁
│     │ Nice to meet│     │
│     │ you! ▏       │     │
│     └─────────────┘     │
│                          │
│              ↓           │  回到底部浮标(手动上翻时出现)
├──────────────────────────┤
│          🎤              │  麦克风按钮, 居中
└──────────────────────────┘
```

- 气泡最大宽度: 68%
- 文字: 13px, 行高 1.55
- 气泡间距: 22px

### 4.3 麦克风按钮

| 状态 | 表现 |
|------|------|
| 默认 | 橙色圆形 `#e07b5a`，自定义 SVG 麦克风图标（线条描边风） |
| 按住录音 | 红色圆形 `#ef4444`，外层波纹扩散动画 + 顶部声波动画条，下方显示"松开发送" |
| AI 回复中 | 灰色禁用 `#e8e3dc`，不可点击 |

### 4.4 滚动行为

- 用户在列表底部时：新消息自动 `animateScrollToItem`
- 用户手动上翻看历史时：不自动滚动，右下角出现环形浮标 `↓`（`#e07b5a` 背景，白色箭头）
- 点击浮标：`scrollToItem(0)` 回到底部，浮标消失

### 4.5 设置页

点击齿轮 → 从底部弹出 BottomSheet：

```
┌──────────────────────────┐
│      (对话页淡出)         │
├──────────────────────────┤
│  ── 拖拽手柄 ──          │
│                          │
│  (E) Emma                │
│  英语老师 · 系统提示词     │
│                          │
│  ┌────────────────────┐  │
│  │ 你是Emma，一位经验  │  │  可编辑文本区
│  │ 丰富的英语老师...   │  │  点击进入编辑模式
│  │                    │  │
│  └────────────────────┘  │
│                          │
│  [取消]    [保存]        │
│       恢复默认提示词       │
└──────────────────────────┘
```

- Sheet 背景: `#ffffff`，顶部圆角 20px
- 提示词修改即时保存到 SharedPreferences
- "恢复默认"用内置默认提示词覆盖

## 5. 数据流

```
按住麦克风 → AudioRecord 采集 PCM16
  → base64 编码
  → WebSocket 发送 input_audio_buffer.append
  → 松手时发送 input_audio_buffer.commit + response.create
  → 接收 response.output_audio.delta → 解码 → AudioTrack 播放
  → 接收 response.audio_transcript.delta → 追加到 AI 气泡文字
  → 接收 conversation.item.input_audio_transcription.completed → 显示用户气泡文字
```

## 6. Qwen API 配置

```json
{
  "type": "session.update",
  "session": {
    "modalities": ["text", "audio"],
    "voice": "Cherry",
    "instructions": "<系统提示词>",
    "input_audio_format": "pcm16",
    "output_audio_format": "pcm16",
    "input_audio_transcription": {"model": null},
    "turn_detection": null
  }
}
```

- URL: `wss://dashscope.aliyuncs.com/api-ws/v1/realtime?model=qwen3.5-omni-plus-realtime`
- 认证: `Authorization: Bearer <DASHSCOPE_API_KEY>`

## 7. 文件结构

```
english-voice-buddy-android/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/englishvoicebuddy/
│       │   ├── MainActivity.kt
│       │   ├── ui/
│       │   │   ├── ChatScreen.kt
│       │   │   ├── SettingsSheet.kt
│       │   │   └── theme/
│       │   │       └── Theme.kt
│       │   ├── engine/
│       │   │   └── AudioEngine.kt
│       │   ├── network/
│       │   │   └── QwenClient.kt
│       │   ├── viewmodel/
│       │   │   └── ChatViewModel.kt
│       │   └── data/
│       │       └── PromptStore.kt
│       └── res/
│           ├── drawable/
│           │   └── mic_icon.xml
│           └── values/
│               └── strings.xml
├── build.gradle.kts
└── settings.gradle.kts
```

## 8. 默认提示词

沿用 server.py 中 Teacher 的完整提示词（Emma，约110行中文提示词），作为 code 内默认值。用户修改后存 SharedPreferences，读时优先取用户版本。"恢复默认"操作重置为 code 内默认值。

## 9. 不做

- 不保留 Buddy 角色
- 不支持自由对话模式（仅按住说话）
- 不做消息持久化/历史记录
- 不实现打断（barge-in）——按住说话模式下不需要
- 不添加多语言支持（仅中文 UI）
