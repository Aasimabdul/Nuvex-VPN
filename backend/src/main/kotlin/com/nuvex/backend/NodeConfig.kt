package com.nuvex.backend

/**
 * One WireGuard exit node that this backend manages. The backend runs on the same
 * machine as the node and adds/removes peers on the local WireGuard interface.
 */
data class NodeConfig(
    val id: String,
    val countryCode: String,
    val countryName: String,
    val city: String?,
    /** Public host:port that clients connect to, for example 203.0.113.10:51820. */
    val endpoint: String,
    /** The node's WireGuard public key (safe to share). */
    val publicKey: String,
    val interfaceName: String,
    /** First three octets plus a dot, for example "10.66.0.". Hosts .2 to .254 are handed out. */
    val subnetPrefix: String,
    val dns: String,
    val trustProxyHeader: Boolean
) {
    companion object {
        private val keyPattern = Regex("^[A-Za-z0-9+/]{43}=$")
        private val subnetPattern = Regex("^[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.$")
        private val interfacePattern = Regex("^[A-Za-z0-9_=+.-]{1,15}$")

        /** Returns null when the required settings are missing, so the API fails closed. */
        fun fromEnv(env: Map<String, String>): NodeConfig? {
            fun value(name: String): String? = env[name]?.trim()?.takeIf { it.isNotEmpty() }

            val id = value("NUVEX_NODE_ID") ?: return null
            val countryCode = value("NUVEX_NODE_COUNTRY_CODE") ?: return null
            val countryName = value("NUVEX_NODE_COUNTRY_NAME") ?: return null
            val endpoint = value("NUVEX_NODE_ENDPOINT") ?: return null
            val publicKey = value("NUVEX_NODE_PUBLIC_KEY") ?: return null
            if (!keyPattern.matches(publicKey)) return null
            if (countryCode.length != 2) return null

            val subnetPrefix = value("NUVEX_NODE_SUBNET_PREFIX") ?: "10.66.0."
            if (!subnetPattern.matches(subnetPrefix)) return null
            val interfaceName = value("NUVEX_WG_INTERFACE") ?: "wg0"
            if (!interfacePattern.matches(interfaceName)) return null

            return NodeConfig(
                id = id,
                countryCode = countryCode.uppercase(),
                countryName = countryName,
                city = value("NUVEX_NODE_CITY"),
                endpoint = endpoint,
                publicKey = publicKey,
                interfaceName = interfaceName,
                subnetPrefix = subnetPrefix,
                dns = value("NUVEX_NODE_DNS") ?: "1.1.1.1",
                trustProxyHeader = value("NUVEX_TRUST_PROXY_HEADER") == "true"
            )
        }
    }
}
