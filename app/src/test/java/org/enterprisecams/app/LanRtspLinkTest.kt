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
}
