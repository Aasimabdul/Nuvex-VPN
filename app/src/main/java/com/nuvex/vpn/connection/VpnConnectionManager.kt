package com.nuvex.vpn.connection

import android.content.Context
import android.net.VpnService
import com.nuvex.vpn.WireGuardEngine
import com.nuvex.vpn.provider.ProviderNotConfiguredException
import com.nuvex.vpn.provider.SessionRequest
import com.nuvex.vpn.provider.VpnProviderRepository
import com.wireguard.android.backend.Tunnel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Coordinates provider-issued session credentials with the existing WireGuard backend.
 * Android VPN consent must be requested by an Activity before retrying connect().
 * No configuration or private key is logged or stored by this manager.
 */
class VpnConnectionManager(
    context: Context,
    private val providers: VpnProviderRepository,
    private val engine: WireGuardEngine
) {
    private val appContext = context.applicationContext
    private val mutableState = MutableStateFlow<VpnState>(VpnState.Disconnected)
    val state: StateFlow<VpnState> = mutableState.asStateFlow()
    private var providerSessionId: String? = null
    private var activeLocationId: String? = null

    suspend fun connect(request: SessionRequest) {
        if (VpnService.prepare(appContext) != null) {
            mutableState.value = VpnState.PermissionRequired
            return
        }
        mutableState.value = VpnState.Connecting
        try {
            val session = providers.createSession(request)
            require(session.protocol.equals("wireguard", ignoreCase = true)) {
                "The selected provider session is not WireGuard-compatible."
            }
            require(session.configuration.isNotBlank()) {
                "The provider returned an empty connection profile."
            }

            withContext(Dispatchers.IO) {
                engine.connect("nuvex", session.configuration) { /* State is checked below. */ }
                check(engine.currentState() == Tunnel.State.UP) {
                    "The VPN tunnel did not reach the UP state."
                }
            }
            providerSessionId = session.id
            activeLocationId = session.locationId
            mutableState.value = VpnState.Connected(
                locationId = session.locationId,
                connectedAtEpochMillis = System.currentTimeMillis()
            )
        } catch (e: ProviderNotConfiguredException) {
            mutableState.value = VpnState.ProviderNotConfigured
        } catch (e: Exception) {
            mutableState.value = VpnState.Error(
                when {
                    e.message?.contains("permission", ignoreCase = true) == true ->
                        "VPN permission is required. Allow the Android VPN prompt and try again."
                    e.message?.contains("timeout", ignoreCase = true) == true ->
                        "The VPN connection timed out. Please try again."
                    else -> "Nuvex couldn't establish a secure VPN connection. Please try again."
                }
            )
        }
    }

    suspend fun disconnect() {
        mutableState.value = VpnState.Disconnecting
        try {
            withContext(Dispatchers.IO) { engine.disconnect() }
            val sessionId = providerSessionId
            providerSessionId = null
            activeLocationId = null
            if (sessionId != null) {
                runCatching { providers.revokeSession(sessionId) }
            }
            mutableState.value = VpnState.Disconnected
        } catch (_: Exception) {
            mutableState.value = VpnState.Error("Nuvex couldn't finish disconnecting cleanly.")
        }
    }

    fun refreshTunnelState() {
        if (mutableState.value is VpnState.Connected &&
            engine.currentState() != Tunnel.State.UP
        ) {
            mutableState.value = VpnState.Error(
                "The VPN tunnel stopped unexpectedly. Reconnect before sending sensitive traffic."
            )
        }
    }
}
