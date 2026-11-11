package com.nuvex.backend

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApplicationTest {

    @Test
    fun serversFailClosedWhenNoNodeIsConfigured() = testApplication {
        application { nuvexModule(null, null) }
        val response = client.get("/api/v1/servers")
        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        assertTrue(response.bodyAsText().contains("NODE_NOT_CONFIGURED"))
    }

    @Test
    fun serversListTheConfiguredCountry() = testApplication {
        val node = testNode()
        application { nuvexModule(node, SessionService(node, FakeProvisioner())) }
        val response = client.get("/api/v1/servers")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Singapore"))
    }

    @Test
    fun sessionReturnsConfigWithoutAnyPrivateKey() = testApplication {
        val node = testNode()
        val provisioner = FakeProvisioner()
        application { nuvexModule(node, SessionService(node, provisioner)) }
        val response = client.post("/api/v1/sessions") {
            contentType(ContentType.Application.Json)
            setBody("""{"deviceId":"device-0001","publicKey":"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="}""")
        }
        assertEquals(HttpStatusCode.Created, response.status)
        val text = response.bodyAsText()
        assertTrue(text.contains("10.66.0.2/32"))
        assertTrue(text.contains("203.0.113.10:51820"))
        assertFalse(text.contains("PrivateKey"))
        assertEquals(1, provisioner.peers.size)
    }

    @Test
    fun invalidKeyIsRejected() = testApplication {
        val node = testNode()
        application { nuvexModule(node, SessionService(node, FakeProvisioner())) }
        val response = client.post("/api/v1/sessions") {
            contentType(ContentType.Application.Json)
            setBody("""{"deviceId":"device-0001","publicKey":"not-a-key"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
