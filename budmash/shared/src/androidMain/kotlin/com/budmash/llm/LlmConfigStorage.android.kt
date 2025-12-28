package com.budmash.llm

import android.content.Context
import android.content.SharedPreferences

actual class LlmConfigStorage {
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences("budmash_llm", Context.MODE_PRIVATE)
    }

    actual fun getApiKey(): String? = prefs?.getString("api_key", null)

    actual fun setApiKey(key: String) {
        prefs?.edit()?.putString("api_key", key)?.apply()
    }

    actual fun getModel(): String = prefs?.getString("model", "anthropic/claude-3-haiku") ?: "anthropic/claude-3-haiku"

    actual fun setModel(model: String) {
        prefs?.edit()?.putString("model", model)?.apply()
    }

    actual fun getProvider(): LlmProviderType {
        val name = prefs?.getString("provider", "OPENROUTER") ?: "OPENROUTER"
        return LlmProviderType.valueOf(name)
    }

    actual fun setProvider(provider: LlmProviderType) {
        prefs?.edit()?.putString("provider", provider.name)?.apply()
    }
}
