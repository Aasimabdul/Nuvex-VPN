package com.nuvex.vpn.provider

/**
 * Client-safe configuration only. Never put provider API keys or backend secrets here.
 * The production API base URL must be configured at build/deployment time.
 */
data class ProviderConfig(
    val apiBaseUrl: String?,
    val providerId: String?,
    val enabled: Boolean
) {
    val isConfigured: Boolean
        get() = enabled && !apiBaseUrl.isNullOrBlank() && !providerId.isNullOrBlank()
}
