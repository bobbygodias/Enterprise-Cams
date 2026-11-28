package org.enterprisecams.app

import kotlinx.coroutines.runBlocking
import org.enterprisecams.app.cloud.*
import org.junit.Assert.*
import org.junit.Test

class VendorAccountControllerTest {
    private class Driver(override val vendor: CloudVendor) : VendorAccountConnector {
        var loginCount = 0
        var logoutCount = 0
        var failInventory = false
        override suspend fun signIn(email: String, password: String): VendorSession {
            loginCount++
            return VendorSession(vendor, email, "opaque-handle")
        }
        override suspend fun cameras(session: VendorSession): List<RemoteCamera> {
            require(session.vendor == vendor)
            if (failInventory) error("test failure")
            return listOf(RemoteCamera("1", vendor.label), RemoteCamera("1", vendor.label))
        }
        override suspend fun signOut(session: VendorSession) { logoutCount++ }
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
        assertEquals(AccountResult.InvalidCredentials, controller.signIn(y.vendor, "bad", "pw"))
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

    @Test fun sessionRepresentationIsRedacted() {
        val session = VendorSession(CloudVendor.YOOSEE, "private-account", "private-token")
        assertFalse(session.toString().contains("private"))
    }
}
