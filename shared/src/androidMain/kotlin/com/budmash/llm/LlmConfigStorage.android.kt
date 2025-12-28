package com.budmash.llm

import android.content.Context
import android.content.SharedPreferences

actual class LlmConfigStorage {
    companion object {
        private var sharedPrefs: SharedPreferences? = null

        fun init(context: Context) {
            if (sharedPrefs == null) {
                sharedPrefs = context.applicationContext.getSharedPreferences("budmash_llm", Context.MODE_PRIVATE)
            }
        }
    }

    private val prefs: SharedPreferences?
        get() = sharedPrefs

    actual fun getApiKey(): String? = prefs?.getString("api_key", null)

    actual fun setApiKey(key: String) {
        prefs?.edit()?.putString("api_key", key)?.apply()
    }

    actual fun getModel(): String = prefs?.getString("model", "anthropic/claude-3-haiku") ?: "anthropic/claude-3-haiku"

    actual fun setModel(model: String) {
        prefs?.edit()?.putString("model", model)?.apply()
    }

    actual fun getVisionModel(): String = prefs?.getString("vision_model", "google/gemini-2.0-flash-001") ?: "google/gemini-2.0-flash-001"

    actual fun setVisionModel(model: String) {
        prefs?.edit()?.putString("vision_model", model)?.apply()
    }

    actual fun getProvider(): LlmProviderType {
        val name = prefs?.getString("provider", "OPENROUTER") ?: "OPENROUTER"
        return try {
            LlmProviderType.valueOf(name)
        } catch (e: IllegalArgumentException) {
            LlmProviderType.OPENROUTER
        }
    }

    actual fun setProvider(provider: LlmProviderType) {
        prefs?.edit()?.putString("provider", provider.name)?.apply()
    }
}
