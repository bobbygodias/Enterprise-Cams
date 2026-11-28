package org.enterprisecams.app.cloud

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Locale
import javax.net.ssl.HttpsURLConnection

/** Restricts manufacturer-authorized cloud calls to their configured HTTPS hosts.
 * The provider is responsible for its own app registration, signature and response schema.
 * Never post a user's password to a URL discovered in an APK without that contract.
 */
internal class OfficialCloudHttp(
    private val allowedHosts: Set<String>,
    private val connect: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection },
) {
    init {
        require(allowedHosts.isNotEmpty())
        require(allowedHosts.all(::validHostname))
    }

    /** Bodies and headers may contain credentials: no generated toString(). */
    internal class Request(
        val host: String,
        val path: String,
        val body: ByteArray,
        val headers: Map<String, String> = emptyMap(),
    ) {
        override fun toString(): String = "OfficialCloudHttp.Request(<redacted>)"
    }

    internal class Response(val status: Int, val bytes: ByteArray) {
        override fun toString(): String = "OfficialCloudHttp.Response(status=$status, <redacted>)"
        fun utf8(): String = bytes.toString(StandardCharsets.UTF_8)
    }

    suspend fun post(request: Request): Response = withContext(Dispatchers.IO) {
        val hostname = request.host.lowercase(Locale.ROOT)
        require(hostname in allowedHosts && validHostname(hostname)) { "Cloud host not configured" }
        require(request.path.startsWith("/") && !request.path.startsWith("//") &&
            '\\' !in request.path && '#' !in request.path && '?' !in request.path &&
            request.path.split('/').none { it == ".." || it == "." }) { "Invalid cloud request path" }
        require(request.body.size <= MAX_REQUEST_BYTES) { "Cloud request body too large" }
        require(request.headers.size <= 24 && request.headers.all { (key, value) ->
            key.matches(HEADER_NAME) && value.length <= 4096 &&
                value.none { it == '\r' || it == '\n' || it == '\u0000' } &&
                key.lowercase(Locale.ROOT) !in RESERVED_HEADERS
        }) { "Invalid cloud headers" }

        val connection = try { connect(URL("https", hostname, request.path)) }
        catch (_: IOException) { throw CloudHttpFailure.TransportFailed }
        try {
            require(connection is HttpsURLConnection) { "Cloud transport must use HTTPS" }
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.useCaches = false
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            request.headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
            connection.outputStream.use { it.write(request.body) }
            val status = connection.responseCode
            if (status in 300..399) throw CloudHttpFailure.RedirectRefused
            val stream = if (status < 400) connection.inputStream else connection.errorStream
            val data = stream?.use { source ->
                val out = java.io.ByteArrayOutputStream()
                val part = ByteArray(8192)
                while (true) {
                    val count = source.read(part)
                    if (count < 0) break
                    if (out.size() + count > MAX_RESPONSE_BYTES) throw CloudHttpFailure.ResponseTooLarge
                    out.write(part, 0, count)
                }
                out.toByteArray()
            } ?: ByteArray(0)
            Response(status, data)
        } catch (e: CloudHttpFailure) {
            throw e
        } catch (_: IOException) {
            // Errors intentionally cannot echo URL, password, token or cloud response body.
            throw CloudHttpFailure.TransportFailed
        } finally {
            connection.disconnect()
        }
    }

    internal sealed class CloudHttpFailure(message: String) : IOException(message) {
        data object RedirectRefused : CloudHttpFailure("Cloud redirected request")
        data object ResponseTooLarge : CloudHttpFailure("Cloud response too large")
        data object TransportFailed : CloudHttpFailure("Cloud connection failed")
    }

    companion object {
        private const val MAX_RESPONSE_BYTES = 1_048_576
        private const val MAX_REQUEST_BYTES = 262_144
        private val HEADER_NAME = Regex("[A-Za-z0-9-]{1,80}")
        private val RESERVED_HEADERS = setOf("host", "content-length", "transfer-encoding", "connection")
        private fun validHostname(host: String): Boolean =
            host.length in 4..253 && host == host.lowercase(Locale.ROOT) &&
                !host.endsWith('.') && !host.contains(':') &&
                host.any { it in 'a'..'z' } && host.split('.').size >= 2 && host.split('.').all { label ->
                    label.length in 1..63 && label.first().isLetterOrDigit() &&
                        label.last().isLetterOrDigit() &&
                        label.all { it in 'a'..'z' || it in '0'..'9' || it == '-' }
                }
    }
}
