# 聊天气泡顺序和累积修复 — 设计文档

> 日期: 2026-05-27
> 状态: 已确认
> 范围: ChatViewModel.kt 单文件

## 问题

三个症状：
1. 多轮对话 AI 气泡累积（新回复追加到旧 AI 气泡）
2. 用户气泡偶尔出现 AI 内容（插入逻辑错位）
3. 消息顺序反转（AI 在用户前面）

## 根因

| 代码 | Bug |
|------|-----|
| `if (activeGen != responseGen) return` | 两值永远相等，守卫从不拦截事件 |
| `list.add(list.lastIndex, userMsg)` | 把新用户消息塞到旧 AI 气泡前 |
| `if (last is ChatMessage.AiStreaming)` 无条件追加 | 跨轮回复累积到旧气泡 |

## 修复：`currentAiStarted` 标记

替换整个 gen 计数 + 插入体系为单布尔标记。

### 删除（共 7 类）
- `responseGen`、`activeGen` 字段
- `isResponseActive` 字段
- 所有 `if (activeGen != responseGen) return`（6 处）
- `ResponseCreated`/`ResponseDone` handler
- `UserTranscript` 中的 `list.add(list.lastIndex, userMsg)` 插入
- `startRecording()` barge-in 和 `stopRecording()` 中的 gen 递增
- `isResponseActive = false` 引用（3 处）

### 新增
- `private var currentAiStarted = false`
- 重置：`startRecording()` 中 `pendingTranscript = ""` 后
- 重置：`stopRecording()` commit 路径 `isAiResponding = true` 前

### 修改

**flushAiText()：**
```
if (last is ChatMessage.AiStreaming && currentAiStarted)
    → 追加
else
    → 新建 + currentAiStarted = true
```

**UserTranscript handler → 简化为：**
```
flushAiText()
list.add(ChatMessage.User(event.text))
```

**startRecording barge-in 段简化为：**
```
if (isAiResponding) {
    flushAiText()
    try { client?.cancelResponse() } catch (_: Exception) {}
    stopAudioPlayer()
    rebuildAudioQueue()
    isAiResponding = false
    _micState.value = MicState.IDLE
}
```

**Error handler → 移除 gen 条件包裹和 isResponseActive：**
```
val msg = event.message
if (msg.contains("none active response") || msg.contains("no active response")) return
stopAudioPlayer()
list.add(ChatMessage.AiStreaming("[${msg}]"))
_messages.value = list
_micState.value = MicState.IDLE
isAiResponding = false
```

## 不动
- AudioEngine / QwenClient / MicButton / 音频 Channel 生命周期
- SettingsSheet / ChatScreen UI
- `isAiResponding`、300ms 防抖、`flog()` 日志

## 验证
1. 第一轮 "hello" → [User, AiStreaming] 顺序正确
2. 第二轮 "how are you" → 新旧气泡独立
3. 打断 → 成功，新气泡不串旧
4. 5+ 轮 → 无累积
