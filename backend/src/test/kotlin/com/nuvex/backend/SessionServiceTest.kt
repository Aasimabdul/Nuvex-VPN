package com.nuvex.backend

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val KEY_A = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
private const val KEY_B = "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="
private const val KEY_C = "CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC="

class FakeProvisioner : PeerProvisioner {
    val peers = mutableMapOf<String, String>()
    override fun addPeer(publicKey: String, address: String) { peers[publicKey] = address }
    override fun removePeer(publicKey: String) { peers.remove(publicKey) }
    override fun listPeers(): Map<String, String> = peers.toMap()
}

fun testNode() = NodeConfig(
    id = "sg-1",
    countryCode = "SG",
    countryName = "Singapore",
    city = null,
    endpoint = "203.0.113.10:51820",
    publicKey = KEY_C,
    interfaceName = "wg0",
    subnetPrefix = "10.66.0.",
    dns = "1.1.1.1",
    trustProxyHeader = false
)

class SessionServiceTest {
    private var now = 1_000_000L

    private fun service(provisioner: FakeProvisioner, ttl: Long = 60_000L): SessionService {
        var counter = 0
        return SessionService(
            node = testNode(),
            provisioner = provisioner,
            ttlMillis = ttl,
            clock = { now },
            newId = { counter += 1; "session-" + counter }
        )
    }

    @Test
    fun assignsDifferentAddressesToDifferentDevices() {
        val provisioner = FakeProvisioner()
        val service = service(provisioner)
        val first = service.create("device-0001", KEY_A)
        val second = service.create("device-0002", KEY_B)
        assertEquals("10.66.0.2", first.address)
        assertEquals("10.66.0.3", second.address)
        assertEquals(2, provisioner.peers.size)
    }

    @Test
    fun reconnectingDeviceReplacesItsOldPeer() {
        val provisioner = FakeProvisioner()
        val service = service(provisioner)
        service.create("device-0001", KEY_A)
        val again = service.create("device-0001", KEY_B)
        assertEquals(1, provisioner.peers.size)
        assertTrue(provisioner.peers.containsKey(KEY_B))
        assertFalse(provisioner.peers.containsKey(KEY_A))
        assertEquals("10.66.0.2", again.address)
    }

    @Test
    fun rejectsInvalidKeyAndDevice() {
        val service = service(FakeProvisioner())
        val badKey = assertFailsWith<SessionException> { service.create("device-0001", "--help") }
        assertEquals("INVALID_KEY", badKey.code)
        val badDevice = assertFailsWith<SessionException> { service.create("x", KEY_A) }
        assertEquals("INVALID_DEVICE", badDevice.code)
    }

    @Test
    fun rejectsKeyUsedByAnotherDevice() {
        val service = service(FakeProvisioner())
        service.create("device-0001", KEY_A)
        val error = assertFailsWith<SessionException> { service.create("device-0002", KEY_A) }
        assertEquals("KEY_IN_USE", error.code)
    }

    @Test
    fun expiredSessionsAreRemovedFromTheNode() {
        val provisioner = FakeProvisioner()
        val service = service(provisioner, ttl = 1_000L)
        service.create("device-0001", KEY_A)
        now += 2_000L
        assertEquals(1, service.removeExpired())
        assertTrue(provisioner.peers.isEmpty())
        assertEquals(0, service.activeCount())
    }

    @Test
    fun revokeRemovesThePeer() {
        val provisioner = FakeProvisioner()
        val service = service(provisioner)
        val session = service.create("device-0001", KEY_A)
        assertTrue(service.revoke(session.id))
        assertTrue(provisioner.peers.isEmpty())
        assertFalse(service.revoke(session.id))
    }

    @Test
    fun recoverRebuildsSessionsFromExistingPeers() {
        val provisioner = FakeProvisioner()
        provisioner.peers[KEY_A] = "10.66.0.7"
        provisioner.peers[KEY_B] = "192.168.1.5"
        val service = service(provisioner)
        service.recover()
        assertEquals(1, service.activeCount())
        val next = service.create("device-0001", KEY_C)
        assertEquals("10.66.0.2", next.address)
    }

    @Test
    fun rateLimiterBlocksAfterTheLimit() {
        var time = 0L
        val limiter = RateLimiter(maxRequests = 2, windowMillis = 1_000L, clock = { time })
        assertTrue(limiter.allow("a"))
        assertTrue(limiter.allow("a"))
        assertFalse(limiter.allow("a"))
        assertTrue(limiter.allow("b"))
        time += 1_500L
        assertTrue(limiter.allow("a"))
    }

    @Test
    fun clientAddressOnlyTrustsTheHeaderWhenEnabled() {
        assertEquals("9.9.9.9", clientAddress("1.2.3.4, 5.6.7.8", "9.9.9.9", false))
        assertEquals("1.2.3.4", clientAddress("1.2.3.4, 5.6.7.8", "9.9.9.9", true))
        assertEquals("9.9.9.9", clientAddress(null, "9.9.9.9", true))
    }
}
