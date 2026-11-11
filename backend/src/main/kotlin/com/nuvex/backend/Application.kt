package com.nuvex.backend

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.origin
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class HealthResponse(val status: String, val providerConfigured: Boolean)

@Serializable
data class ApiError(val code: String, val message: String)

@Serializable
data class LocationDto(
    val id: String,
    val countryCode: String,
    val countryName: String,
    val city: String? = null,
    val protocols: List<String>,
    val available: Boolean
)

@Serializable
data class CreateSessionRequest(
    val deviceId: String,
    val publicKey: String,
    val locationId: String? = null
)

@Serializable
data class SessionResponse(
    val sessionId: String,
    val locationId: String,
    val protocol: String,
    /** Address for the client interface, with prefix length. */
    val address: String,
    val serverPublicKey: String,
    val endpoint: String,
    val dns: String,
    val allowedIps: String,
    val expiresAtEpochSeconds: Long
)

/** Entry point used by the Ktor engine. Reads the node settings from environment variables. */
fun Application.module() {
    val node = NodeConfig.fromEnv(System.getenv())
    val service = node?.let { SessionService(it, CommandPeerProvisioner(it.interfaceName)) }
    service?.recover()

    if (service != null) {
        // Remove expired peers every five minutes.
        launch {
            while (isActive) {
                delay(5L * 60L * 1000L)
                service.removeExpired()
            }
        }
    }
    nuvexModule(node, service)
}

/** Routes and plugins. Split from [module] so tests can pass a fake service. */
fun Application.nuvexModule(node: NodeConfig?, service: SessionService?) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; explicitNulls = false })
    }

    val limiter = RateLimiter(maxRequests = 30, windowMillis = 60L * 60L * 1000L)

    routing {
        get("/api/v1/health") {
            call.respond(HealthResponse("ok", node != null && service != null))
        }
        route("/api/v1") {
            get("/servers") {
                if (node == null) {
                    call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        ApiError("NODE_NOT_CONFIGURED", "No VPN node is configured for this deployment.")
                    )
                    return@get
                }
                val location = LocationDto(
                    id = node.id,
                    countryCode = node.countryCode,
                    countryName = node.countryName,
                    city = node.city,
                    protocols = listOf("wireguard"),
                    available = true
                )
                call.respond(listOf(location))
            }

            post("/sessions") {
                if (node == null || service == null) {
                    call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        ApiError("NODE_NOT_CONFIGURED", "No VPN node is configured for this deployment.")
                    )
                    return@post
                }
                val client = clientAddress(call.request.headers["X-Forwarded-For"], call.request.origin.remoteHost, node.trustProxyHeader)
                if (!limiter.allow(client)) {
                    call.respond(
                        HttpStatusCode.TooManyRequests,
                        ApiError("RATE_LIMITED", "Too many requests. Please try again later.")
                    )
                    return@post
                }
                val body = runCatching { call.receive<CreateSessionRequest>() }.getOrNull()
                if (body == null) {
                    call.respond(HttpStatusCode.BadRequest, ApiError("INVALID_REQUEST", "The request body is not valid."))
                    return@post
                }
                if (body.locationId != null && body.locationId != node.id) {
                    call.respond(HttpStatusCode.NotFound, ApiError("LOCATION_NOT_FOUND", "That location does not exist."))
                    return@post
                }
                try {
                    val session = service.create(body.deviceId, body.publicKey)
                    call.respond(
                        HttpStatusCode.Created,
                        SessionResponse(
                            sessionId = session.id,
                            locationId = node.id,
                            protocol = "wireguard",
                            address = session.address + "/32",
                            serverPublicKey = node.publicKey,
                            endpoint = node.endpoint,
                            dns = node.dns,
                            allowedIps = "0.0.0.0/0, ::/0",
                            expiresAtEpochSeconds = session.expiresAtMillis / 1000L
                        )
                    )
                } catch (e: SessionException) {
                    call.respond(e.status, ApiError(e.code, e.message))
                }
            }

            delete("/sessions/{id}") {
                val id = call.parameters["id"]
                if (service == null || id == null) {
                    call.respond(HttpStatusCode.NotFound, ApiError("SESSION_NOT_FOUND", "That session does not exist."))
                    return@delete
                }
                if (service.revoke(id)) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound, ApiError("SESSION_NOT_FOUND", "That session does not exist."))
                }
            }
        }
    }
}

/**
 * Picks the address used for rate limiting. Only trust X-Forwarded-For when a reverse proxy
 * that overwrites it (for example Caddy) is the only way to reach this service.
 */
fun clientAddress(forwardedFor: String?, remoteHost: String, trustProxyHeader: Boolean): String {
    if (trustProxyHeader && !forwardedFor.isNullOrBlank()) {
        return forwardedFor.substringBefore(",").trim()
    }
    return remoteHost
}
