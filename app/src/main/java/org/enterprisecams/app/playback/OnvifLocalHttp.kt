package org.enterprisecams.app.playback

import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI

/**
 * HTTP/1.1 transport exclusively for an explicitly selected private-IPv4 ONVIF endpoint.
 * Android disallows app-wide cleartext traffic; a local socket permits this narrowly scoped
 * exception without permitting HTTP media links or cloud credentials over plaintext.
 * WS-Security PasswordDigest is provided by OnvifMediaClient, not by this transport.
 */
internal object OnvifLocalHttp {
    private const val MAX_BODY = 1_048_576
    private const val MAX_HEADERS = 16_384
    private const val TIMEOUT_MS = 8000

    fun post(endpoint: URI, body: ByteArray, action: String): ByteArray {
        require(action in setOf(
            "http://www.onvif.org/ver10/media/wsdl/GetProfiles",
            "http://www.onvif.org/ver10/media/wsdl/GetStreamUri",
            "http://www.onvif.org/ver10/device/wsdl/GetCapabilities"
        )) { "Ação ONVIF não permitida." }
        require(endpoint.scheme == "http" && OnvifMediaClient.endpoint(endpoint.toString()) == endpoint)
        require(body.size in 1..MAX_BODY)
        val port = if (endpoint.port == -1) 80 else endpoint.port
        val head = "POST " + endpoint.rawPath + " HTTP/1.1\r\n" +
            "Host: " + endpoint.host + ":" + port + "\r\n" +
            "Content-Type: application/soap+xml; charset=utf-8; action=\\"" + action + "\\"\r\n" +
            "Content-Length: " + body.size + "\r\n" +
            "Accept: application/soap+xml\r\n" +
            "Connection: close\r\n\r\n"
        Socket().use { socket ->
            socket.connect(InetSocketAddress(endpoint.host, port), TIMEOUT_MS)
            socket.soTimeout = TIMEOUT_MS
            socket.getOutputStream().use { output ->
                output.write(head.toByteArray(Charsets.US_ASCII))
                output.write(body)
                output.flush()
                // Never follow HTTP redirects: credentials must remain on the selected LAN camera.
                return readResponse(BufferedInputStream(socket.getInputStream()))
            }
        }
    }

    /** Unit-testable, size-bounded HTTP parser; no unbounded readBytes() or redirects. */
    internal fun readResponse(input: InputStream): ByteArray {
        var headerBytes = 0
        fun line(): String {
            val out = ByteArrayOutputStream()
            while (true) {
                val b = input.read()
                require(b >= 0) { "Resposta ONVIF interrompida." }
                headerBytes++
                require(headerBytes <= MAX_HEADERS) { "Cabeçalhos ONVIF grandes demais." }
                if (b == 10) {
                    val bytes = out.toByteArray()
                    require(bytes.isNotEmpty() && bytes.last() == 13.toByte()) { "HTTP ONVIF inválido." }
                    return String(bytes, 0, bytes.size - 1, Charsets.US_ASCII)
                }
                require(b in 32..126 || b == 13) { "Cabeçalho HTTP ONVIF inválido." }
                out.write(b)
            }
        }
        val status = line()
        require(status.matches(Regex("""HTTP/1\.[01] 200(?: .*)?"""))) {
            "O dispositivo ONVIF recusou a operação HTTP."
        }
        var length: Int? = null
        var chunked = false
        while (true) {
            val header = line()
            if (header.isEmpty()) break
            val index = header.indexOf(':')
            require(index > 0) { "Cabeçalho ONVIF inválido." }
            val key = header.substring(0, index).trim().lowercase(java.util.Locale.ROOT)
            val value = header.substring(index + 1).trim()
            when (key) {
                "content-length" -> {
                    require(length == null) { "Cabeçalho duplicado." }
                    length = value.toIntOrNull()?.takeIf { it in 0..MAX_BODY }
                        ?: throw IllegalArgumentException("Tamanho de resposta inválido.")
                }
                "transfer-encoding" -> {
                    require(value.equals("chunked", ignoreCase = true) && !chunked) {
                        "Codificação HTTP ONVIF não suportada."
                    }
                    chunked = true
                }
                "content-encoding" -> require(value.equals("identity", ignoreCase = true)) {
                    "Resposta ONVIF comprimida não suportada."
                }
            }
        }
        require(!(chunked && length != null)) { "Cabeçalhos de tamanho ambíguos." }
        val out = ByteArrayOutputStream()
        fun append(size: Int) {
            require(size >= 0 && out.size() + size <= MAX_BODY) { "Resposta ONVIF grande demais." }
            var remaining = size
            val buffer = ByteArray(4096)
            while (remaining > 0) {
                val count = input.read(buffer, 0, minOf(buffer.size, remaining))
                require(count > 0) { "Resposta ONVIF interrompida." }
                out.write(buffer, 0, count)
                remaining -= count
            }
        }
        if (chunked) {
            while (true) {
                val header = line().substringBefore(';').trim()
                require(header.length in 1..8 && header.all { it in "0123456789abcdefABCDEF" }) {
                    "Tamanho de bloco ONVIF inválido."
                }
                val block = header.toInt(16)
                if (block == 0) {
                    // Ignore only valid bounded HTTP trailer lines.
                    while (line().isNotEmpty()) { /* trailers */ }
                    break
                }
                append(block)
                require(input.read() == 13 && input.read() == 10) { "Bloco HTTP ONVIF inválido." }
            }
        } else if (length != null) {
            append(length)
        } else {
            val buffer = ByteArray(4096)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                require(out.size() + n <= MAX_BODY) { "Resposta ONVIF grande demais." }
                out.write(buffer, 0, n)
            }
        }
        require(out.size() in 1..MAX_BODY) { "Resposta ONVIF vazia." }
        return out.toByteArray()
    }
}
