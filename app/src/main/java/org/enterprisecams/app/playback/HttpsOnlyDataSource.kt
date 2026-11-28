package org.enterprisecams.app.playback

import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import java.io.IOException

/** Checks every HLS manifest, segment and key request; never permits a local-file source. */
@UnstableApi
class HttpsOnlyDataSource(private val delegate: DataSource) : DataSource by delegate {
    override fun open(dataSpec: DataSpec): Long {
        try { HttpsVideoLink.parse(dataSpec.uri.toString()) }
        catch (_: IllegalArgumentException) { throw IOException("A reprodução exige HTTPS.") }
        return delegate.open(dataSpec)
    }

    companion object {
        fun factory(): DataSource.Factory {
            val http = DefaultHttpDataSource.Factory()
                .setUserAgent("EnterpriseCams/0.2")
                .setConnectTimeoutMs(12_000)
                .setReadTimeoutMs(12_000)
                .setAllowCrossProtocolRedirects(false)
            return DataSource.Factory { HttpsOnlyDataSource(http.createDataSource()) }
        }
    }
}
