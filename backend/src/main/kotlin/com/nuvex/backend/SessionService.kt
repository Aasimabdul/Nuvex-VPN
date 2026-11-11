package com.nuvex.backend

import io.ktor.http.HttpStatusCode
import java.util.UUID

class SessionException(
    val status: HttpStatusCode,
    val code: String,
    override val message: String
) : RuntimeException(message)

data class VpnSession(
    val id: String,
    val deviceId: String,
    val publicKey: String,
    /** IPv4 address without prefix length. */
    val address: String,
    val expiresAtMillis: Long
)

/**
 * Hands out one WireGuard peer per device. The client generates its own key pair and only
 * sends the public key, so this service never sees a user's private key.
 *
 * Sessions live in memory. After a restart, [recover] rebuilds them from the peers that are
 * still configured on the interface, with a fresh expiry.
 */
class SessionService(
    private val node: NodeConfig,
    private val provisioner: PeerProvisioner,
    private val ttlMillis: Long = 24L * 60L * 60L * 1000L,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val newId: () -> String = { UUID.randomUUID().toString() }
) {
    private val sessions = mutableMapOf<String, VpnSession>()

    @Synchronized
    fun create(deviceId: String, publicKey: String): VpnSession {
        validateDeviceId(deviceId)
        validatePublicKey(publicKey)
        removeExpired()

        // One active session per device: a reconnect replaces the old peer.
        sessions.values.filter { it.deviceId == deviceId }.forEach { revokeInternal(it) }

        if (sessions.values.any { it.publicKey == publicKey }) {
            throw SessionException(HttpStatusCode.Conflict, "KEY_IN_USE", "This key is already in use.")
        }

        val address = nextFreeAddress()
            ?: throw SessionException(HttpStatusCode.ServiceUnavailable, "NODE_FULL", "This location is full. Try another one later.")

        try {
            provisioner.addPeer(publicKey, address)
        } catch (e: Exception) {
            throw SessionException(HttpStatusCode.BadGateway, "NODE_ERROR", "The server could not create a connection.")
        }

        val session = VpnSession(newId(), deviceId, publicKey, address, clock() + ttlMillis)
        sessions[session.id] = session
        return session
    }

    @Synchronized
    fun revoke(sessionId: String): Boolean {
        val session = sessions[sessionId] ?: return false
        revokeInternal(session)
        return true
    }

    @Synchronized
    fun removeExpired(): Int {
        val now = clock()
        val expired = sessions.values.filter { it.expiresAtMillis <= now }
        expired.forEach { revokeInternal(it) }
        return expired.size
    }

    @Synchronized
    fun activeCount(): Int = sessions.size

    /** Rebuilds session state from the peers already configured on the interface. */
    @Synchronized
    fun recover() {
        val peers = try {
            provisioner.listPeers()
        } catch (e: Exception) {
            return
        }
        for ((key, address) in peers) {
            if (!address.startsWith(node.subnetPrefix)) continue
            val session = VpnSession(newId(), "recovered", key, address, clock() + ttlMillis)
            sessions[session.id] = session
        }
    }

    private fun revokeInternal(session: VpnSession) {
        try {
            provisioner.removePeer(session.publicKey)
        } catch (e: Exception) {
            // Keep the session record so the cleanup job retries the removal later.
            return
        }
        sessions.remove(session.id)
    }

    private fun nextFreeAddress(): String? {
        val used = sessions.values.map { it.address }.toSet()
        for (host in 2..254) {
            val candidate = node.subnetPrefix + host
            if (candidate !in used) return candidate
        }
        return null
    }

    companion object {
        private val keyPattern = Regex("^[A-Za-z0-9+/]{43}=$")
        private val devicePattern = Regex("^[A-Za-z0-9-]{8,64}$")

        fun validatePublicKey(key: String) {
            if (!keyPattern.matches(key)) {
                throw SessionException(HttpStatusCode.BadRequest, "INVALID_KEY", "The public key is not valid.")
            }
        }

        fun validateDeviceId(deviceId: String) {
            if (!devicePattern.matches(deviceId)) {
                throw SessionException(HttpStatusCode.BadRequest, "INVALID_DEVICE", "The device id is not valid.")
            }
        }
    }
}

/** Very small in-memory limiter so one address cannot use up all peers on a node. */
class RateLimiter(
    private val maxRequests: Int,
    private val windowMillis: Long,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private val hits = mutableMapOf<String, MutableList<Long>>()

    @Synchronized
    fun allow(key: String): Boolean {
        val now = clock()
        val recent = hits.getOrPut(key) { mutableListOf() }
        recent.removeAll { it <= now - windowMillis }
        if (recent.size >= maxRequests) return false
        recent.add(now)
        if (hits.size > 10_000) {
            hits.entries.removeAll { it.value.isEmpty() }
        }
        return true
    }
}
