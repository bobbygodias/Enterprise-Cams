package org.enterprisecams.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.enterprisecams.app.cloud.*

@Composable
fun CloudAccountsDialog(controller: AccountController, onDismiss: () -> Unit) {
    var vendor by remember { mutableStateOf(CloudVendor.YOOSEE) }
    var email by remember(vendor) { mutableStateOf("") }
    var password by remember(vendor) { mutableStateOf("") }
    var result by remember(vendor) { mutableStateOf<AccountResult>(AccountResult.SignedOut) }
    var busy by remember(vendor) { mutableStateOf(false) }
    // Camera streams and temporary signed URLs live only while this dialog is composed.
    var playing by remember(vendor) { mutableStateOf<Pair<RemoteCamera, VendorVideoSource>?>(null) }
    var playbackError by remember(vendor) { mutableStateOf<String?>(null) }
    var playerAttempt by remember(vendor) { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(vendor) {
        busy = true
        try { result = controller.refresh(vendor) }
        finally { busy = false }
    }

    fun perform(operation: suspend () -> AccountResult) {
        if (busy) return
        playing = null
        playbackError = null
        busy = true
        scope.launch {
            try { result = operation() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { result = AccountResult.Failed }
            finally { busy = false; password = "" }
        }
    }

    fun open(camera: RemoteCamera) {
        if (busy) return
        playing = null
        playbackError = null
        busy = true
        val target = vendor
        scope.launch {
            try {
                when (val value = controller.openCamera(target, camera.id)) {
                    is CameraSourceResult.Ready -> {
                        playing = camera to value.source
                        playerAttempt++
                    }
                    CameraSourceResult.NotSupported -> playbackError =
                        "O fabricante ainda não forneceu um fluxo de vídeo compatível para esta câmera."
                    CameraSourceResult.NotInAccount -> playbackError =
                        "Esta câmera não consta mais na conta autenticada. Atualize a lista."
                    CameraSourceResult.SignedOut -> playbackError =
                        "A sessão expirou ou foi desconectada. Entre novamente na conta."
                    CameraSourceResult.Unavailable -> playbackError =
                        "O conector desta conta não está disponível nesta versão."
                    CameraSourceResult.Failed -> playbackError =
                        "Não foi possível obter o vídeo. Verifique a conexão e tente novamente."
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { playbackError = "Não foi possível abrir esta câmera." }
            finally { busy = false }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Contas de câmeras") },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CloudVendor.values().forEach { item ->
                        FilterChip(selected = vendor == item, enabled = !busy,
                            onClick = { password = ""; vendor = item }, label = { Text(item.label) })
                    }
                }
                if (!controller.available(vendor)) {
                    Text("O módulo autorizado de conta de " + vendor.label + " não está incluído nesta compilação. Nenhuma senha será solicitada ou transmitida.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    when (val current = result) {
                        is AccountResult.Connected -> {
                            Text("Câmeras da conta: " + current.cameras.size, style = MaterialTheme.typography.titleMedium)
                            if (current.cameras.isEmpty()) Text("Nenhuma câmera retornada pelo serviço.")
                            current.cameras.forEach { camera ->
                                OutlinedButton(enabled = !busy, onClick = { open(camera) },
                                    modifier = Modifier.fillMaxWidth()) {
                                    Text("Assistir: " + camera.name.ifBlank { "Câmera sem nome" })
                                }
                            }
                            playbackError?.let {
                                Text(it, color = MaterialTheme.colorScheme.error)
                            }
                            playing?.let { (camera, stream) ->
                                Text("Vídeo da conta: " + camera.name,
                                    style = MaterialTheme.typography.titleMedium)
                                key(vendor, camera.id, playerAttempt) {
                                    InternalVideoPlayer(
                                        source = (stream as? VendorVideoSource.Https)?.link,
                                        lanSource = (stream as? VendorVideoSource.Rtsp)?.link,
                                        hls = (stream as? VendorVideoSource.Https)?.hls ?: false,
                                        demo = false,
                                        onRetry = { playerAttempt++ }
                                    )
                                }
                                OutlinedButton(onClick = { playing = null }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Encerrar vídeo")
                                }
                            }
                            Text("O vídeo depende de um conector autorizado do fabricante; nenhum link ou token é gravado.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedButton(enabled = !busy, onClick = { perform { controller.refresh(vendor) } }) { Text("Atualizar câmeras") }
                            OutlinedButton(enabled = !busy, onClick = { perform { controller.signOut(vendor) } }) { Text("Desconectar conta") }
                        }
                        else -> {
                            OutlinedTextField(email, { email = it.take(254) }, enabled = !busy,
                                label = { Text("E-mail da conta") }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(password, { password = it.take(1024) }, enabled = !busy,
                                label = { Text("Senha da conta") }, singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth())
                            Button(enabled = !busy && email.isNotBlank() && password.isNotEmpty(),
                                onClick = {
                                    val target = vendor
                                    val login = email
                                    val secret = password
                                    perform { controller.signIn(target, login, secret) }
                                }, modifier = Modifier.fillMaxWidth()) { Text("Entrar e consultar câmeras") }
                            if (current == AccountResult.InvalidCredentials) Text("Confira o e-mail e a senha.", color = MaterialTheme.colorScheme.error)
                            if (current == AccountResult.Failed) Text("Não foi possível autenticar ou consultar o inventário.", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Fechar") } }
    )
}
