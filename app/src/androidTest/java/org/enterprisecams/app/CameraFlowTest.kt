package org.enterprisecams.app

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.runBlocking
import org.enterprisecams.app.data.CameraRepository
import org.enterprisecams.app.data.HubState
import org.junit.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class CameraFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @get:Rule val testName = org.junit.rules.TestName()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context: Context get() = instrumentation.targetContext
    private val device get() = UiDevice.getInstance(instrumentation)

    @Before fun clearPanel() {
        runBlocking { CameraRepository(context).update { HubState() } }
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Suas câmeras, no mesmo lugar.").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun capture(name: String) {
        rule.waitForIdle()
        val directory = File(context.getExternalFilesDir(null), "qa").apply { mkdirs() }
        instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
            File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        device.dumpWindowHierarchy(File(directory, "$name.xml"))
        device.executeShellCommand("mkdir -p /sdcard/Download/enterprise-qa")
        device.executeShellCommand("cp ${directory.absolutePath}/$name.png /sdcard/Download/enterprise-qa/$name.png")
        device.executeShellCommand("cp ${directory.absolutePath}/$name.xml /sdcard/Download/enterprise-qa/$name.xml")
    }

    @After fun captureFinalState() { capture("99-${testName.methodName}") }

    private fun fillCamera(name: String, provider: String) {
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Suas câmeras, no mesmo lugar.").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Adicionar câmera").performScrollTo().performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Nome da câmera").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Nome da câmera").performScrollTo().performTextInput(name)
        rule.onNodeWithText("Local (opcional)").performScrollTo().performTextInput("Casa")
        rule.onNodeWithText(provider, useUnmergedTree = true).performScrollTo().performClick()
        rule.onNodeWithText("Continuar").performScrollTo().performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Salvar câmera no painel").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun internalViewerPlaybackSaveFavoriteEditAndRemove() {
        capture("01-empty")
        fillCamera("Portão", "Yoosee")
        rule.onNodeWithText("Abrir Yoosee").performScrollTo().assertExists()
        capture("02-setup")
        rule.onNodeWithText("Salvar câmera no painel").performScrollTo().performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Abrir visualizador interno").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Portão").performClick()
        rule.onNodeWithText("Conexão da câmera pendente").assertExists()
        rule.onNodeWithText("Câmera adicionada ao seu painel.").assertDoesNotExist()
        Assert.assertEquals("Viewing must stay inside Enterprise", "org.enterprisecams.app", device.currentPackageName)
        rule.onNodeWithText("Testar vídeo interno").performScrollTo().assertIsDisplayed().performClick()
        rule.waitUntil(5_000) {
            rule.onAllNodesWithText("TESTE DO PLAYER · imagem gerada, sem câmera conectada").fetchSemanticsNodes().isNotEmpty()
        }
        rule.waitUntil(20_000) { rule.onAllNodesWithText("Vídeo de teste em reprodução").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Pausar").performScrollTo().performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Reprodução pausada ou concluída").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Reproduzir").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Vídeo de teste em reprodução").fetchSemanticsNodes().isNotEmpty() }
        capture("02b-internal-video")
        rule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        rule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        rule.waitUntil(20_000) { rule.onAllNodesWithText("Vídeo de teste em reprodução").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithContentDescription("Voltar ao painel").performClick()
        rule.onNodeWithContentDescription("Favoritar Portão").performClick()
        rule.onNodeWithText("Favoritas").performClick()
        rule.onNodeWithText("Portão").assertExists()
        rule.onNodeWithContentDescription("Desfavoritar Portão").assertExists()
        capture("03-panel")
        rule.activityRule.scenario.recreate()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Portão").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithContentDescription("Opções de Portão").performClick()
        rule.onNodeWithText("Editar nome e local").performClick()
        rule.onNodeWithText("Nome da câmera").performTextReplacement("Entrada")
        rule.onNodeWithText("Salvar").performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Entrada").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Buscar câmera ou local").performTextInput("Não existe")
        rule.onNodeWithText("Nenhuma câmera neste filtro. Experimente outro nome ou local.").assertExists()
        rule.onNodeWithContentDescription("Limpar busca").performScrollTo().performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Entrada").fetchSemanticsNodes().isNotEmpty() }
        capture("03b-cleared-search")
        rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Entrada"))
        rule.onNodeWithContentDescription("Opções de Entrada").performClick()
        rule.onNodeWithText("Remover do painel").performClick()
        rule.onNodeWithText("Cancelar").performClick()
        rule.onNodeWithText("Entrada").assertExists()
        rule.onNodeWithContentDescription("Opções de Entrada").performClick()
        rule.onNodeWithText("Remover do painel").performClick()
        rule.onNodeWithText("Remover acesso").performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Suas câmeras, no mesmo lugar.").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun cameraCanBeSavedWithoutVendorAppAndDraftSurvivesRecreation() {
        fillCamera("TowerCam", "Hilevel")
        rule.onNodeWithText("Instalar Hilevel").performScrollTo().assertExists()
        rule.onNodeWithText("Salvar câmera no painel").performScrollTo().assertIsEnabled()
        rule.activityRule.scenario.recreate()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Instalar Hilevel").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Salvar câmera no painel").performScrollTo().assertIsEnabled()
        rule.onNodeWithContentDescription("Voltar ao painel").performClick()
        rule.onNodeWithText("Continuar cadastro").performClick()
        rule.onNodeWithText("TowerCam").assertExists()
        rule.onNodeWithText("Salvar câmera no painel").performScrollTo().performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Abrir visualizador interno").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("TowerCam").performClick()
        rule.onNodeWithText("Conexão da câmera pendente").assertExists()
        Assert.assertEquals("org.enterprisecams.app", device.currentPackageName)
        capture("04-missing-app-internal-view")
    }

    @Test fun largeTextKeepsSetupActionsReachable() {
        device.executeShellCommand("settings put system font_scale 1.5")
        try {
            rule.activityRule.scenario.recreate()
            fillCamera("Câmera da entrada principal", "V380 Pro")
            rule.onNodeWithText("Instalar V380 Pro").performScrollTo().assertIsDisplayed()
            rule.onNodeWithText("Salvar câmera no painel").performScrollTo().assertIsDisplayed()
            capture("05-large-text")
        } finally { device.executeShellCommand("settings put system font_scale 1.0") }
    }
}
