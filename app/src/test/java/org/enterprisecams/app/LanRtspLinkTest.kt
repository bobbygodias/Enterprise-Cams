package org.enterprisecams.app

import org.enterprisecams.app.playback.LanRtspLink
import org.junit.Assert.*
import org.junit.Test

class LanRtspLinkTest {
    @Test fun acceptsExplicitPrivateCamera() {
        listOf("rtsp://192.168.1.20:554/live", "rtsp://10.2.3.4/video",
            "rtsp://172.16.0.2/stream", "rtsp://172.31.255.1/stream").forEach {
            assertEquals(it, LanRtspLink.parse(it).value)
        }
    }

    @Test fun credentialsNeverAppearInToString() {
        val url = "rtsp://owner:secret@192.168.1.20/live"
        assertEquals(url, LanRtspLink.parse(url).value)
        assertEquals("LanRtspLink(<redacted>)", LanRtspLink.parse(url).toString())
    }

    @Test fun rejectsPublicAndAmbiguousAddresses() {
        listOf("rtsp://8.8.8.8/live", "rtsp://172.32.0.1/live",
            "rtsp://127.0.0.1/live", "rtsp://camera.local/live",
            "rtsp://192.168.001.2/live", "rtsp://192.168.1.2.evil/live",
            "rtsp://192.168.1.2@8.8.8.8/live",
            "http://192.168.1.2/live", "rtsp://192.168.1.2:65536/live",
            "rtsp://192.168.1.2/live#x", "rtsp://192.168.1.2",
            "rtsp://192.168.1.2/li\nve").forEach {
            assertTrue(it, runCatching { LanRtspLink.parse(it) }.isFailure)
        }
    }

    @org.junit.Test fun directCredentialsAreEncodedAndRedacted() {
        val link = org.enterprisecams.app.playback.LanRtspLink.authenticated(
            "rtsp://192.168.1.50:554/live?channel=1", "camera@example.org", "p@ss#? /")
        val uri = java.net.URI(link.value)
        org.junit.Assert.assertEquals("camera@example.org:p@ss#? /", uri.userInfo)
        org.junit.Assert.assertEquals("192.168.1.50", uri.host)
        org.junit.Assert.assertEquals("channel=1", uri.rawQuery)
        org.junit.Assert.assertFalse(link.toString().contains("p@ss"))
    }

    @org.junit.Test fun directCredentialsRejectPublicAndAmbiguousDestinations() {
        for (address in listOf("rtsp://8.8.8.8/live", "rtsp://other:secret@192.168.1.50/live",
            "https://192.168.1.50/live")) {
            org.junit.Assert.assertTrue(runCatching {
                org.enterprisecams.app.playback.LanRtspLink.authenticated(address, "owner", "pw")
            }.isFailure)
        }
        org.junit.Assert.assertTrue(runCatching {
            org.enterprisecams.app.playback.LanRtspLink.authenticated("rtsp://192.168.1.50/live", "bad:name", "pw")
        }.isFailure)
    }
}
