package org.enterprisecams.app

import org.enterprisecams.app.playback.OnvifDiscovery
import org.junit.Assert.*
import org.junit.Test

class OnvifDiscoveryTest {
    private val ns = "http://schemas.xmlsoap.org/ws/2005/04/discovery"
    private fun xml(addresses: String) =
        ("<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" " +
         "xmlns:d=\"" + ns + "\"><s:Body><d:ProbeMatches><d:ProbeMatch>" +
         "<d:XAddrs>" + addresses + "</d:XAddrs>" +
         "</d:ProbeMatch></d:ProbeMatches></s:Body></s:Envelope>").toByteArray()

    @Test fun acceptsOnlyPrivateLanOnvifServiceAddresses() {
        val list = OnvifDiscovery.candidates(xml(
            "http://192.168.1.20/onvif/device_service https://10.0.0.5:8443/onvif/media_service " +
                "http://8.8.8.8/onvif/device_service http://camera.example/onvif/device_service " +
                "http://192.168.1.20/onvif/device_service"))
        assertEquals(listOf("http://192.168.1.20/onvif/device_service",
            "https://10.0.0.5:8443/onvif/media_service"), list)
    }

    @Test fun rejectsForeignXmlAndExternalEntities() {
        assertTrue(OnvifDiscovery.candidates("<x>XAddrs http://192.168.1.20/</x>".toByteArray()).isEmpty())
        assertTrue(OnvifDiscovery.candidates(
            """<!DOCTYPE a [<!ENTITY a SYSTEM "file:///etc/passwd">]><a>&a;</a>""".toByteArray()).isEmpty())
    }

    @Test fun probeTargetsNetworkVideoTransmitters() {
        val bytes = OnvifDiscovery.probe("12345678-1234-1234-1234-123456789abc")
        val text = bytes.toString(Charsets.UTF_8)
        assertTrue(text.contains("NetworkVideoTransmitter"))
        assertTrue(text.contains("12345678-1234-1234-1234-123456789abc"))
    }
}
