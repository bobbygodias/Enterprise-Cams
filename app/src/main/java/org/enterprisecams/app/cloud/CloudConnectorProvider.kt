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
    /** A broken or duplicated SDK module must not hide the other camera brand. */
    internal fun select(factories: Iterable<() -> VendorAccountConnector>): List<VendorAccountConnector> {
        val active = linkedMapOf<CloudVendor, VendorAccountConnector>()
        factories.forEach { create ->
            val driver = try { create() }
            catch (_: java.util.ServiceConfigurationError) { null }
            catch (_: LinkageError) { null }
            catch (_: Exception) { null }
            if (driver != null && driver.vendor !in active) active[driver.vendor] = driver
        }
        return active.values.toList()
    }

    fun load(context: Context): List<VendorAccountConnector> {
        val iterator = try {
            ServiceLoader.load(CloudConnectorProvider::class.java, CloudConnectorProvider::class.java.classLoader).iterator()
        } catch (_: Exception) { return emptyList() }
        catch (_: java.util.ServiceConfigurationError) { return emptyList() }
        catch (_: LinkageError) { return emptyList() }

        val factories = mutableListOf<() -> VendorAccountConnector>()
        var consecutiveFailures = 0
        // ServiceLoader may leave a broken provider at the iterator cursor;
        // bound retries so a malformed manifest cannot block startup indefinitely.
        while (factories.size < 32 && consecutiveFailures < 8) {
            val provider = try {
                if (!iterator.hasNext()) break
                iterator.next()
            } catch (_: java.util.ServiceConfigurationError) {
                consecutiveFailures++
                continue
            } catch (_: LinkageError) {
                consecutiveFailures++
                continue
            } catch (_: Exception) {
                consecutiveFailures++
                continue
            }
            consecutiveFailures = 0
            factories.add { provider.create(context.applicationContext) }
        }
        return select(factories)
    }
}
