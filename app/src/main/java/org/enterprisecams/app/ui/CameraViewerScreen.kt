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
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.ui.PlayerView
import org.enterprisecams.app.BuildConfig
import org.enterprisecams.app.R
import org.enterprisecams.app.data.CameraEntry
import org.enterprisecams.app.data.Providers
import org.enterprisecams.app.playback.HttpsOnlyDataSource
import org.enterprisecams.app.playback.HttpsVideoLink

/** This screen owns playback. It has no reference to OfficialApps or an external Intent. */
@Composable
fun CameraViewerScreen(camera: CameraEntry) {
    val providerName = Providers.find(camera.providerId)?.name.orEmpty()
    // Sensitive links deliberately do not use rememberSaveable, DataStore or backup state.
    var source by remember(camera.id) { mutableStateOf<HttpsVideoLink?>(null) }
    var demo by remember(camera.id) { mutableStateOf(false) }
    var linkForm by remember(camera.id) { mutableStateOf(false) }
    var hls by remember(camera.id) { mutableStateOf(true) }
    var attempt by remember(camera.id) { mutableIntStateOf(0) }
    val scroll = rememberScrollState()
    LaunchedEffect(source, demo, attempt) {
        // A SurfaceView entirely clipped above a scroller may never render its first frame.
        // Bring the video into view when the action is triggered from the buttons below it.
        if (demo || source != null) scroll.scrollTo(0)
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

        if (demo || source != null) {
            Text(if (demo) "TESTE DO PLAYER · imagem gerada, sem câmera conectada" else "LINK DE VÍDEO · sessão temporária",
                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            if (foreground) key(camera.id, source, demo, attempt, hls) {
                InternalVideoPlayer(source, hls, demo, onRetry = { attempt++ })
            } else Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black))
            OutlinedButton(onClick = { source = null; demo = false }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
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
            Button(onClick = { source = null; demo = true; attempt++ }, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) {
                Text("Testar vídeo interno")
            }
            TextButton(onClick = { linkForm = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Tenho um link de vídeo autorizado")
            }
        }
    }

    if (linkForm) VideoLinkDialog(onDismiss = { linkForm = false }, onPlay = { link, isHls ->
        source = link; hls = isHls; demo = false; attempt++; linkForm = false
    })
}

@Composable
private fun VideoLinkDialog(onDismiss: () -> Unit, onPlay: (HttpsVideoLink, Boolean) -> Unit) {
    var input by remember { mutableStateOf("") }
    var hls by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Testar um link de vídeo") }, text = {
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
            Text("O link fica apenas nesta sessão e não entra no backup. Este teste não conecta sua conta do fabricante.")
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
private fun InternalVideoPlayer(source: HttpsVideoLink?, hls: Boolean, demo: Boolean, onRetry: () -> Unit) {
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
        val uri = if (demo) Uri.parse("android.resource://${context.packageName}/${R.raw.player_check}")
            else Uri.parse(requireNotNull(source).value)
        exoPlayer.setMediaItem(MediaItem.Builder().setUri(uri)
            .setMimeType(if (!demo && hls) MimeTypes.APPLICATION_M3U8 else MimeTypes.VIDEO_MP4).build())
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
