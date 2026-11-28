package org.enterprisecams.app.playback

import java.net.URI

/** Explicit, temporary RTSP input for a camera on the user's private IPv4 LAN.
 * RTSP is not encrypted; callers must warn the user and must not log this URL.
 * No hostname resolution, automatic scanning, default credentials or persistence.
 */
class LanRtspLink private constructor(val value: String) {
    override fun toString(): String = "LanRtspLink(<redacted>)"

    companion object {
        const val MAX_LENGTH = 8192
        /** The owner supplies the camera stream address and its own device credentials.
         * No account password is sent to a guessed host or stored in the camera record.
         */
        fun authenticated(address: String, username: String, password: String): LanRtspLink {
            val original = URI(parse(address).value)
            require(original.rawUserInfo == null) { "Informe o endereço sem usuário ou senha." }
            require(username.length in 1..256 && ':' !in username &&
                password.length in 1..512 && (username + password).none { it.isISOControl() }) {
                "Informe o usuário e a senha da câmera."
            }
            val userInfo = URI("rtsp", "$username:$password", original.host, original.port,
                "/", null, null).rawUserInfo
            return parse("rtsp://$userInfo@" + original.rawAuthority + original.rawPath +
                (original.rawQuery?.let { "?$it" } ?: ""))
        }

        fun parse(input: String): LanRtspLink {
            val value = input.trim()
            require(value.length in 1..MAX_LENGTH && value.none { it.isISOControl() }) {
                "Informe um endereço RTSP válido."
            }
            val uri = try { URI(value) } catch (_: Exception) {
                throw IllegalArgumentException("Informe um endereço RTSP válido.")
            }
            val host = uri.host.orEmpty()
            val octets = host.split(".")
            val ip = octets.mapNotNull { part ->
                part.toIntOrNull()?.takeIf { it in 0..255 && it.toString() == part }
            }
            val privateLan = octets.size == 4 && ip.size == 4 &&
                (ip[0] == 10 || (ip[0] == 172 && ip[1] in 16..31) ||
                    (ip[0] == 192 && ip[1] == 168))
            require(uri.scheme == "rtsp" && privateLan && uri.rawFragment == null &&
                (uri.port == -1 || uri.port in 1..65535) &&
                !uri.rawPath.isNullOrBlank() && uri.rawPath.startsWith("/")) {
                "Use RTSP com IP privado da câmera e caminho de vídeo."
            }
            return LanRtspLink(value)
        }
    }
}
