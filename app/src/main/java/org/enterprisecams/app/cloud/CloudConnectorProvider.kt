package org.enterprisecams.app.cloud

import android.content.Context
import java.util.ServiceLoader

/** An explicitly packaged, authorized vendor SDK module supplies this implementation.
 * Never load classes or code from other installed APKs or remote URLs.
 */
interface CloudConnectorProvider {
    fun create(context: Context): VendorAccountConnector
}

object CloudConnectors {
    fun load(context: Context): List<VendorAccountConnector> =
        try {
            ServiceLoader.load(CloudConnectorProvider::class.java, CloudConnectorProvider::class.java.classLoader)
                .map { it.create(context.applicationContext) }
                .also { entries -> require(entries.map { it.vendor }.distinct().size == entries.size) }
        } catch (_: Exception) {
            emptyList()
        } catch (_: java.util.ServiceConfigurationError) {
            emptyList()
        } catch (_: LinkageError) {
            emptyList()
        }
}
