package com.budmash.data

sealed class ParseError {
    data class NetworkError(val message: String) : ParseError()
    data class LlmError(val message: String) : ParseError()
    data class ParseFailure(val message: String) : ParseError()
    data object NoFlowersFound : ParseError()

    fun toUserMessage(): String = when (this) {
        is NetworkError -> "Network error: $message"
        is LlmError -> "AI processing error: $message"
        is ParseFailure -> "Failed to parse menu: $message"
        is NoFlowersFound -> "No flower products found on this menu"
    }
}
