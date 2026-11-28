package org.enterprisecams.app.cloud

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException

class OfficialCloudHttpTest {
    private val host = "openapi-iot.cloudlinks.cn"

    @Test fun unknownHostIsBlockedBeforeAnyNetworkCall() {
        var opened = false
        val client = OfficialCloudHttp(setOf(host)) {
            opened = true
            throw IOException("must not connect")
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                client.post(OfficialCloudHttp.Request("example.org", "/login", "{}".toByteArray()))
            }
        }
        assertFalse(opened)
    }

    @Test fun ambiguousPathsAreBlockedWithoutNetwork() {
        val client = OfficialCloudHttp(setOf(host)) { throw IOException("must not connect") }
        for (path in listOf("//evil/path", "/path/../admin", "/path?token=secret",
            "/path#fragment", "/a\\b", "login")) {
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    client.post(OfficialCloudHttp.Request(host, path, "{}".toByteArray()))
                }
            }
        }
    }

    @Test fun headerInjectionAndOversizedBodiesAreBlocked() {
        val client = OfficialCloudHttp(setOf(host)) { throw IOException("must not connect") }
        for (headers in listOf(mapOf("Authorization" to "token\r\nX-Extra: bad"),
            mapOf("Host" to "evil"))) {
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    client.post(OfficialCloudHttp.Request(host, "/session", "{}".toByteArray(), headers))
                }
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                client.post(OfficialCloudHttp.Request(host, "/session", ByteArray(262145)))
            }
        }
    }

    @Test fun requestAndResponseToStringRedactSensitiveFields() {
        val req = OfficialCloudHttp.Request(host, "/login", "password=private".toByteArray(),
            mapOf("Authorization" to "Bearer sensitive"))
        assertFalse(req.toString().contains("private"))
        assertFalse(req.toString().contains("sensitive"))
        val response = OfficialCloudHttp.Response(200, "accessToken=secret".toByteArray())
        assertEquals("accessToken=secret", response.utf8())
        assertFalse(response.toString().contains("secret"))
    }

    @Test fun misconfiguredDomainsAreRejected() {
        for (domain in listOf("http://example.org", "127.0.0.1", "foo",
            "evil.com:8443", "evil.com/track")) {
            assertThrows(IllegalArgumentException::class.java) {
                OfficialCloudHttp(setOf(domain))
            }
        }
    }
}
