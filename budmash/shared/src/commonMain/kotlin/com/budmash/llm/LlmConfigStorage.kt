package com.budmash.llm

expect class LlmConfigStorage() {
    fun getApiKey(): String?
    fun setApiKey(key: String)
    fun getModel(): String
    fun setModel(model: String)
    fun getVisionModel(): String
    fun setVisionModel(model: String)
    fun getProvider(): LlmProviderType
    fun setProvider(provider: LlmProviderType)
}
