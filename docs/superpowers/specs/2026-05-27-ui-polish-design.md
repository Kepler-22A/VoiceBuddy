# UI 样式修复 + 设置拆分 — 设计文档

> 日期: 2026-05-27
> 改: ChatScreen.kt, MicButton.kt, SettingsSheet.kt（拆分）, PromptStore.kt, ChatViewModel.kt
> 不碰: AudioEngine, QwenClient, 消息处理, 音频 Channel, 打断, 手势

## 1. 滚动追踪修复

`ChatList` 中 LaunchedEffect 的 `isAtBottom` 检查调整为含占位符在内的 messages.size。

## 2. 底部录音区 — 固定高度

**MicButton.kt：**
- 删除 `SoundWaveBars` composable 和"松开发送"文字。
- 三态：
  - IDLE：48dp 橙色圆 + SVG 麦克风图标
  - RECORDING：48dp 红色圆 + 双层涟漪波纹
  - DISABLED → 重命名为 AI_SPEAKING：48dp 橙色圆环边框 + 内部 5 根跳动声波条（暗示可打断）

**ChatScreen.kt 底部区：**
- 加 `border-top: 1px solid #e8e3dc`
- padding 上下各 6dp，总高 60dp

## 3. 边界分隔

- TopBar 加 `border-bottom: 1px solid #e8e3dc`
- 底部录音区加 `border-top: 1px solid #e8e3dc`

## 4. 设置拆分

### 齿轮 → API 设置 Sheet
- DashScope API Key 输入框
- 模型下拉：qwen3.5-omni-plus-realtime / qwen3.5-omni-flash-realtime / qwen3-omni-flash-realtime
- 紧凑按钮：取消 + 保存

### 头像（带 ✎ 角标）→ 提示词编辑 Sheet
- Emma 头像 + "英语老师 · 系统提示词"
- 编辑区 250dp，橙色边框 `#e07b5a`
- 紧凑按钮行：取消 + 保存 + 恢复默认（同一行）

### 数据层
- PromptStore 加 `getModel()` / `saveModel()`，默认 `qwen3.5-omni-plus-realtime`
- ChatViewModel 加 `getModel()` / `saveSettings(apiKey, model, prompt)`
- QwenClient URL 改为可拼模型参数

## 5. 不变

- AudioEngine / QwenClient 核心 / 消息流程 / 音频 Channel / 打断逻辑 / 气泡管理 / 300ms 防抖 / 手势
