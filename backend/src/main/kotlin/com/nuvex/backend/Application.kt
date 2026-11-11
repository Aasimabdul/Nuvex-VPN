package com.nuvex.backend

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class HealthResponse(val status: String, val providerConfigured: Boolean)

@Serializable
data class ApiError(val code: String, val message: String)

fun Application.module() {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; explicitNulls = false })
    }

    val providerConfigured = System.getenv("NUVEX_PROVIDER")?.isNotBlank() == true &&
        System.getenv("NUVEX_PROVIDER_API_KEY")?.isNotBlank() == true

    routing {
        get("/api/v1/health") {
            call.respond(HealthResponse("ok", providerConfigured))
        }
        route("/api/v1") {
            get("/servers") {
                if (!providerConfigured) {
                    call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        ApiError("PROVIDER_NOT_CONFIGURED",
                            "No authorized VPN provider is configured for this deployment.")
                    )
                } else {
                    // Deliberately fail closed until a reviewed provider adapter is installed.
                    call.respond(
                        HttpStatusCode.NotImplemented,
                        ApiError("PROVIDER_ADAPTER_PENDING",
                            "Provider credentials exist, but the provider adapter is not implemented.")
                    )
                }
            }
        }
    }
}
