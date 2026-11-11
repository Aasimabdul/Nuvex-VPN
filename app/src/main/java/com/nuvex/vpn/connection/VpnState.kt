package com.nuvex.vpn.connection

sealed interface VpnState {
    data object Disconnected : VpnState
    data object Connecting : VpnState
    data class Connected(
        val locationId: String,
        val connectedAtEpochMillis: Long
    ) : VpnState
    data object Reconnecting : VpnState
    data object Disconnecting : VpnState
    data object PermissionRequired : VpnState
    data object ProviderNotConfigured : VpnState
    data class Error(val userMessage: String) : VpnState
}
