package com.nuvex.backend

import java.util.concurrent.TimeUnit

/** Adds and removes WireGuard peers on the node. */
interface PeerProvisioner {
    fun addPeer(publicKey: String, address: String)
    fun removePeer(publicKey: String)

    /** Returns every peer currently configured, as public key to IPv4 address (no prefix length). */
    fun listPeers(): Map<String, String>
}

/**
 * Talks to the local `wg` tool. Commands are run without a shell, and callers must validate
 * public keys first so that no argument can look like an option.
 */
class CommandPeerProvisioner(
    private val interfaceName: String,
    private val wgBinary: String = "wg"
) : PeerProvisioner {

    override fun addPeer(publicKey: String, address: String) {
        run(listOf(wgBinary, "set", interfaceName, "peer", publicKey, "allowed-ips", "$address/32"))
    }

    override fun removePeer(publicKey: String) {
        run(listOf(wgBinary, "set", interfaceName, "peer", publicKey, "remove"))
    }

    override fun listPeers(): Map<String, String> {
        val output = run(listOf(wgBinary, "show", interfaceName, "allowed-ips"))
        val peers = mutableMapOf<String, String>()
        for (line in output.lines()) {
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size < 2) continue
            val address = parts[1].substringBefore("/")
            if (parts[0].isNotEmpty() && address.matches(Regex("^[0-9.]+$"))) {
                peers[parts[0]] = address
            }
        }
        return peers
    }

    private fun run(command: List<String>): String {
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            throw IllegalStateException("wg command timed out")
        }
        if (process.exitValue() != 0) {
            // The output is not logged: it can contain key material.
            throw IllegalStateException("wg command failed with exit code " + process.exitValue())
        }
        return output
    }
}
