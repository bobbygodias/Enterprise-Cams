package org.enterprisecams.app.cloud

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed interface AccountResult {
    data object Unavailable : AccountResult
    data object InvalidCredentials : AccountResult
    data object Failed : AccountResult
    data object SignedOut : AccountResult
    data class Connected(val cameras: List<RemoteCamera>) : AccountResult
}

sealed interface CameraSourceResult {
    data object Unavailable : CameraSourceResult
    data object SignedOut : CameraSourceResult
    data object NotInAccount : CameraSourceResult
    data object NotSupported : CameraSourceResult
    data object Failed : CameraSourceResult
    data class Ready(val source: VendorVideoSource) : CameraSourceResult
}

/** Account state lives only in process memory. Never expose the token to Compose. */
class AccountController(connectors: List<VendorAccountConnector>) {
    private val drivers = connectors.associateBy { it.vendor }
    private val sessions = mutableMapOf<CloudVendor, VendorSession>()
    private val inventoryIds = mutableMapOf<CloudVendor, Set<String>>()
    private val lock = Mutex()

    init { require(drivers.size == connectors.size) }

    fun available(vendor: CloudVendor) = vendor in drivers

    private suspend fun close(driver: VendorAccountConnector, session: VendorSession) {
        withContext(NonCancellable) {
            try { driver.signOut(session) } catch (_: Exception) {
                // Local authorization is already removed, regardless of remote logout.
            }
        }
    }

    suspend fun signIn(vendor: CloudVendor, email: String, password: String): AccountResult = lock.withLock {
        val driver = drivers[vendor] ?: return@withLock AccountResult.Unavailable
        val account = email.trim()
        if (account.length !in 3..254 || account.any { it.isWhitespace() || it.isISOControl() } ||
            password.isEmpty() || password.length > 1024) return@withLock AccountResult.InvalidCredentials
        sessions.remove(vendor)?.let { close(driver, it) }
        inventoryIds.remove(vendor)
        var pending: VendorSession? = null
        try {
            val session = driver.signIn(account, password)
            pending = session
            require(session.vendor == vendor)
            val cameras = inventory(driver, session)
            sessions[vendor] = session
            inventoryIds[vendor] = cameras.map { it.id }.toSet()
            pending = null
            AccountResult.Connected(cameras)
        } catch (e: CancellationException) {
            pending?.let { close(driver, it) }
            throw e
        } catch (_: Exception) {
            pending?.let { close(driver, it) }
            AccountResult.Failed
        }
    }

    suspend fun refresh(vendor: CloudVendor): AccountResult = lock.withLock {
        val driver = drivers[vendor] ?: return@withLock AccountResult.Unavailable
        val session = sessions[vendor] ?: return@withLock AccountResult.SignedOut
        try {
            val cameras = inventory(driver, session)
            inventoryIds[vendor] = cameras.map { it.id }.toSet()
            AccountResult.Connected(cameras)
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) {
            sessions.remove(vendor)
            inventoryIds.remove(vendor)
            close(driver, session)
            AccountResult.Failed
        }
    }

    suspend fun signOut(vendor: CloudVendor): AccountResult = lock.withLock {
        val driver = drivers[vendor] ?: return@withLock AccountResult.Unavailable
        sessions.remove(vendor)?.let { close(driver, it) }
        inventoryIds.remove(vendor)
        AccountResult.SignedOut
    }

    /** Refuse to fetch or play an ID that is not in the last authenticated device list. */
    suspend fun openCamera(vendor: CloudVendor, cameraId: String): CameraSourceResult = lock.withLock {
        val driver = drivers[vendor] ?: return@withLock CameraSourceResult.Unavailable
        val session = sessions[vendor] ?: return@withLock CameraSourceResult.SignedOut
        if (cameraId !in inventoryIds[vendor].orEmpty()) return@withLock CameraSourceResult.NotInAccount
        try {
            driver.liveSource(session, cameraId)?.let { CameraSourceResult.Ready(it) }
                ?: CameraSourceResult.NotSupported
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { CameraSourceResult.Failed }
    }

    private suspend fun inventory(driver: VendorAccountConnector, session: VendorSession): List<RemoteCamera> {
        val items = driver.cameras(session)
        require(items.size <= 5000)
        return items.distinctBy { it.id }
    }
}
