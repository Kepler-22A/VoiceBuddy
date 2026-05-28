# English Voice Buddy Android App — 崩溃诊断报告

> 日期: 2026-05-27

## 崩溃现象

多轮对话中 AI 正在说话时用户点击麦克风 → AI 闭嘴 → 按钮无动效 → App 崩溃

## 根因分析

### 1. 转录与音频完成时序不同步（核心问题）

Qwen Realtime API 的两个事件 `TranscriptDone` 和 `AudioDone` 异步到达：

```
时间线:
  TranscriptDelta...(多个) → TranscriptDone → micState = IDLE ← 按钮可用！
  AudioDelta...(多个)       → AudioDone    → micState = IDLE
                                  ↑
                          音频还在播放，但按钮已经可以按了
```

`TranscriptDone` 提前将 micState 设为 IDLE，此时音频播放器仍在写入 AudioTrack。用户看到按钮恢复后按下 → `startRecording()` 取消音频播放协程 → AudioTrack 在被 write() 时被 cancel → **SIGSEGV 原生崩溃**。

### 2. 打断（Barge-in）未实现

服务器端 `response.cancel` 从未发送。当用户在 AI 回应中打断时：
- 服务端继续生成音频，浪费资源
- 本地音频队列残留数据继续播放
- Channel 被 cancel 后 trySend 失败，数据丢失
- 第二轮对话时旧 Channel 已关闭，新音频无法入队

### 3. AudioTrack 跨线程释放竞态

多处代码可能同时调用 `AudioEngine.release()`：
- 音频播放协程的 finally 块
- `startRecording()` 中的清理逻辑
- ViewModel.onCleared()

即使使用 `?.` 安全调用，两个线程同时拿到同一 AudioTrack 引用后分别调用 `release()`，第二次 `release()` 在已释放的原生对象上触发 SIGSEGV。

## 修复方案

### Fix 1: 统一 micState 管理
- `TranscriptDone` **不再**设置 micState = IDLE
- 仅 `AudioDone`（finally 块）设置 micState = IDLE
- 新增 `isAiResponding` boolean 跟踪 AI 状态

### Fix 2: 实现打断
- `startRecording()` 检测到 isAiResponding = true 时：
  1. 发送 `response.cancel` 到 WebSocket
  2. 停止音频播放器（cancel player coroutine）
  3. 清理音频队列
  4. 开始新录音
- 打断延迟目标 < 200ms

### Fix 3: AudioEngine 线程安全
- `release()` 先取引用再 null，确保最多释放一次
- 移除 `startRecording()` 中的 `release()` 调用
- 仅由各自 owning coroutine 的 finally 块执行释放
