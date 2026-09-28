package org.enterprisecams.app

import org.enterprisecams.app.playback.HttpsVideoLink
import org.junit.Assert.*
import org.junit.Test

class HttpsVideoLinkTest {
    @Test fun keepsSignedQueryExactlyAsIssued() {
        val url = "https://video.example.org/live.m3u8?token=a%2Fb%2Bc&expires=1800000000"
        assertEquals(url, HttpsVideoLink.parse(url).value)
        assertFalse(HttpsVideoLink.parse(url).toString().contains("token"))
    }

    @Test fun rejectsUnsafeInputsWithoutEchoingThem() {
        listOf("http://camera.example/live", "rtsp://camera.example/live", "file:///etc/passwd",
            "content://camera/live", "data:video/mp4;base64,abc", "https://user:secret@host/live",
            "https://host/live#secret", "https:///no-host", "https://host:99999/live",
            "https://host/\nsecret", "https://host/?secret=" + "a".repeat(HttpsVideoLink.MAX_LENGTH))
            .forEach { input ->
                val error = runCatching { HttpsVideoLink.parse(input) }.exceptionOrNull()
                assertTrue("Should reject the unsafe input", error is IllegalArgumentException)
                assertFalse(error?.message.orEmpty().contains("secret"))
            }
    }
}
