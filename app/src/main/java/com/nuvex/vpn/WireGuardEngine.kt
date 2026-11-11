package com.nuvex.vpn

import android.content.Context
import android.net.VpnService
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import java.io.BufferedReader
import java.io.StringReader

/**
 * Thin adapter around WireGuard's maintained Android tunnel backend.
 *
 * The app must obtain a real, trusted WireGuard profile from its gateway service before
 * calling connect(). Never pass a fabricated profile or report a connection before the
 * backend confirms State.UP.
 */
class WireGuardEngine(context: Context) {
    private val appContext = context.applicationContext
    private val backend = GoBackend(appContext)
    private var activeTunnel: AppTunnel? = null

    @Synchronized
    @Throws(Exception::class)
    fun connect(tunnelName: String, profileText: String, onStateChanged: (Tunnel.State) -> Unit) {
        require(tunnelName.matches(Regex("[A-Za-z0-9_=+.-]{1,15}"))) {
            "Invalid tunnel name"
        }
        check(VpnService.prepare(appContext) == null) {
            "Android VPN permission has not been granted"
        }
        require(profileText.isNotBlank()) { "No VPN profile was supplied" }

        val parsedConfig = Config.parse(BufferedReader(StringReader(profileText)))
        val tunnel = AppTunnel(tunnelName, onStateChanged)
        // setState only returns UP after the backend has successfully established the tunnel.
        backend.setState(tunnel, Tunnel.State.UP, parsedConfig)
        activeTunnel = tunnel
    }

    @Synchronized
    @Throws(Exception::class)
    fun disconnect() {
        val tunnel = activeTunnel ?: return
        backend.setState(tunnel, Tunnel.State.DOWN, null)
        activeTunnel = null
    }

    @Synchronized
    fun currentState(): Tunnel.State =
        activeTunnel?.let { backend.getState(it) } ?: Tunnel.State.DOWN

    private class AppTunnel(
        private val tunnelName: String,
        private val stateListener: (Tunnel.State) -> Unit
    ) : Tunnel {
        override fun getName(): String = tunnelName

        override fun onStateChange(newState: Tunnel.State) {
            stateListener(newState)
        }
    }
}
