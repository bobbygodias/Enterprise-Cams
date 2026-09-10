package org.enterprisecams.app

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import kotlinx.coroutines.runBlocking
import org.enterprisecams.app.data.CameraRepository
import org.enterprisecams.app.data.HubState
import org.junit.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class CameraFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context: Context get() = instrumentation.targetContext
    private val device get() = UiDevice.getInstance(instrumentation)

    @Before fun clearPanel() {
        runBlocking { CameraRepository(context).update { HubState() } }
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Adicionar câmera").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun capture(name: String) {
        rule.waitForIdle()
        val directory = File(context.getExternalFilesDir(null), "qa").apply { mkdirs() }
        instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
            File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        device.dumpWindowHierarchy(File(directory, "$name.xml"))
    }

    private fun fillCamera(name: String, provider: String) {
        rule.onNodeWithText("Adicionar câmera").performClick()
        rule.onNodeWithText("Nome da câmera").performTextInput(name)
        rule.onNodeWithText("Local (opcional)").performTextInput("Casa")
        rule.onNodeWithText(provider, useUnmergedTree = true).performScrollTo().performClick()
        rule.onNodeWithText("Continuar").performScrollTo().performClick()
    }

    @Test fun officialHandoffReturnSaveFavoriteEditAndRemove() {
        capture("01-empty")
        fillCamera("Portão", "Yoosee")
        rule.onNodeWithText("Abrir Yoosee").performScrollTo().assertExists()
        capture("02-setup")
        rule.onNodeWithText("Abrir Yoosee").performClick()
        Assert.assertTrue("Fixture activity should receive the Android launch", device.wait(Until.hasObject(By.pkg("com.yoosee")), 8_000))
        device.pressBack()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Concluí a configuração").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Concluí a configuração").performScrollTo().performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Abrir no Yoosee").fetchSemanticsNodes().isNotEmpty() }
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
        rule.onNodeWithContentDescription("Limpar busca").performClick()
        rule.onNodeWithContentDescription("Opções de Entrada").performClick()
        rule.onNodeWithText("Remover do painel").performClick()
        rule.onNodeWithText("Cancelar").performClick()
        rule.onNodeWithText("Entrada").assertExists()
        rule.onNodeWithContentDescription("Opções de Entrada").performClick()
        rule.onNodeWithText("Remover do painel").performClick()
        rule.onNodeWithText("Remover acesso").performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Suas câmeras, no mesmo lugar.").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun missingAppCannotBeMarkedConfiguredAndDraftSurvivesRecreation() {
        fillCamera("TowerCam", "Hilevel")
        rule.onNodeWithText("Instalar Hilevel").performScrollTo().assertExists()
        rule.onNodeWithText("Concluí a configuração").performScrollTo().assertIsNotEnabled()
        rule.activityRule.scenario.recreate()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Instalar Hilevel").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Concluí a configuração").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithContentDescription("Voltar ao painel").performClick()
        rule.onNodeWithText("Continuar cadastro").performClick()
        rule.onNodeWithText("TowerCam").assertExists()
        capture("04-missing-app")
    }

    @Test fun largeTextKeepsSetupActionsReachable() {
        device.executeShellCommand("settings put system font_scale 1.5")
        try {
            rule.activityRule.scenario.recreate()
            fillCamera("Câmera da entrada principal", "V380 Pro")
            rule.onNodeWithText("Instalar V380 Pro").performScrollTo().assertIsDisplayed()
            rule.onNodeWithText("Concluí a configuração").performScrollTo().assertIsDisplayed()
            capture("05-large-text")
        } finally { device.executeShellCommand("settings put system font_scale 1.0") }
    }
}
