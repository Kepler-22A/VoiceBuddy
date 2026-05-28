# UI 样式修复 + 设置拆分 实施计划

> **执行方式:** executing-plans，按任务顺序逐个实施

**Goal:** 修复滚动/高度/边界 3 个 UI 问题 + 拆分设置为齿轮(API)和头像(提示词)双入口

**Architecture:** 5 文件改动，仅 UI 层 + 数据层新增 model 字段，绝对不动 AudioEngine/QwenClient/消息/音频 Channel/打断/手势

**Tech Stack:** Jetpack Compose, Material3 BottomSheet

---

### Task 1: MicButton — 三态简化

**Modify:** `MicButton.kt`

- 删除 `SoundWaveBars` composable 和"松开发送"文字
- DISABLED 改为 AI_SPEAKING 态：48dp 橙色圆环 `border: 2dp #e07b5a` + 5 根跳动声波条 + 背景 `#faf6f3`
- 按钮缩小为 48dp
- IDLE 和 RECORDING 保持：橙色圆/红色圆+涟漪

### Task 2: ChatScreen — 边界+双入口+滚动

**Modify:** `ChatScreen.kt`

- TopBar `border-bottom: 1px solid #e8e3dc`
- 底部录音区 `border-top: 1px solid #e8e3dc`, padding 6dp
- 头像加 ✎ 角标 → `showPromptSettings`
- 齿轮 → `showApiSettings`
- 两个独立 ModalBottomSheet：ApiSettingsSheet / PromptSettingsSheet
- 滚动追踪修正

### Task 3: 拆分为两个 SettingsSheet

**Create:** `ApiSettingsSheet.kt` / Modify: `SettingsSheet.kt` → rename `PromptSettingsSheet.kt`

- ApiSettingsSheet: API Key 输入 + 模型下拉(3选1) + 取消/保存
- PromptSettingsSheet: 原有+编辑区 250dp 橙色边框 + 取消/保存/恢复默认紧凑行

### Task 4: PromptStore + ChatViewModel — 加 model 字段

**Modify:** `PromptStore.kt`, `ChatViewModel.kt`

- PromptStore: `getModel()`, `saveModel()`, 默认 `qwen3.5-omni-plus-realtime`
- ChatViewModel: `getModel()`, `saveSettings(apiKey, model, prompt)`
- QwenClient URL 改为可拼 model 参数

### Task 5: Build & 验证

```bash
gradlew.bat assembleDebug && adb install -r
```
