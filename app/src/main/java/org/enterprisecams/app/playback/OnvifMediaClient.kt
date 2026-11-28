package org.enterprisecams.app.playback

import java.io.ByteArrayInputStream
import java.net.Proxy
import java.net.URI
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import javax.net.ssl.HttpsURLConnection
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/** ONVIF Media 1 for a private-LAN endpoint explicitly chosen by the owner. */
object OnvifMediaClient {
    private const val SOAP = "http://www.w3.org/2003/05/soap-envelope"
    private const val MEDIA = "http://www.onvif.org/ver10/media/wsdl"
    private const val DEVICE = "http://www.onvif.org/ver10/device/wsdl"
    private const val SCHEMA = "http://www.onvif.org/ver10/schema"
    private const val WSSE = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
    private const val WSU = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
    private const val MAX = 1_048_576

    data class Profile(val token: String, val name: String)

    fun endpoint(input: String): URI {
        val uri = try { URI(input.trim()) } catch (_: Exception) { throw IllegalArgumentException("Endereço ONVIF inválido.") }
        require(uri.scheme in setOf("https", "http") && uri.rawUserInfo == null && uri.rawFragment == null &&
            uri.rawQuery == null && !uri.rawPath.isNullOrBlank() && (uri.port == -1 || uri.port in 1..65535)) {
            "Informe o serviço Media ONVIF HTTP ou HTTPS de sua câmera local."
        }
        LanRtspLink.parse("rtsp://" + uri.host + "/validate")
        return uri
    }

    private fun escape(s: String): String = s.replace("&", "&amp;").replace("<", "&lt;")
        .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")

    internal fun envelope(body: String, user: String, password: String,
                          nonce: ByteArray, created: String): String {
        require(user.length in 1..256 && password.length in 1..512 &&
            (user + password).none { it.isISOControl() }) { "Credenciais ONVIF inválidas." }
        val digest = MessageDigest.getInstance("SHA-1").digest(nonce +
            created.toByteArray(Charsets.UTF_8) + password.toByteArray(Charsets.UTF_8))
        val encoded = Base64.getEncoder()
        return """<s:Envelope xmlns:s="$SOAP" xmlns:m="$MEDIA" xmlns:d="$DEVICE" xmlns:t="$SCHEMA" xmlns:wsse="$WSSE" xmlns:wsu="$WSU">
            <s:Header><wsse:Security s:mustUnderstand="true"><wsse:UsernameToken>
            <wsse:Username>${escape(user)}</wsse:Username>
            <wsse:Password Type="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordDigest">${encoded.encodeToString(digest)}</wsse:Password>
            <wsse:Nonce EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary">${encoded.encodeToString(nonce)}</wsse:Nonce>
            <wsu:Created>${escape(created)}</wsu:Created>
            </wsse:UsernameToken></wsse:Security></s:Header><s:Body>$body</s:Body></s:Envelope>"""
    }

    private fun document(xml: ByteArray): Element {
        require(xml.size in 1..MAX) { "Resposta ONVIF inválida." }
        val text = Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(xml)).toString()
        require(!text.contains("<!DOCTYPE", true) && !text.contains("<!ENTITY", true) &&
            !text.contains('\u0000')) { "XML ONVIF inválido." }
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            isExpandEntityReferences = false
        }
        val builder = factory.newDocumentBuilder()
        builder.setEntityResolver { _, _ -> throw org.xml.sax.SAXException("External entities disabled") }
        val root = builder.parse(ByteArrayInputStream(text.toByteArray(Charsets.UTF_8))).documentElement
        require(root.namespaceURI == SOAP && root.localName == "Envelope" &&
            root.getElementsByTagNameNS(SOAP, "Fault").length == 0) { "O serviço ONVIF recusou a operação." }
        return root
    }

    internal fun profiles(xml: ByteArray): List<Profile> {
        val nodes = document(xml).getElementsByTagNameNS(MEDIA, "Profiles")
        require(nodes.length in 1..128) { "Nenhum perfil de vídeo ONVIF disponível." }
        return (0 until nodes.length).map {
            val node = nodes.item(it) as Element
            val token = node.getAttribute("token")
            require(token.length in 1..256 && token.none { c -> c.isISOControl() }) { "Perfil ONVIF inválido." }
            val name = node.getElementsByTagNameNS(SCHEMA, "Name").item(0)?.textContent.orEmpty()
            Profile(token, name.take(100))
        }
    }

    internal fun stream(xml: ByteArray, endpoint: URI, user: String, password: String): LanRtspLink {
        val nodes = document(xml).getElementsByTagNameNS(SCHEMA, "Uri")
        require(nodes.length == 1) { "Endereço de vídeo ONVIF indisponível." }
        val raw = URI(LanRtspLink.parse(nodes.item(0).textContent).value)
        require(raw.host == endpoint.host && raw.rawUserInfo == null) {
            "O serviço retornou endereço de outro dispositivo ou credenciais inesperadas."
        }
        require(':' !in user) { "Usuário RTSP inválido." }
        val info = URI("rtsp", "$user:$password", raw.host, raw.port, "/", null, null).rawUserInfo
        return LanRtspLink.parse("rtsp://$info@" + raw.rawAuthority + raw.rawPath +
            (raw.rawQuery?.let { "?$it" } ?: ""))
    }

    private fun request(endpoint: URI, operation: String, body: String, user: String, password: String,
                        namespace: String = MEDIA): ByteArray {
        val nonce = ByteArray(20).also { SecureRandom().nextBytes(it) }
        val payload = envelope(body, user, password, nonce, Instant.now().toString()).toByteArray(Charsets.UTF_8)
        if (endpoint.scheme == "http") return OnvifLocalHttp.post(endpoint, payload, namespace + "/" + operation)
        val connection = endpoint.toURL().openConnection(Proxy.NO_PROXY) as HttpsURLConnection
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/soap+xml; charset=utf-8; action=\"$namespace/$operation\"")
            connection.setFixedLengthStreamingMode(payload.size)
            connection.outputStream.use { it.write(payload) }
            require(connection.responseCode == 200) { "O serviço ONVIF recusou a operação." }
            return connection.inputStream.use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(out.size() + count <= MAX) { "Resposta ONVIF grande demais." }
                    out.write(buffer, 0, count)
                }
                out.toByteArray()
            }
        } finally { connection.disconnect() }
    }

    /** WS-Discovery normally returns a Device service, not the Media service.
     * Resolve the advertised Media XAddr on that exact device before GetProfiles.
     */
    internal fun mediaEndpoint(xml: ByteArray, device: URI): URI {
        val medias = document(xml).getElementsByTagNameNS(SCHEMA, "Media")
        require(medias.length in 1..4) { "Dispositivo não anunciou um serviço Media ONVIF." }
        for (i in 0 until medias.length) {
            val node = medias.item(i) as Element
            val addresses = node.getElementsByTagNameNS(SCHEMA, "XAddr")
            for (j in 0 until addresses.length) {
                val value = addresses.item(j).textContent.trim()
                val uri = runCatching { endpoint(value) }.getOrNull() ?: continue
                if (uri.host == device.host) return uri
            }
        }
        throw IllegalArgumentException("Dispositivo anunciou serviço Media em outro endereço.")
    }

    fun mediaAddress(deviceAddress: String, user: String, password: String): String {
        val device = endpoint(deviceAddress)
        val body = "<d:GetCapabilities><d:Category>Media</d:Category></d:GetCapabilities>"
        return mediaEndpoint(request(device, "GetCapabilities", body, user, password, DEVICE),
            device).toString()
    }

    fun listProfiles(address: String, user: String, password: String): List<Profile> =
        profiles(request(endpoint(address), "GetProfiles", "<m:GetProfiles/>", user, password))

    fun resolve(address: String, user: String, password: String, profile: Profile): LanRtspLink {
        val target = endpoint(address)
        val body = """<m:GetStreamUri><m:StreamSetup><t:Stream>RTP-Unicast</t:Stream>
            <t:Transport><t:Protocol>RTSP</t:Protocol></t:Transport></m:StreamSetup>
            <m:ProfileToken>${escape(profile.token)}</m:ProfileToken></m:GetStreamUri>"""
        return stream(request(target, "GetStreamUri", body, user, password), target, user, password)
    }
}
