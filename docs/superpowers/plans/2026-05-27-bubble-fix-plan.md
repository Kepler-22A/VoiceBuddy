# 气泡修复实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement inline.

**Goal:** 删除不工作的 gen 计数器和插入逻辑，用 `currentAiStarted` 布尔标记修复气泡顺序和累积

**Architecture:** ChatViewModel.kt 单文件改动，先删后加，不动其他文件

**Tech Stack:** Kotlin, Compose StateFlow

---

### Task 1: 重写 ChatViewModel.kt

**Files:**
- Modify: `C:\Users\Administrator\Desktop\ETalkApp\app\src\main\java\com\englishvoicebuddy\viewmodel\ChatViewModel.kt`

**改动点：**
1. 删除 `responseGen`/`activeGen`/`isResponseActive` 字段
2. 删除 6 处 `if (activeGen != responseGen) return`
3. 删除 `ResponseCreated`/`ResponseDone` handler
4. 删除 `UserTranscript` 插入逻辑
5. 新增 `currentAiStarted` 字段
6. 修改 `flushAiText()` 追加条件
7. 新增两处重置点（startRecording/stopRecording）
8. 简化 barge-in 段
9. 简化 Error handler

**Full replacement file provided below.**

- [ ] **Step 1: 替换文件并 Build**

```bash
cd C:\Users\Administrator\Desktop\ETalkApp && gradlew.bat assembleDebug
```

- [ ] **Step 2: 安装测试**
