package com.budmash.llm

import platform.Foundation.NSUserDefaults

actual class LlmConfigStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun getApiKey(): String? = defaults.stringForKey("budmash_api_key")

    actual fun setApiKey(key: String) {
        defaults.setObject(key, "budmash_api_key")
    }

    actual fun getModel(): String = defaults.stringForKey("budmash_model") ?: "anthropic/claude-3-haiku"

    actual fun setModel(model: String) {
        defaults.setObject(model, "budmash_model")
    }

    actual fun getProvider(): LlmProviderType {
        val name = defaults.stringForKey("budmash_provider") ?: "OPENROUTER"
        return try {
            LlmProviderType.valueOf(name)
        } catch (e: IllegalArgumentException) {
            LlmProviderType.OPENROUTER
        }
    }

    actual fun setProvider(provider: LlmProviderType) {
        defaults.setObject(provider.name, "budmash_provider")
    }
}
