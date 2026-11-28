package org.enterprisecams.app.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.ui.PlayerView
import org.enterprisecams.app.BuildConfig
import org.enterprisecams.app.R
import org.enterprisecams.app.data.CameraEntry
import org.enterprisecams.app.data.Providers
import org.enterprisecams.app.playback.HttpsOnlyDataSource
import org.enterprisecams.app.playback.HttpsVideoLink
import org.enterprisecams.app.playback.LanRtspLink
import org.enterprisecams.app.playback.OnvifMediaClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** This screen owns playback. It has no reference to OfficialApps or an external Intent. */
@Composable
fun CameraViewerScreen(camera: CameraEntry, onOfficial: () -> Unit) {
    val providerName = Providers.find(camera.providerId)?.name.orEmpty()
    // Sensitive links deliberately do not use rememberSaveable, DataStore or backup state.
    var source by remember(camera.id) { mutableStateOf<HttpsVideoLink?>(null) }
    var lanSource by remember(camera.id) { mutableStateOf<LanRtspLink?>(null) }
    var lanForm by remember(camera.id) { mutableStateOf(false) }
    var onvifForm by remember(camera.id) { mutableStateOf(false) }
    var demo by remember(camera.id) { mutableStateOf(false) }
    var linkForm by remember(camera.id) { mutableStateOf(false) }
    var hls by remember(camera.id) { mutableStateOf(true) }
    var attempt by remember(camera.id) { mutableIntStateOf(0) }
    val scroll = rememberScrollState()
    LaunchedEffect(source, lanSource, demo, attempt) {
        // A SurfaceView entirely clipped above a scroller may never render its first frame.
        // Bring the video into view when the action is triggered from the buttons below it.
        if (demo || source != null || lanSource != null) scroll.scrollTo(0)
    }
    val owner = LocalLifecycleOwner.current
    var foreground by remember(owner) { mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, _ ->
            foreground = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    Column(Modifier.fillMaxSize().widthIn(max = 900.dp).verticalScroll(scroll)
        .imePadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(camera.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(listOf(camera.location, providerName).filter(String::isNotBlank).joinToString(" · "),
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (demo || source != null || lanSource != null) {
            Text(if (demo) "TESTE DO PLAYER · imagem gerada, sem câmera conectada" else if (lanSource != null) "RTSP LOCAL · sessão temporária, sem criptografia" else "LINK DE VÍDEO · sessão temporária",
                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            if (foreground) key(camera.id, source, lanSource, demo, attempt, hls) {
                InternalVideoPlayer(source, lanSource, hls, demo, onRetry = { attempt++ })
            } else Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black))
            OutlinedButton(onClick = { source = null; lanSource = null; demo = false }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text("Encerrar reprodução")
            }
        } else Surface(shape = MaterialTheme.shapes.large, color = Color(0xFF0B1925)) {
            Box(Modifier.fillMaxWidth().heightIn(min = 180.dp).padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Conexão da câmera pendente", style = MaterialTheme.typography.titleLarge)
                    Text("A imagem de $providerName ainda não está disponível nesta versão de teste.")
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Nuvem $providerName", style = MaterialTheme.typography.titleLarge)
                Text("A conta e a assinatura continuam com o fabricante. Para comprar ou renovar a nuvem, entre no $providerName.")
                Text("Login, imagem ao vivo, gravações da nuvem e controles desta câmera ainda aguardam integração.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (BuildConfig.DEBUG) {
            Text("Verificação desta versão", style = MaterialTheme.typography.titleMedium)
            Text("O vídeo abaixo verifica a reprodução dentro do Enterprise. Ele não usa sua câmera nem sua conta.")
            Button(onClick = { source = null; lanSource = null; demo = true; attempt++ }, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) {
                Text("Testar vídeo interno")
            }
        }
        Button(onClick = { linkForm = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) {
            Text("Tenho um link de vídeo autorizado")
        }
        OutlinedButton(onClick = { lanForm = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) {
            Text("Conectar câmera RTSP na rede local")
        }
        OutlinedButton(onClick = { onvifForm = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) {
            Text("Buscar perfis ONVIF da câmera")
        }
        OutlinedButton(onClick = onOfficial, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Configuração oficial: $providerName")
        }
    }

    if (linkForm) VideoLinkDialog(onDismiss = { linkForm = false }, onPlay = { link, isHls ->
        source = link; lanSource = null; hls = isHls; demo = false; attempt++; linkForm = false
    })
    if (onvifForm) OnvifDialog(onDismiss = { onvifForm = false }, onPlay = { link ->
        lanSource = link; source = null; demo = false; attempt++; onvifForm = false
    })
    if (lanForm) LanLinkDialog(onDismiss = { lanForm = false }, onPlay = { link ->
        lanSource = link; source = null; demo = false; attempt++; lanForm = false
    })
}

@Composable
private fun OnvifDialog(onDismiss: () -> Unit, onPlay: (LanRtspLink) -> Unit) {
    var address by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var profiles by remember { mutableStateOf<List<OnvifMediaClient.Profile>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun execute(profile: OnvifMediaClient.Profile? = null) {
        if (busy) return
        busy = true; error = false
        scope.launch {
            try {
                if (profile == null) {
                    profiles = withContext(Dispatchers.IO) { OnvifMediaClient.listProfiles(address, user, password) }
                } else {
                    val link = withContext(Dispatchers.IO) { OnvifMediaClient.resolve(address, user, password, profile) }
                    password = ""
                    onPlay(link)
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { error = true }
            finally { busy = false }
        }
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Serviço Media ONVIF") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Informe o serviço Media HTTPS e a conta ONVIF da sua câmera. O certificado precisa ser confiável. O vídeo RTSP resultante não é criptografado: use uma rede local confiável.")
            OutlinedTextField(address, { address = it.take(2048); profiles = emptyList(); error = false },
                enabled = !busy, label = { Text("Endereço HTTPS do serviço Media") }, singleLine = true)
            OutlinedTextField(user, { user = it.take(256); profiles = emptyList(); error = false },
                enabled = !busy, label = { Text("Usuário ONVIF/RTSP") }, singleLine = true)
            OutlinedTextField(password, { password = it.take(512); profiles = emptyList(); error = false },
                enabled = !busy, label = { Text("Senha ONVIF/RTSP") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
            if (busy) CircularProgressIndicator()
            if (error) Text("Não foi possível consultar a câmera. Confira serviço, certificado e credenciais. Câmeras somente HTTP devem usar o endereço RTSP manual.", color = MaterialTheme.colorScheme.error)
            for (profile in profiles) {
                OutlinedButton(enabled = !busy, onClick = { execute(profile) }, modifier = Modifier.fillMaxWidth()) {
                    Text(profile.name.ifBlank { profile.token })
                }
            }
        }
    }, confirmButton = {
        TextButton(enabled = !busy && address.isNotBlank() && user.isNotBlank() && password.isNotBlank(),
            onClick = { execute() }) { Text("Consultar perfis") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
private fun LanLinkDialog(onDismiss: () -> Unit, onPlay: (LanRtspLink) -> Unit) {
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Câmera RTSP local") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Use o endereço RTSP fornecido para sua própria câmera, com IP privado. RTSP não criptografa o vídeo; utilize apenas uma rede local confiável.")
            OutlinedTextField(input, { input = it.take(LanRtspLink.MAX_LENGTH); error = null },
                label = { Text("Endereço RTSP") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), isError = error != null)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Text("O endereço e eventuais credenciais não são salvos. O modo RTSP não conecta a conta do fabricante.")
        }
    }, confirmButton = {
        TextButton(enabled = input.isNotBlank(), onClick = {
            try { onPlay(LanRtspLink.parse(input)); input = "" }
            catch (e: IllegalArgumentException) { error = e.message }
        }) { Text("Conectar") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
private fun VideoLinkDialog(onDismiss: () -> Unit, onPlay: (HttpsVideoLink, Boolean) -> Unit) {
    var input by remember { mutableStateOf("") }
    var hls by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Reproduzir link de vídeo autorizado") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Use um link HTTPS de vídeo HLS ou MP4 fornecido por um serviço que você tem autorização para acessar. Links de instalação e códigos QR da câmera não são links de vídeo.")
            OutlinedTextField(input, { input = it.take(HttpsVideoLink.MAX_LENGTH); error = null },
                label = { Text("Link HTTPS") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), isError = error != null)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(hls, { hls = true }, label = { Text("HLS") })
                FilterChip(!hls, { hls = false }, label = { Text("MP4") })
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Text("O link fica apenas nesta sessão e não entra no backup. Este link não conecta sua conta do fabricante.")
        }
    }, confirmButton = {
        TextButton(enabled = input.isNotBlank(), onClick = {
            try { onPlay(HttpsVideoLink.parse(input), hls); input = "" }
            catch (e: IllegalArgumentException) { error = e.message }
        }) { Text("Reproduzir aqui") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
@Composable
private fun InternalVideoPlayer(source: HttpsVideoLink?, lanSource: LanRtspLink?, hls: Boolean, demo: Boolean, onRetry: () -> Unit) {
    val context = LocalContext.current
    var firstFrame by remember { mutableStateOf(false) }
    var buffering by remember { mutableStateOf(true) }
    var failure by remember { mutableStateOf<String?>(null) }
    var playing by remember { mutableStateOf(false) }
    var muted by remember { mutableStateOf(true) }
    val exoPlayer = remember {
        // Library exception traces may include signed media URLs. Never emit them.
        Log.setLogLevel(Log.LOG_LEVEL_OFF)
        val factory = if (demo) DefaultDataSource.Factory(context) else HttpsOnlyDataSource.factory()
        ExoPlayer.Builder(context).setMediaSourceFactory(
            DefaultMediaSourceFactory(factory).setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(1))
        ).build().apply {
            volume = 0f
            repeatMode = if (demo) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        }
    }
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() { firstFrame = true }
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
            override fun onPlaybackStateChanged(state: Int) { buffering = state == Player.STATE_BUFFERING }
            override fun onPlayerError(error: PlaybackException) {
                buffering = false
                failure = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "O serviço recusou o link. Ele pode ter expirado ou exigir autorização. Obtenha outro link no serviço de origem."
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "Não foi possível receber o vídeo. Confira sua conexão e tente novamente."
                    else -> "Não foi possível reproduzir este vídeo. Confira o link e o formato escolhido."
                }
            }
        }
        exoPlayer.addListener(listener)
        if (lanSource != null) {
            val item = MediaItem.fromUri(lanSource.value)
            exoPlayer.setMediaSource(RtspMediaSource.Factory().setForceUseRtpTcp(true).createMediaSource(item))
        } else {
            val uri = if (demo) Uri.parse("android.resource://${context.packageName}/${R.raw.player_check}")
                else Uri.parse(requireNotNull(source).value)
            exoPlayer.setMediaItem(MediaItem.Builder().setUri(uri)
                .setMimeType(if (!demo && hls) MimeTypes.APPLICATION_M3U8 else MimeTypes.VIDEO_MP4).build())
        }
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black), contentAlignment = Alignment.Center) {
            AndroidView(factory = { PlayerView(it).apply { this.player = exoPlayer; useController = false } },
                update = { it.keepScreenOn = playing }, modifier = Modifier.fillMaxSize())
            if (buffering) CircularProgressIndicator()
        }
        Text(when {
            failure != null -> failure!!
            buffering -> "Carregando vídeo…"
            firstFrame && playing -> if (demo) "Vídeo de teste em reprodução" else "Vídeo em reprodução"
            firstFrame -> "Reprodução pausada ou concluída"
            else -> "Preparando imagem…"
        }, color = if (failure == null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error)
        if (failure != null) Button(onClick = onRetry, modifier = Modifier.heightIn(min = 52.dp)) { Text("Tentar novamente") }
        else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() },
                modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { Text(if (playing) "Pausar" else "Reproduzir") }
            OutlinedButton(onClick = { muted = !muted; exoPlayer.volume = if (muted) 0f else 1f },
                modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { Text(if (muted) "Ativar som" else "Silenciar") }
        }
    }
}
