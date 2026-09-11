package org.enterprisecams.app

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.runBlocking
import org.enterprisecams.app.data.*
import org.enterprisecams.app.platform.QrImageReader
import org.junit.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class QrIdentificationTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Before fun clearPanel() {
        runBlocking { CameraRepository(InstrumentationRegistry.getInstrumentation().targetContext).update { HubState() } }
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Suas câmeras, no mesmo lugar.").fetchSemanticsNodes().isNotEmpty() }
    }
    private fun openSetup() {
        rule.onNodeWithText("Adicionar câmera").performScrollTo().performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Nome da câmera").fetchSemanticsNodes().isNotEmpty() }
    }
    @Test fun qrPixelsDecodeOfflineToTheExactOfficialStoreApp() {
        val provider = requireNotNull(Providers.find("icsee"))
        val matrix = QRCodeWriter().encode(provider.storeUrl, BarcodeFormat.QR_CODE, 400, 400)
        val pixels = IntArray(400 * 400) { index -> if (matrix[index % 400, index / 400]) android.graphics.Color.BLACK else android.graphics.Color.WHITE }
        val bitmap = Bitmap.createBitmap(pixels, 400, 400, Bitmap.Config.ARGB_8888)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "qa-qr.png")
        try {
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            Assert.assertEquals(provider, ProviderIdentifier.identify(QrImageReader.read(context, android.net.Uri.fromFile(file))))
        }
        finally { bitmap.recycle(); file.delete() }
    }
    @Test fun liveReaderOpensOnDemandAndCancellationReturnsToSetup() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = androidx.test.uiautomator.UiDevice.getInstance(instrumentation)
        device.executeShellCommand("pm grant ${instrumentation.targetContext.packageName} android.permission.CAMERA")
        openSetup()
        rule.onNodeWithText("Ler QR com a câmera").performScrollTo().performClick()
        Assert.assertTrue(device.wait(androidx.test.uiautomator.Until.hasObject(androidx.test.uiautomator.By.textContains("Aponte para o QR")), 10_000))
        device.pressBack()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Leitura encerrada. Você pode usar uma imagem ou escolher o aplicativo abaixo.").fetchSemanticsNodes().isNotEmpty() }
    }
    @Test fun identificationRoutesMissingAppToInstallationAndKeepsDraft() {
        openSetup()
        rule.onNodeWithText("Nome da câmera").performScrollTo().performTextInput("Garagem")
        rule.onNodeWithText("Link ou nome do aplicativo").performScrollTo().performTextInput("https://play.google.com/store/apps/details?id=com.xm.csee")
        rule.onNodeWithText("Identificar aplicativo").performScrollTo().performClick()
        rule.onNodeWithText("Aplicativo identificado: iCSee. Confira a seleção abaixo e continue para instalar ou configurar.").assertExists()
        rule.onNodeWithText("Continuar").performScrollTo().performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Instalar iCSee").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Instalar iCSee").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Concluí a configuração").performScrollTo().assertIsNotEnabled()
        rule.activityRule.scenario.recreate()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Instalar iCSee").fetchSemanticsNodes().isNotEmpty() }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null), "qa").apply { mkdirs() }
        InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()?.let { bitmap ->
            File(directory, "06-identified-install.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
        }
        val device = androidx.test.uiautomator.UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.executeShellCommand("mkdir -p /sdcard/Download/enterprise-qa")
        device.executeShellCommand("cp ${directory.absolutePath}/06-identified-install.png /sdcard/Download/enterprise-qa/06-identified-install.png")
    }
    @Test fun serialOnlyQrHasAnHonestManualFallback() {
        openSetup()
        rule.onNodeWithText("Nome da câmera").performScrollTo().performTextInput("Sala")
        rule.onNodeWithText("Link ou nome do aplicativo").performScrollTo().performTextInput("123456789")
        rule.onNodeWithText("Identificar aplicativo").performScrollTo().performClick()
        rule.onNodeWithText("Não foi possível identificar o aplicativo. O QR pode conter só o número da câmera. Escolha abaixo o nome indicado no manual.").assertExists()
        rule.onNodeWithText("Hilevel", useUnmergedTree = true).performScrollTo().assertExists()
        rule.onNodeWithText("Continuar").performScrollTo().assertIsNotEnabled()
    }
}
