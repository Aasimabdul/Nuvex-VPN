package com.nuvex.vpn.provider

import com.wireguard.crypto.KeyPair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Talks to the Nuvex backend. The WireGuard key pair is created on this device and only the
 * public key is sent, so the backend never receives the private key.
 */
class SelfHostedProvider(apiBaseUrl: String) : ProviderAdapter {
    private val baseUrl = apiBaseUrl.trim().trimEnd('/')

    override val providerId: String = "nuvex"

    init {
        require(baseUrl.startsWith("https://")) { "The Nuvex API address must use https." }
    }

    override suspend fun listLocations(): List<VpnLocation> = withContext(Dispatchers.IO) {
        val body = request("GET", "/api/v1/servers", null)
        val array = JSONArray(body)
        val result = mutableListOf<VpnLocation>()
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val protocols = mutableSetOf<String>()
            val protocolArray = item.optJSONArray("protocols")
            if (protocolArray != null) {
                for (p in 0 until protocolArray.length()) protocols.add(protocolArray.getString(p))
            }
            result.add(
                VpnLocation(
                    id = item.getString("id"),
                    countryCode = item.getString("countryCode"),
                    countryName = item.getString("countryName"),
                    city = if (item.has("city") && !item.isNull("city")) item.getString("city") else null,
                    supportedProtocols = protocols,
                    available = item.optBoolean("available", true)
                )
            )
        }
        result
    }

    override suspend fun createSession(request: SessionRequest): ProviderSession = withContext(Dispatchers.IO) {
        val keyPair = KeyPair()
        val payload = JSONObject()
            .put("deviceId", request.deviceId)
            .put("publicKey", keyPair.publicKey.toBase64())
        if (request.locationId != null) payload.put("locationId", request.locationId)

        val json = JSONObject(request("POST", "/api/v1/sessions", payload.toString()))
        val configuration = buildString {
            append("[Interface]\n")
            append("PrivateKey = ").append(keyPair.privateKey.toBase64()).append('\n')
            append("Address = ").append(json.getString("address")).append('\n')
            append("DNS = ").append(json.getString("dns")).append("\n\n")
            append("[Peer]\n")
            append("PublicKey = ").append(json.getString("serverPublicKey")).append('\n')
            append("AllowedIPs = ").append(json.getString("allowedIps")).append('\n')
            append("Endpoint = ").append(json.getString("endpoint")).append('\n')
            append("PersistentKeepalive = 25\n")
        }
        ProviderSession(
            id = json.getString("sessionId"),
            locationId = json.getString("locationId"),
            protocol = json.getString("protocol"),
            configuration = configuration,
            expiresAtEpochSeconds = json.optLong("expiresAtEpochSeconds", 0L).takeIf { it > 0L }
        )
    }

    override suspend fun revokeSession(providerSessionId: String): Unit = withContext(Dispatchers.IO) {
        request("DELETE", "/api/v1/sessions/" + java.net.URLEncoder.encode(providerSessionId, "UTF-8"), null)
        Unit
    }

    private fun request(method: String, path: String, body: String?): String {
        val connection = URL(baseUrl + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Accept", "application/json")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) {
                throw ProviderUnavailableException(describeError(code, text))
            }
            return text
        } catch (e: IOException) {
            throw ProviderUnavailableException("Could not reach the Nuvex server. Check your internet connection.", e)
        } finally {
            connection.disconnect()
        }
    }

    private fun describeError(code: Int, text: String): String {
        val message = runCatching { JSONObject(text).optString("message", "") }.getOrDefault("")
        return when {
            message.isNotBlank() -> message
            code == 429 -> "Too many attempts. Please try again later."
            else -> String.format(Locale.US, "The Nuvex server returned error %d.", code)
        }
    }
}
