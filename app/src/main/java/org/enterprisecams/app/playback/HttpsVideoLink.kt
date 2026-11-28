package org.enterprisecams.app.playback

import java.net.URI

/** Ephemeral playback input. Never serialize this class or include it in backups/logs. */
class HttpsVideoLink private constructor(val value: String) {
    override fun toString(): String = "HttpsVideoLink(<redacted>)"

    companion object {
        const val MAX_LENGTH = 8192

        fun parse(input: String): HttpsVideoLink {
            val value = input.trim()
            require(value.length in 1..MAX_LENGTH && value.none { it.isISOControl() }) {
                "Informe um link HTTPS de vídeo válido."
            }
            val uri = try { URI(value) } catch (_: Exception) {
                throw IllegalArgumentException("Informe um link HTTPS de vídeo válido.")
            }
            require(uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank() &&
                uri.rawUserInfo == null && uri.rawFragment == null &&
                (uri.port == -1 || uri.port in 1..65535)) {
                "Use HTTPS, sem usuário ou senha no endereço."
            }
            return HttpsVideoLink(value)
        }
    }
}
