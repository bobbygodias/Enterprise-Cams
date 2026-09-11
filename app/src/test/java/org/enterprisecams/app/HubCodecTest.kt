package org.enterprisecams.app

import org.enterprisecams.app.data.*
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class HubCodecTest {
    private fun entry(name: String = "Portão") = CameraEntry(name = name, location = "Casa", providerId = "yoosee")
    private fun rejects(block: () -> Unit) {
        try { block(); fail("Expected invalid input to be rejected") } catch (_: IllegalArgumentException) { }
    }

    @Test fun unicodeAndFavoritesRoundTripWithoutLoss() {
        val state = HubState(cameras = listOf(entry("Portão dos fundos 🎥").copy(favorite = true)))
        assertEquals(state, HubCodec.decode(HubCodec.encode(state)))
    }
    @Test fun setupDraftSurvivesSerialization() {
        val draft = CameraDraft("Garagem", "Casa", "hilevel", true)
        assertEquals(draft, HubCodec.decode(HubCodec.encode(HubState(draft = draft))).draft)
    }
    @Test fun rejectsFutureSchemaRatherThanLosingFields() { rejects { HubCodec.decode("{\"schemaVersion\":2}") } }
    @Test fun rejectsUnknownProviderAndInjectedPackage() {
        rejects { HubCodec.encode(HubState(cameras = listOf(entry().copy(providerId = "evil.package")))) }
        rejects { HubCodec.decode("{\"cameras\":[],\"packageName\":\"evil.package\"}") }
    }
    @Test fun rejectsBlankNamesAndControlCharacters() {
        rejects { HubCodec.encode(HubState(cameras = listOf(entry("  ")))) }
        rejects { HubCodec.encode(HubState(cameras = listOf(entry("Portão\n")))) }
    }
    @Test fun rejectsOversizedNamesAndFiles() {
        rejects { HubCodec.encode(HubState(cameras = listOf(entry("a".repeat(81))))) }
        rejects { HubCodec.readBounded(ByteArray(HubCodec.MAX_BYTES + 1).inputStream()) }
    }
    @Test fun rejectsMalformedUtf8() {
        try { HubCodec.readBounded(byteArrayOf(0xC3.toByte(), 0x28).inputStream()); fail() }
        catch (_: java.nio.charset.CharacterCodingException) { }
    }
    @Test fun rejectsDuplicateAndInvalidIds() {
        val first = entry()
        rejects { HubCodec.encode(HubState(cameras = listOf(first, first))) }
        rejects { HubCodec.encode(HubState(cameras = listOf(first.copy(id = "../../file")))) }
    }
    @Test fun importIsAdditiveAndRepeatedRestoreDoesNotDuplicate() {
        val current = HubState(cameras = listOf(entry()))
        val backup = HubState(cameras = listOf(entry("Quintal")))
        val merged = HubCodec.merge(current, backup)
        assertEquals(2, merged.cameras.size)
        assertEquals(current.cameras.first(), merged.cameras.first())
        assertEquals(merged, HubCodec.merge(merged, backup))
    }
    @Test fun sameCameraWithDifferentIdIsNotDuplicated() {
        val camera = entry()
        val current = HubState(cameras = listOf(camera))
        assertEquals(current, HubCodec.merge(current, HubState(cameras = listOf(camera.copy(id = UUID.randomUUID().toString())))))
    }
    @Test fun conflictingIdsPreserveBothDistinctCameras() {
        val camera = entry()
        val merged = HubCodec.merge(HubState(cameras = listOf(camera)), HubState(cameras = listOf(camera.copy(name = "Sala"))))
        assertEquals(2, merged.cameras.size)
        assertEquals(2, merged.cameras.map { it.id }.toSet().size)
        assertEquals(camera, merged.cameras.first())
    }
    @Test fun finishRequiresPreparedDraftAndDoesNotDuplicateCamera() {
        rejects { HubCodec.finishDraft(HubState()) }
        rejects { HubCodec.finishDraft(HubState(draft = CameraDraft("Portão"))) }
        val result = HubCodec.finishDraft(HubState(draft = CameraDraft("Portão", "Casa", "yoosee", true)))
        assertNull(result.draft)
        assertEquals("Portão", result.cameras.single().name)
        rejects { HubCodec.finishDraft(result.copy(draft = CameraDraft("Portão", "Casa", "yoosee", true))) }
    }
    @Test fun cameraLimitIsEnforcedBeforeWriting() {
        val state = HubState(cameras = List(500) { entry("Câmera $it") })
        rejects { HubCodec.merge(state, HubState(cameras = listOf(entry("Mais uma")))) }
        assertEquals(500, state.cameras.size)
    }
    @Test fun malformedBackupDoesNotFallBackToAnEmptyPanel() { rejects { HubCodec.decode("{broken") } }
    @Test fun cameraProviderVariantsRemainSeparate() {
        assertNotEquals(Providers.find("v380")!!.packageName, Providers.find("v380pro")!!.packageName)
        assertEquals(5, Providers.all.map { it.packageName }.toSet().size)
        assertTrue(Providers.all.all { it.storeUrl.startsWith("https://play.google.com/store/apps/details?id=" + it.packageName) })
    }
}
