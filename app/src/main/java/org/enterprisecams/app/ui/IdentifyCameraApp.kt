package org.enterprisecams.app.ui

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.enterprisecams.app.data.CameraProvider
import org.enterprisecams.app.data.ProviderIdentifier
import org.enterprisecams.app.platform.QrImageReader

@Composable
fun IdentifyCameraApp(enabled: Boolean, onIdentified: (CameraProvider?) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var reading by remember { mutableStateOf(false) }
    val accept: (String) -> Unit = { raw ->
        val provider = ProviderIdentifier.identify(raw)
        if (provider == null) {
            onIdentified(null)
            message = "Não foi possível identificar o aplicativo. O QR pode conter só o número da câmera. Escolha abaixo o nome indicado no manual."
        } else {
            input = ""
            onIdentified(provider)
            message = "Aplicativo identificado: ${provider.name}. Confira a seleção abaixo e continue para instalar ou configurar."
        }
    }
    val scanner = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let(accept) ?: run { message = "Leitura encerrada. Você pode usar uma imagem ou escolher o aplicativo abaixo." }
    }
    val startScan = {
        try {
            scanner.launch(ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt("Aponte para o QR do manual ou da câmera")
                .setBeepEnabled(false).setBarcodeImageEnabled(false).setOrientationLocked(false))
        } catch (_: Exception) { message = "Não foi possível abrir o leitor. Use uma imagem ou cole o link do manual." }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startScan() else message = "Câmera não autorizada. Você pode ler uma imagem ou escolher o aplicativo pelo manual."
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            reading = true
            try { accept(withContext(Dispatchers.IO) { QrImageReader.read(context, uri) }) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { message = e.message ?: "Não foi possível ler o QR dessa imagem." }
            finally { reading = false }
        }
    }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Descobrir o aplicativo", style = MaterialTheme.typography.titleMedium)
            Text("Leia o QR do manual, da caixa ou da câmera. Também pode colar o link ou escrever o nome do aplicativo.")
            OutlinedButton(onClick = {
                if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) permission.launch(android.Manifest.permission.CAMERA)
                else message = "Este aparelho não tem câmera disponível. Use uma imagem ou informe o nome do aplicativo."
            }, enabled = enabled && !reading, modifier = Modifier.fillMaxWidth()) { Text("Ler QR com a câmera") }
            OutlinedButton(onClick = { imagePicker.launch(arrayOf("image/*")) }, enabled = enabled && !reading,
                modifier = Modifier.fillMaxWidth()) { Text("Ler QR de uma imagem") }
            OutlinedTextField(input, { input = it.take(ProviderIdentifier.MAX_INPUT) }, enabled = enabled && !reading,
                label = { Text("Link ou nome do aplicativo") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            TextButton(onClick = { accept(input) }, enabled = enabled && !reading && input.isNotBlank()) { Text("Identificar aplicativo") }
            if (reading) LinearProgressIndicator(Modifier.fillMaxWidth())
            message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            Text("A leitura acontece neste aparelho. Links desconhecidos não são abertos automaticamente.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
