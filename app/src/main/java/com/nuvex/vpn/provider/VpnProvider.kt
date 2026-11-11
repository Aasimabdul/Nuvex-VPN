package com.nuvex.vpn.provider

/**
 * Provider-neutral contract. Implementations must call only an authorized provider API.
 * Provider secrets belong on the backend, never in this Android process.
 */
interface VpnProvider {
    val providerId: String

    suspend fun listLocations(): List<VpnLocation>

    /** Returns a short-lived WireGuard profile from an authorized provider. */
    suspend fun createSession(request: SessionRequest): ProviderSession

    suspend fun revokeSession(providerSessionId: String)
}

data class VpnLocation(
    val id: String,
    val countryCode: String,
    val countryName: String,
    val city: String?,
    val supportedProtocols: Set<String>,
    val available: Boolean
)

data class SessionRequest(
    val userId: String,
    val deviceId: String,
    val locationId: String?,
    val protocol: String = "wireguard"
)

data class ProviderSession(
    val id: String,
    val locationId: String,
    val protocol: String,
    /** Treat as secret; do not log or persist unencrypted. */
    val configuration: String,
    val expiresAtEpochSeconds: Long?
)

class ProviderNotConfiguredException :
    IllegalStateException("VPN provider is not configured. Contact the app administrator.")

class ProviderUnavailableException(message: String, cause: Throwable? = null) :
    IllegalStateException(message, cause)
