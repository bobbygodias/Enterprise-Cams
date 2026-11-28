package org.enterprisecams.app

import kotlinx.coroutines.runBlocking
import org.enterprisecams.app.cloud.*
import org.enterprisecams.app.playback.HttpsVideoLink
import org.junit.Assert.*
import org.junit.Test

class VendorAccountControllerTest {
    private class Driver(override val vendor: CloudVendor) : VendorAccountConnector {
        var loginCount = 0
        var logoutCount = 0
        var failInventory = false
        var camerasReturned = listOf("1")
        var streamRequests = 0
        var enableStream = true
        var failStream = false
        override suspend fun signIn(email: String, password: String): VendorSession {
            loginCount++
            return VendorSession(vendor, email, "opaque-handle")
        }
        override suspend fun cameras(session: VendorSession): List<RemoteCamera> {
            require(session.vendor == vendor)
            if (failInventory) error("test failure")
            return camerasReturned.flatMap { listOf(RemoteCamera(it, vendor.label), RemoteCamera(it, vendor.label)) }
        }
        override suspend fun signOut(session: VendorSession) { logoutCount++ }
        override suspend fun liveSource(session: VendorSession, cameraId: String): VendorVideoSource? {
            streamRequests++
            require(session.vendor == vendor)
            if (failStream) error("synthetic transport failure")
            if (!enableStream) return null
            return VendorVideoSource.Https(
                HttpsVideoLink.parse("https://camera.example/live.m3u8?token=signed-private"),
                hls = true
            )
        }
    }

    @Test fun unavailableNeverCollectsOrUsesCredentials() = runBlocking {
        val controller = AccountController(emptyList())
        assertFalse(controller.available(CloudVendor.YOOSEE))
        assertEquals(AccountResult.Unavailable, controller.signIn(CloudVendor.YOOSEE, "a@example.org", "password"))
    }

    @Test fun sessionsAreSeparateAndInventoryDeduplicated() = runBlocking {
        val y = Driver(CloudVendor.YOOSEE)
        val h = Driver(CloudVendor.HILEVEL)
        val controller = AccountController(listOf(y,h))
        assertEquals(AccountResult.Connected(listOf(RemoteCamera("1", "Yoosee"))),
            controller.signIn(y.vendor, "a@example.org", "pw"))
        assertEquals(AccountResult.Connected(listOf(RemoteCamera("1", "Hilevel"))),
            controller.signIn(h.vendor, "b@example.org", "pw"))
        controller.signOut(y.vendor)
        assertEquals(AccountResult.SignedOut, controller.refresh(y.vendor))
        assertTrue(controller.refresh(h.vendor) is AccountResult.Connected)
        assertEquals(1,y.logoutCount)
        assertEquals(0,h.logoutCount)
    }

    @Test fun inventoryFailureClosesSessionAndRejectsBadInput() = runBlocking {
        val y = Driver(CloudVendor.YOOSEE)
        val controller = AccountController(listOf(y))
        assertEquals(AccountResult.InvalidCredentials, controller.signIn(y.vendor, "bad account", "pw"))
        assertEquals(0,y.loginCount)
        y.failInventory = true
        assertEquals(AccountResult.Failed, controller.signIn(y.vendor, "a@example.org", "pw"))
        assertEquals(1,y.logoutCount)
        assertEquals(AccountResult.SignedOut, controller.refresh(y.vendor))
    }

    @Test fun failedRefreshRemovesAuthorization() = runBlocking {
        val y = Driver(CloudVendor.YOOSEE)
        val controller = AccountController(listOf(y))
        assertTrue(controller.signIn(y.vendor, "a@example.org", "pw") is AccountResult.Connected)
        y.failInventory = true
        assertEquals(AccountResult.Failed, controller.refresh(y.vendor))
        assertEquals(AccountResult.SignedOut, controller.refresh(y.vendor))
        assertEquals(1,y.logoutCount)
    }

    @Test fun defectiveSdkModuleDoesNotDisableIndependentVendor() {
        val y = Driver(CloudVendor.YOOSEE)
        val h = Driver(CloudVendor.HILEVEL)
        val modules = CloudConnectors.select(listOf(
            { error("synthetic broken provider") },
            { y },
            { Driver(CloudVendor.YOOSEE) },
            { h }
        ))
        assertEquals(listOf(CloudVendor.YOOSEE, CloudVendor.HILEVEL), modules.map { it.vendor })
        val onlyH = CloudConnectors.select(listOf({ error("Yoosee unavailable") }, { h }))
        assertEquals(listOf(CloudVendor.HILEVEL), onlyH.map { it.vendor })
    }

    @Test fun vendorPortsDoNotAssumeOnePrivateApiVersionOrEmailOnlyLogin() = runBlocking {
        val y = object : YooseeAccountApi {
            override suspend fun login(accountIdentifier: String, password: String): VendorSession {
                assertEquals("123456789", accountIdentifier)
                return VendorSession(CloudVendor.YOOSEE, accountIdentifier, "yoosee-test")
            }
            override suspend fun listDevices(session: VendorSession): List<RemoteCamera> =
                listOf(RemoteCamera("y-1", "Yoosee Camera"))
            override suspend fun close(session: VendorSession) = Unit
        }
        val h = object : HilevelAccountApi {
            override suspend fun login(email: String, password: String): VendorSession {
                assertEquals("55-62912345678", email)
                return VendorSession(CloudVendor.HILEVEL, email, "hilevel-test")
            }
            override suspend fun listDevices(session: VendorSession): List<RemoteCamera> =
                listOf(RemoteCamera("h-1", "Hilevel Camera"))
            override suspend fun close(session: VendorSession) = Unit
        }
        val controller = AccountController(listOf(YooseeConnector(y), HilevelConnector(h)))
        assertEquals("y-1", ((controller.signIn(CloudVendor.YOOSEE, "123456789", "secret")
            as AccountResult.Connected).cameras.single().id))
        assertEquals("h-1", ((controller.signIn(CloudVendor.HILEVEL, "55-62912345678", "secret")
            as AccountResult.Connected).cameras.single().id))
        assertEquals(CameraSourceResult.NotSupported, controller.openCamera(CloudVendor.HILEVEL, "h-1"))
    }

    @Test fun registeredPhoneOrDeviceIdIsNotBlockedBeforeManufacturerValidation() = runBlocking {
        val driver = Driver(CloudVendor.YOOSEE)
        val controller = AccountController(listOf(driver))
        assertTrue(controller.signIn(driver.vendor, "55-62912345678", "password") is AccountResult.Connected)
        assertTrue(controller.signIn(driver.vendor, "123456789", "password") is AccountResult.Connected)
        assertEquals(2, driver.loginCount)
        assertEquals(AccountResult.InvalidCredentials,
            controller.signIn(driver.vendor, "invalid identifier", "password"))
    }

    @Test fun inventoryMustAuthorizeCameraBeforeVideoRequest() = runBlocking {
        val driver = Driver(CloudVendor.YOOSEE)
        val controller = AccountController(listOf(driver))
        assertEquals(CameraSourceResult.SignedOut, controller.openCamera(driver.vendor, "1"))
        assertEquals(0, driver.streamRequests)
        assertTrue(controller.signIn(driver.vendor, "owner@example.org", "secret") is AccountResult.Connected)
        assertEquals(CameraSourceResult.NotInAccount, controller.openCamera(driver.vendor, "other"))
        assertEquals(0, driver.streamRequests)
        val ready = controller.openCamera(driver.vendor, "1") as CameraSourceResult.Ready
        assertTrue(ready.source is VendorVideoSource.Https)
        assertEquals(1, driver.streamRequests)
        assertFalse(ready.source.toString().contains("signed-private"))
        controller.signOut(driver.vendor)
        assertEquals(CameraSourceResult.SignedOut, controller.openCamera(driver.vendor, "1"))
        assertEquals(1, driver.streamRequests)
    }

    @Test fun refreshedInventoryRevokesOldCameraIds() = runBlocking {
        val driver = Driver(CloudVendor.HILEVEL)
        val controller = AccountController(listOf(driver))
        assertTrue(controller.signIn(driver.vendor, "owner@example.org", "secret") is AccountResult.Connected)
        driver.camerasReturned = listOf("2")
        assertTrue(controller.refresh(driver.vendor) is AccountResult.Connected)
        assertEquals(CameraSourceResult.NotInAccount, controller.openCamera(driver.vendor, "1"))
        assertTrue(controller.openCamera(driver.vendor, "2") is CameraSourceResult.Ready)
        assertEquals(1, driver.streamRequests)
    }

    @Test fun unsupportedAndFailedStreamsAreNotPretendedPlayable() = runBlocking {
        val driver = Driver(CloudVendor.YOOSEE)
        val controller = AccountController(listOf(driver))
        assertTrue(controller.signIn(driver.vendor, "owner@example.org", "secret") is AccountResult.Connected)
        driver.enableStream = false
        assertEquals(CameraSourceResult.NotSupported, controller.openCamera(driver.vendor, "1"))
        driver.enableStream = true
        driver.failStream = true
        assertEquals(CameraSourceResult.Failed, controller.openCamera(driver.vendor, "1"))
        assertEquals(CameraSourceResult.Unavailable,
            AccountController(emptyList()).openCamera(driver.vendor, "1"))
    }

    @Test fun sessionRepresentationIsRedacted() {
        val session = VendorSession(CloudVendor.YOOSEE, "private-account", "private-token")
        assertFalse(session.toString().contains("private"))
    }
}
