package org.enterprisecams.app.data

import java.net.URI
import java.net.URLDecoder
import java.util.Locale

/** Offline recognition: never follows a QR URL or infers a vendor from a serial number. */
object ProviderIdentifier {
    const val MAX_INPUT = 2048

    fun identify(raw: String): CameraProvider? {
        if (raw.length > MAX_INPUT || raw.any(Char::isISOControl)) return null
        val value = raw.trim()
        val normalized = value.lowercase(Locale.ROOT).replace(Regex(" +"), " ")
        Providers.all.firstOrNull {
            normalized == it.name.lowercase(Locale.ROOT) || normalized == it.packageName ||
                (it.id == "v380pro" && normalized == "v380pro")
        }?.let { return it }
        return try {
            val uri = URI(value)
            if (uri.rawUserInfo != null || uri.port != -1 || uri.rawFragment != null) return null
            val scheme = uri.scheme?.lowercase(Locale.ROOT)
            val host = uri.host?.lowercase(Locale.ROOT)
            val isStore = (scheme in listOf("https", "http") && host == "play.google.com" && uri.path == "/store/apps/details") ||
                (scheme == "market" && host == "details" && uri.path.isNullOrEmpty())
            if (isStore) {
                val pairs = uri.rawQuery.orEmpty().split('&').map { item ->
                    item.split('=', limit = 2).let { parts ->
                        URLDecoder.decode(parts[0], "UTF-8") to URLDecoder.decode(parts.getOrElse(1) { "" }, "UTF-8")
                    }
                }
                val ids = pairs.filter { it.first == "id" }
                if (ids.size != 1) null else Providers.all.firstOrNull { it.packageName == ids.single().second }
            } else when {
                scheme == "yoosee" && host == "share" -> Providers.find("yoosee")
                scheme == "hilevelen" -> Providers.find("hilevel")
                else -> null
            }
        } catch (_: Exception) { null }
    }
}
