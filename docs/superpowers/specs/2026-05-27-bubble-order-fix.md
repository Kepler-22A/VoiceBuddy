# 气泡时序修复设计 — 占位符先建

> 日期: 2026-05-27
> 状态: 已确认
> 范围: ChatViewModel.kt

## 策略

吸纳 Web 版经验：stopRecording 时立刻插入用户气泡占位符，比任何服务端事件都早。UserTranscript 到达时替换为真实文本。物理上保证用户消息绝不在 AI 之后。

## 改动

### 新增字段
```kotlin
private var placeholderIndex = -1
```

### stopRecording() — commit 前插入占位符
```kotlin
if (client != null && audioChunks.isNotEmpty()) {
    val list = _messages.value.toMutableList()
    list.add(ChatMessage.User("..."))
    placeholderIndex = list.lastIndex
    _messages.value = list

    rebuildAudioQueue()
    currentAiIndex = -1
    isAiResponding = true
    _micState.value = MicState.DISABLED
    try { client?.commitAndRespond() } catch (_: Exception) {}
}
```

### UserTranscript handler — 替换占位符
```kotlin
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
```

### startRecording() — 重置
```
placeholderIndex = -1  (与 currentAiIndex = -1 并列)
```

## 不变
- TranscriptDelta / flushAiText / currentAiIndex 逻辑完全不动
- AudioEngine / QwenClient / MicButton 不动
- 打断 / 防抖 / Channel 等全部不动
