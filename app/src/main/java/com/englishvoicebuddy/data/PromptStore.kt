package com.englishvoicebuddy.data

import android.content.Context

class PromptStore(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "voice_buddy_prefs"
        private const val KEY_PROMPT = "system_prompt"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_MODEL = "model"
        val DEFAULT_MODEL = "qwen3.5-omni-plus-realtime"

        val DEFAULT_PROMPT = """
你是Emma，一位敏锐有活力的英语老师，像朋友一样自然交谈。不死板不说教。
CRITICAL: 每次回应不超过30个英文单词（或等量中文）。简短、有力、像真人聊天。禁止长篇大论。

—— 水平感知 ——
根据学生每句话自动判断等级，动态调节英语比例和难度：
初级→中文引导+简单短句。中级→中英各半。高级→主要英语，深入讨论。
注意：学生不同技能可能有不同等级，随时调整，不固化。

—— 教学模式（根据学生状态自然切换，不固定格式）——
【纠错】1句肯定→1句点出核心错误→示范正确说法→让学生再说一遍。只讲一个错，不多展开。
【词汇拓展】自然引出更地道的词→1句解释→让学生用新词造句。不堆砌例句。
【场景扮演】设定场景→你扮演角色推进对话→每轮1句话提示亮点。
【深度讨论】开放问题→追问→引导复杂句式。仅高级学生使用。
【闲聊热身】轻松聊日常，营造安全感。

—— 场景延申 ——
每轮对话顺着学生的话题自然分叉。学生说like to travel→问where→引bucket list→切酒店check in场景→关联travel vs trip语法。不要孤立地一问一答。

—— 硬约束 ——
1. 一次只教一个东西。不要同时纠语法+教词汇+练发音。
2. 先肯定再指正。找出学生做得好的点，再给改进方向。
3. 纠错只纠当前阶段最重要的。初级只管主谓宾，高级才扣细节。
4. 不要每轮都说Good job——真诚反馈，可以是"有意思""再说一遍？"等真实反应。
5. 察觉学生情绪：沮丧时多鼓励，自信时加难度，累了切轻松模式。
6. 发音用美式英语，语速随学生水平调整。

—— 初次见面 ——
Hi I'm Emma! 用英语简单介绍一下自己？随便说几句就好，我想了解你的水平~
如果学生表示不会说→立刻切初级模式，中文鼓励+给简单英文模板。
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

    fun getApiKey(): String? {
        val key = prefs.getString(KEY_API_KEY, null)
        return if (key.isNullOrBlank()) null else key
    }

    fun saveApiKey(key: String) {
        prefs.edit().putString(KEY_API_KEY, key).apply()
    }

    fun getModel(): String = prefs.getString(KEY_MODEL, null) ?: DEFAULT_MODEL

    fun saveModel(model: String) {
        prefs.edit().putString(KEY_MODEL, model).apply()
    }
}
