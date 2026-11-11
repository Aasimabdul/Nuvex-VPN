package com.nuvex.vpn.provider

sealed interface ProviderStatus {
    data object NotConfigured : ProviderStatus
    data class Ready(val providerId: String) : ProviderStatus
}

class VpnProviderRepository(
    private val config: ProviderConfig,
    private val adapter: ProviderAdapter?
) {
    fun status(): ProviderStatus =
        if (config.isConfigured && adapter != null) {
            ProviderStatus.Ready(adapter.providerId)
        } else {
            ProviderStatus.NotConfigured
        }

    suspend fun listLocations(): List<VpnLocation> = requireProvider().listLocations()

    suspend fun createSession(request: SessionRequest): ProviderSession =
        requireProvider().createSession(request)

    suspend fun revokeSession(providerSessionId: String) =
        requireProvider().revokeSession(providerSessionId)

    private fun requireProvider(): ProviderAdapter {
        if (!config.isConfigured || adapter == null) throw ProviderNotConfiguredException()
        return adapter
    }
}
