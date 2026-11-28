package org.enterprisecams.app

import org.enterprisecams.app.playback.OnvifMediaClient
import org.junit.Assert.*
import org.junit.Test
import java.net.URI
import java.security.MessageDigest
import java.util.Base64

class OnvifMediaClientTest {
    private val endpoint = OnvifMediaClient.endpoint("https://192.168.1.20/onvif/media_service")
    private fun response(body: String) = """<s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope"
        xmlns:m="http://www.onvif.org/ver10/media/wsdl" xmlns:t="http://www.onvif.org/ver10/schema">
        <s:Body>$body</s:Body></s:Envelope>""".toByteArray()

    @Test fun ownerCredentialsAreDigestedAndEscaped() {
        val nonce = byteArrayOf(1,2,3)
        val date = "2026-11-28T12:00:00Z"
        val expected = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1")
            .digest(nonce + date.toByteArray() + "secret".toByteArray()))
        val xml = OnvifMediaClient.envelope("<m:GetProfiles/>", "a&b", "secret", nonce, date)
        assertTrue(xml.contains("a&amp;b"))
        assertTrue(xml.contains(expected))
        assertFalse(xml.contains(">secret<"))
    }

    @Test fun parsesProfilesAndResolvesSameCamera() {
        val profiles = OnvifMediaClient.profiles(response(
            """<m:GetProfilesResponse><m:Profiles token="main"><t:Name>Principal</t:Name></m:Profiles>
            <m:Profiles token="sub"><t:Name>Sub</t:Name></m:Profiles></m:GetProfilesResponse>"""))
        assertEquals(listOf("main", "sub"), profiles.map { it.token })
        val result = OnvifMediaClient.stream(response(
            "<m:GetStreamUriResponse><m:MediaUri><t:Uri>rtsp://192.168.1.20/live?a=1&amp;b=2</t:Uri></m:MediaUri></m:GetStreamUriResponse>"),
            endpoint, "owner", "p@ss")
        assertEquals("owner:p@ss", URI(result.value).userInfo)
        assertEquals("a=1&b=2", URI(result.value).query)
        assertFalse(result.toString().contains("owner"))
    }

    @Test fun rejectsUnsafeEndpointAndCrossDeviceStream() {
        listOf("http://8.8.8.8/media", "https://8.8.8.8/media",
            "https://user:pw@192.168.1.20/media", "https://192.168.1.20/media#frag").forEach {
            assertTrue(runCatching { OnvifMediaClient.endpoint(it) }.isFailure)
        }
        assertEquals("http", OnvifMediaClient.endpoint("http://192.168.1.20/onvif/media_service").scheme)
        for (url in listOf("rtsp://192.168.1.21/live", "rtsp://8.8.8.8/live", "rtsp://other:pw@192.168.1.20/live")) {
            assertTrue(runCatching { OnvifMediaClient.stream(response("<t:Uri>$url</t:Uri>"), endpoint, "owner", "secret") }.isFailure)
        }
    }

    @Test fun rejectsDtdAndSoapFault() {
        assertTrue(runCatching { OnvifMediaClient.profiles(
            "<!DOCTYPE x [<!ENTITY y SYSTEM 'file:///etc/passwd'>]><x>&y;</x>".toByteArray()) }.isFailure)
        assertTrue(runCatching { OnvifMediaClient.profiles(response("<s:Fault/>")) }.isFailure)
    }
}
