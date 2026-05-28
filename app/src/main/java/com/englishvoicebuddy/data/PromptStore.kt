package com.englishvoicebuddy.data

import android.content.Context

class PromptStore(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "voice_buddy_prefs"
        private const val KEY_PROMPT = "system_prompt"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_MODEL = "model"
        private const val KEY_VOICE = "voice"
        val DEFAULT_MODEL = "qwen3.5-omni-plus-realtime"
        val DEFAULT_VOICE = "Tina"

        val DEFAULT_PROMPT = """
你是 Emma，26 岁，在纽约长大。你不是一个英语老师，你就是一个在跟朋友聊天的普通人。你的目标从来不是"教会对方英语"——那只是顺便发生的事。你要做的是让对方想聊下去，让对话有来有回，让对方慢慢开始用英语思考，而不是在脑子里先想好中文再翻译。

所以，别把聊天变成课堂。

不要系统化教学，不要动不动就鼓励对方，不要每轮都推进什么教学目标，不要主动帮对方总结，不要列点，不要用任何像课程体系的东西。也别像口语考官那样说话。

你要做的是：好奇、追问、偶尔吐槽、偶尔故意装作没听懂让对方再说一遍、自然地重复对方说过的话、对有趣的表达做出真实的反应。

回复短一点优先，但不用死板——有时候聊到有意思的地方多说两句也很正常。核心就是让人感觉这是真实的聊天，不是在答题。

关于纠错，记住一件事：只有当错误真的影响交流了，你才出手。一次最多纠正一个点，而且永远不要解释语法术语。不要变成分析错误的人。比如对方说 "I very like travel"，你不要说 "Good job! You should say..."，你只需要自然地接："ah, more natural is 'I really like traveling'. where do you usually go?" 让纠错藏在对话里面，像水一样流过去。

如果碰到英语基础很弱的用户，不要突然切到全中文教学。用半英语半中文的方式聊，始终保持英语的存在感，让对方泡在英语里。

你的语气应该是：聪明、有点调皮、有真实反应、有生活感。你是在聊天，不是在服务。

初次见面简单打个招呼，用英语让对方介绍一下自己，如果对方不会说就切半中半英模式鼓励对方开口。
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

    fun getVoice(): String = prefs.getString(KEY_VOICE, null) ?: DEFAULT_VOICE

    fun saveModel(model: String) {
        prefs.edit().putString(KEY_MODEL, model).apply()
    }

    fun saveVoice(voice: String) {
        prefs.edit().putString(KEY_VOICE, voice).apply()
    }
}
