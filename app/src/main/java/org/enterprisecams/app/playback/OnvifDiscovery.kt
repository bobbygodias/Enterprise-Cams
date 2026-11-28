package org.enterprisecams.app.playback

import android.content.Context
import android.net.wifi.WifiManager
import java.io.ByteArrayInputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.UUID
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/**
 * Explicit user-triggered WS-Discovery probe for ONVIF devices on the same LAN.
 * No IP sweep, default passwords, persistent identifiers or background discovery.
 * Discovery is unauthenticated; user must pick an address before sending credentials.
 */
object OnvifDiscovery {
    private const val DISCOVERY_NS = "http://schemas.xmlsoap.org/ws/2005/04/discovery"
    private const val ALT_DISCOVERY_NS = "http://docs.oasis-open.org/ws-dd/ns/discovery/2009/01"
    private const val MAX_PACKET = 8192
    private const val LIMIT = 24

    internal fun probe(messageId: String): ByteArray {
        require(messageId.matches(Regex("[0-9a-fA-F-]{36}")))
        return ("""<?xml version="1.0" encoding="UTF-8"?>""" +
            """<s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope" """ +
            """xmlns:a="http://schemas.xmlsoap.org/ws/2004/08/addressing" """ +
            """xmlns:d="http://schemas.xmlsoap.org/ws/2005/04/discovery" """ +
            """xmlns:dn="http://www.onvif.org/ver10/network/wsdl">""" +
            """<s:Header><a:Action>http://schemas.xmlsoap.org/ws/2005/04/discovery/Probe</a:Action>""" +
            """<a:MessageID>uuid:""" + messageId + """</a:MessageID>""" +
            """<a:To>urn:schemas-xmlsoap-org:ws:2005:04:discovery</a:To></s:Header>""" +
            """<s:Body><d:Probe><d:Types>dn:NetworkVideoTransmitter</d:Types></d:Probe></s:Body>""" +
            """</s:Envelope>""").toByteArray(Charsets.UTF_8)
    }

    /** Parse only ProbeMatch XAddrs, never arbitrary URLs from unauthenticated XML. */
    internal fun candidates(xml: ByteArray): List<String> {
        if (xml.size !in 1..MAX_PACKET) return emptyList()
        return try {
            val text = xml.toString(Charsets.UTF_8)
            if (text.contains("<!DOCTYPE", ignoreCase = true) ||
                text.contains("<!ENTITY", ignoreCase = true) ||
                '\u0000' in text) return emptyList()
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
                isExpandEntityReferences = false
            }
            val builder = factory.newDocumentBuilder()
            builder.setEntityResolver { _, _ -> throw org.xml.sax.SAXException("External entity refused") }
            val document = builder.parse(ByteArrayInputStream(xml))
            val result = LinkedHashSet<String>()
            for (namespace in listOf(DISCOVERY_NS, ALT_DISCOVERY_NS)) {
                val matches = document.getElementsByTagNameNS(namespace, "ProbeMatch")
                for (i in 0 until minOf(matches.length, LIMIT)) {
                    val match = matches.item(i) as? Element ?: continue
                    val addresses = match.getElementsByTagNameNS(namespace, "XAddrs")
                    for (j in 0 until addresses.length) {
                        for (address in addresses.item(j).textContent.trim().split(Regex("\\s+"))) {
                            val safe = runCatching { OnvifMediaClient.endpoint(address) }.getOrNull()
                            if (safe != null) result.add(safe.toString())
                            if (result.size >= LIMIT) return result.toList()
                        }
                    }
                }
            }
            result.toList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Expected to run on Dispatchers.IO. A bounded multicast probe, not a LAN-wide
     * port scan; responses from devices that suppress WS-Discovery may be absent.
     */
    fun discover(context: Context, windowMs: Int = 3500): List<String> {
        require(windowMs in 500..8000)
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val lock = wifi?.createMulticastLock("enterprise-cams-onvif")?.apply {
            setReferenceCounted(false)
        }
        try {
            lock?.acquire()
            DatagramSocket().use { socket ->
                socket.soTimeout = 350
                val message = probe(UUID.randomUUID().toString())
                socket.send(DatagramPacket(message, message.size, InetAddress.getByName("239.255.255.250"), 3702))
                val until = System.nanoTime() + windowMs * 1_000_000L
                val result = LinkedHashSet<String>()
                while (System.nanoTime() < until && result.size < LIMIT) {
                    val buf = ByteArray(MAX_PACKET)
                    val response = DatagramPacket(buf, buf.size)
                    try {
                        socket.receive(response)
                        result.addAll(candidates(buf.copyOf(response.length)))
                    } catch (_: java.net.SocketTimeoutException) {
                        // Continue until the bounded window ends.
                    }
                }
                return result.take(LIMIT)
            }
        } finally {
            if (lock?.isHeld == true) lock.release()
        }
    }
}
