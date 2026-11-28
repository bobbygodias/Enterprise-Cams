package org.enterprisecams.app.cloud

import org.enterprisecams.app.playback.HttpsVideoLink
import org.enterprisecams.app.playback.LanRtspLink

/** The current delivery targets exactly these two ecosystems. */
enum class CloudVendor(val id: String, val label: String) {
    YOOSEE("yoosee", "Yoosee"), HILEVEL("hilevel", "Hilevel");
    companion object {
        fun fromId(id: String): CloudVendor? = values().firstOrNull { it.id == id }
    }
}

/** Session identifiers belong to an account and must never enter a backup or log. */
class VendorSession(val vendor: CloudVendor, val accountId: String, val handle: String) {
    init { require(accountId.isNotBlank() && handle.isNotBlank()) }
    override fun toString() = "VendorSession(redacted)"
}

data class RemoteCamera(val id: String, val name: String) {
    init { require(id.isNotBlank() && id.length <= 256 && name.length <= 256) }
}

/** A short-lived source issued for a camera that belongs to the authenticated account.
 * Links stay in memory; the viewer must not save, print or back up signed addresses.
 * These are playable standards-based sources; vendor-specific P2P requires its own driver.
 */
sealed interface VendorVideoSource {
    class Https(val link: HttpsVideoLink, val hls: Boolean) : VendorVideoSource {
        override fun toString() = "VendorVideoSource.Https(<redacted>)"
    }
    class Rtsp(val link: LanRtspLink) : VendorVideoSource {
        override fun toString() = "VendorVideoSource.Rtsp(<redacted>)"
    }
}

/** Implementations must use an approved vendor client registration/SDK.
 * Do not use APK-embedded private keys, copy app identity or intercept credentials.
 * Handles are opaque and scoped to this adapter instance.
 */
interface VendorAccountConnector {
    val vendor: CloudVendor
    suspend fun signIn(email: String, password: String): VendorSession
    suspend fun cameras(session: VendorSession): List<RemoteCamera>
    suspend fun signOut(session: VendorSession)
    /** Return null when this authorized driver cannot provide a playable source. */
    suspend fun liveSource(session: VendorSession, cameraId: String): VendorVideoSource? = null
}

/** SDK-specific ports reflect the account and list operations recovered from the apps.
 * Each implementation owns request signing, discovery, MFA, expiry and pagination.
 */
interface YooseeAccountApi {
    suspend fun emailLogin(email: String, password: String): VendorSession
    suspend fun deviceList(session: VendorSession): List<RemoteCamera>
    suspend fun close(session: VendorSession)
    suspend fun liveSource(session: VendorSession, cameraId: String): VendorVideoSource? = null
}
interface HilevelAccountApi {
    suspend fun login(email: String, password: String): VendorSession
    suspend fun getDeviceListV2(session: VendorSession): List<RemoteCamera>
    suspend fun close(session: VendorSession)
    suspend fun liveSource(session: VendorSession, cameraId: String): VendorVideoSource? = null
}

class YooseeConnector(private val api: YooseeAccountApi) : VendorAccountConnector {
    override val vendor = CloudVendor.YOOSEE
    override suspend fun signIn(email: String, password: String) =
        api.emailLogin(email, password).also { require(it.vendor == vendor) }
    override suspend fun cameras(session: VendorSession): List<RemoteCamera> {
        require(session.vendor == vendor)
        return api.deviceList(session)
    }
    override suspend fun signOut(session: VendorSession) {
        require(session.vendor == vendor)
        api.close(session)
    }
    override suspend fun liveSource(session: VendorSession, cameraId: String): VendorVideoSource? {
        require(session.vendor == vendor && cameraId.isNotBlank())
        return api.liveSource(session, cameraId)
    }
}

class HilevelConnector(private val api: HilevelAccountApi) : VendorAccountConnector {
    override val vendor = CloudVendor.HILEVEL
    override suspend fun signIn(email: String, password: String) =
        api.login(email, password).also { require(it.vendor == vendor) }
    override suspend fun cameras(session: VendorSession): List<RemoteCamera> {
        require(session.vendor == vendor)
        return api.getDeviceListV2(session)
    }
    override suspend fun signOut(session: VendorSession) {
        require(session.vendor == vendor)
        api.close(session)
    }
    override suspend fun liveSource(session: VendorSession, cameraId: String): VendorVideoSource? {
        require(session.vendor == vendor && cameraId.isNotBlank())
        return api.liveSource(session, cameraId)
    }
}
