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
            org.enterprisecams.app.data.HubCodec.validate(state.copy(draft = requireNotNull(state.draft).copy(setupStarted = true)))
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
    @Test fun arbitraryPlayStorePackageProducesUnverifiedCandidate() {
        val pkg = "org.example.camera"
        for (qr in listOf(
            "https://play.google.com/store/apps/details?id=$pkg&hl=pt_BR",
            "market://details?id=$pkg",
        )) {
            val candidate = requireNotNull(ProviderIdentifier.identify(qr))
            assertEquals("package:$pkg", candidate.id)
            assertEquals(pkg, candidate.packageName)
            assertFalse(Providers.isCatalogued(candidate))
            assertEquals(candidate, Providers.find(candidate.id))
        }
        assertEquals(Providers.find("yoosee"), ProviderIdentifier.identify("https://play.google.com/store/apps/details?id=com.yoosee"))
    }

    @Test fun candidateRequiresExactStoreLinkAndSafePackage() {
        listOf(
            "org.example.camera",
            "https://evil.example/store/apps/details?id=org.example.camera",
            "https://play.google.com/store/apps/details?id=org.example.camera&id=org.example.other",
            "https://play.google.com/store/apps/details?id=org.example.camera%2Fbad",
            "https://play.google.com/store/apps/details?id=org..camera",
            "https://play.google.com/store/apps/details?id=org.example.camera%40evil",
            "market://details?id=org.example.camera%0A",
            "https://play.google.com/store/apps/details?id=org.example.camera#fragment",
        ).forEach { assertNull("Unexpected candidate: $it", ProviderIdentifier.identify(it)) }
        assertNull(Providers.find("package:org..camera"))
        assertNull(Providers.find("package:com.yoosee"))
    }

    @Test fun unknownSerialOrCameraMarketingNameDoesNotGuessAnApp() {
        listOf("123456789", "TowerCam", "WiFi Smart Camera", "ICSee compatible maybe", "WIFI:T:WPA;S:camera;P:secret;;").forEach {
            assertNull(ProviderIdentifier.identify(it))
        }
    }
    @Test fun lookalikeDomainsAndArbitraryUrlsCannotChooseAStoreTarget() {
        listOf("https://play.google.com.evil.example/store/apps/details?id=com.yoosee",
            "https://evil.example/yoosee", "https://play.google.com@evil.example/store/apps/details?id=com.yoosee",
            "intent://camera#Intent;package=com.yoosee;end", "https://play.google.com/store/apps/details?id=com.unknown.app%2Fbad").forEach {
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
