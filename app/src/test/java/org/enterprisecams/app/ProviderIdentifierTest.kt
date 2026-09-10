package org.enterprisecams.app

import org.enterprisecams.app.data.ProviderIdentifier
import org.enterprisecams.app.data.Providers
import org.junit.Assert.*
import org.junit.Test

class ProviderIdentifierTest {
    @Test fun newDraftRequiresAnIdentifiedOrExplicitlySelectedProvider() {
        val state = org.enterprisecams.app.data.HubState(draft = org.enterprisecams.app.data.CameraDraft(name = "Portão"))
        assertEquals("", state.draft!!.providerId)
        org.enterprisecams.app.data.HubCodec.validate(state)
        try {
            org.enterprisecams.app.data.HubCodec.validate(state.copy(draft = state.draft.copy(setupStarted = true)))
            fail("A setup without a provider must not continue")
        } catch (_: IllegalArgumentException) { }
    }
    @Test fun officialStoreLinksSelectEachExactPackage() {
        Providers.all.forEach { p ->
            assertEquals(p, ProviderIdentifier.identify(p.storeUrl))
            assertEquals(p, ProviderIdentifier.identify("market://details?id=${p.packageName}"))
            assertEquals(p, ProviderIdentifier.identify(p.storeUrl + "&hl=pt_BR&referrer=manual"))
        }
    }
    @Test fun exactNamesAndPackagesAreRecognizedWithoutMixingVariants() {
        assertEquals("v380pro", ProviderIdentifier.identify("  v380   PRO  ")?.id)
        assertEquals("v380", ProviderIdentifier.identify("V380")?.id)
        assertEquals("icsee", ProviderIdentifier.identify("com.xm.csee")?.id)
        assertEquals("yoosee", ProviderIdentifier.identify("YOOSEE")?.id)
    }
    @Test fun unknownSerialOrCameraMarketingNameDoesNotGuessAnApp() {
        listOf("123456789", "TowerCam", "WiFi Smart Camera", "ICSee compatible maybe", "WIFI:T:WPA;S:camera;P:secret;;").forEach {
            assertNull(ProviderIdentifier.identify(it))
        }
    }
    @Test fun lookalikeDomainsAndArbitraryUrlsCannotChooseAStoreTarget() {
        listOf("https://play.google.com.evil.example/store/apps/details?id=com.yoosee",
            "https://evil.example/yoosee", "https://play.google.com@evil.example/store/apps/details?id=com.yoosee",
            "intent://camera#Intent;package=com.yoosee;end", "https://play.google.com/store/apps/details?id=com.unknown.app").forEach {
            assertNull(ProviderIdentifier.identify(it))
        }
    }
    @Test fun ambiguousOrMalformedPayloadsAreRejected() {
        listOf("https://play.google.com/store/apps/details?id=com.yoosee&id=com.xm.csee",
            "market://details?id=com.yoosee%ZZ", "yoosee\n", "x".repeat(2049)).forEach {
            assertNull(ProviderIdentifier.identify(it))
        }
    }
    @Test fun inspectedSchemesOnlyIdentifyProviderWithoutClaimingPlayback() {
        assertEquals("yoosee", ProviderIdentifier.identify("yoosee://share?page=web&webPath=example")?.id)
        assertEquals("hilevel", ProviderIdentifier.identify("hilevelen://camera")?.id)
        assertNull(ProviderIdentifier.identify("icsee://unknown-camera"))
    }
}
