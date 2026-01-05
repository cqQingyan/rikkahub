package me.rerere.ai.provider

sealed class ProviderError(override val message: String, override val cause: Throwable? = null) : Exception(message, cause) {
    data class NetworkError(val originalMessage: String, override val cause: Throwable? = null) : ProviderError("Network error: $originalMessage", cause)
    data class ApiError(val code: Int, val errorBody: String) : ProviderError("API Error $code: $errorBody")
    data class ParsingError(val originalMessage: String, override val cause: Throwable? = null) : ProviderError("Parsing error: $originalMessage", cause)
    data class InvalidConfiguration(val originalMessage: String) : ProviderError("Invalid configuration: $originalMessage")
    data class UnknownError(val originalMessage: String, override val cause: Throwable? = null) : ProviderError("Unknown error: $originalMessage", cause)
}
