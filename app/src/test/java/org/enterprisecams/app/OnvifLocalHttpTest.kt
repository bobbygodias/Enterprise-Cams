package org.enterprisecams.app

import java.io.ByteArrayInputStream
import org.enterprisecams.app.playback.OnvifLocalHttp
import org.junit.Assert.*
import org.junit.Test

class OnvifLocalHttpTest {
    private fun response(headers: String, body: String) =
        ByteArrayInputStream(("HTTP/1.1 200 OK\r\n" + headers + "\r\n" + body).toByteArray())

    @Test fun decodesBoundedLengthAndChunkedBodies() {
        assertEquals("<ok/>", OnvifLocalHttp.readResponse(
            response("Content-Length: 5\r\n", "<ok/>")).toString(Charsets.UTF_8))
        assertEquals("<ok/>", OnvifLocalHttp.readResponse(
            response("Transfer-Encoding: chunked\r\n", "5\r\n<ok/>\r\n0\r\n\r\n")
        ).toString(Charsets.UTF_8))
    }

    @Test fun rejectsRedirectsTruncationAndSmuggling() {
        val bad = listOf(
            "HTTP/1.1 302 Found\r\nLocation: http://192.168.1.99/\r\n\r\n",
            "HTTP/1.1 200 OK\r\nContent-Length: 500\r\n\r\n<ok/>",
            "HTTP/1.1 200 OK\r\nContent-Length: 5\r\nTransfer-Encoding: chunked\r\n\r\n5\r\n<ok/>\r\n0\r\n\r\n",
            "HTTP/1.1 200 OK\r\nContent-Length: 5\r\nContent-Length: 5\r\n\r\n<ok/>",
            "HTTP/1.1 200 OK\r\nContent-Length: 1048577\r\n\r\n<ok/>",
            "HTTP/1.1 200 OK\r\nTransfer-Encoding: gzip\r\n\r\n<ok/>"
        )
        bad.forEach { raw ->
            assertTrue(runCatching {
                OnvifLocalHttp.readResponse(ByteArrayInputStream(raw.toByteArray()))
            }.isFailure)
        }
    }
}
