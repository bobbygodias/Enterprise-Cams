package org.enterprisecams.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.enterprisecams.app.BuildConfig
import org.enterprisecams.app.HubViewModel
import org.enterprisecams.app.R
import org.enterprisecams.app.data.*
import org.enterprisecams.app.platform.AppAvailability

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterpriseCamsApp(vm: HubViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var showSetup by rememberSaveable { mutableStateOf(false) }
    var about by rememberSaveable { mutableStateOf(false) }
    var backupMenu by remember { mutableStateOf(false) }
    var backupInfo by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<HubState?>(null) }
    var editCamera by remember { mutableStateOf<CameraEntry?>(null) }
    var deleteCamera by remember { mutableStateOf<CameraEntry?>(null) }
    var missingApp by remember { mutableStateOf<CameraProvider?>(null) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.exportBackup(uri)
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            try { pendingImport = vm.readImport(uri) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { vm.message("Não foi possível ler um backup válido. Seus cadastros continuam intactos.") }
        }
    }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) vm.refreshApps() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(ui.message) {
        ui.message?.let { snackbar.showSnackbar(it); vm.dismissMessage() }
    }
    val inSetup = showSetup && ui.hub.draft != null
    BackHandler(inSetup) { showSetup = false }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (inSetup) Text("Adicionar câmera")
                    else Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.enterprise_badge), null,
                            Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("ENTERPRISE", fontSize = 12.sp, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.primary)
                            Text("Cams", fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        }
                    }
                },
                navigationIcon = {
                    if (inSetup) IconButton(onClick = { showSetup = false }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar ao painel")
                    }
                },
                actions = {
                    if (!inSetup) {
                        IconButton(onClick = { backupMenu = true }) { Icon(Icons.Default.MoreVert, "Opções do painel") }
                        DropdownMenu(expanded = backupMenu, onDismissRequest = { backupMenu = false }) {
                            DropdownMenuItem(text = { Text("Salvar backup") }, enabled = ui.loaded && !ui.busy,
                                onClick = { backupMenu = false; backupInfo = true })
                            DropdownMenuItem(text = { Text("Importar backup") }, enabled = ui.loaded && !ui.busy,
                                onClick = { backupMenu = false; import.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) })
                            DropdownMenuItem(text = { Text("Sobre o Enterprise Cams") },
                                onClick = { backupMenu = false; about = true })
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (ui.loaded && !inSetup && ui.hub.cameras.isNotEmpty()) {
                ExtendedFloatingActionButton(onClick = { if (!ui.busy) { vm.startDraft(); showSetup = true } },
                    icon = { Icon(Icons.Default.Add, null) }, text = { Text("Adicionar câmera") })
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            when {
                ui.loadError -> Column(Modifier.widthIn(max = 620.dp).padding(28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Não foi possível ler o cadastro", style = MaterialTheme.typography.headlineSmall)
                    Text("Os dados existentes foram preservados. Tente novamente antes de continuar.")
                    Button(onClick = vm::observe) { Text("Tentar novamente") }
                }
                !ui.loaded -> CircularProgressIndicator(Modifier.padding(40.dp))
                inSetup -> SetupScreen(
                    draft = ui.hub.draft!!, apps = ui.apps, busy = ui.busy,
                    onContinue = vm::saveDraft, onOpen = vm::open, onInstall = vm::install,
                    onFinish = vm::finishDraft, onDiscard = { vm.discardDraft(); showSetup = false },
                )
                else -> HomeScreen(
                    cameras = ui.hub.cameras, draft = ui.hub.draft, apps = ui.apps, enabled = !ui.busy,
                    onAdd = { vm.startDraft(); showSetup = true }, onResumeDraft = { showSetup = true },
                    onOpen = { camera ->
                        Providers.find(camera.providerId)?.let { provider ->
                            if (ui.apps[provider.id] == AppAvailability.MISSING) missingApp = provider else vm.open(provider)
                        }
                    },
                    onFavorite = { vm.favorite(it.id) }, onEdit = { editCamera = it }, onRemove = { deleteCamera = it },
                )
            }
            if (ui.busy) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
        }
    }

    missingApp?.let { provider ->
        AlertDialog(onDismissRequest = { missingApp = null }, title = { Text("Instalar ${provider.name}") },
            text = { Text("O acesso à câmera está salvo. Instale o aplicativo oficial e depois volte ao painel.") },
            confirmButton = { TextButton(onClick = { missingApp = null; vm.install(provider) }) { Text("Ir para instalação") } },
            dismissButton = { TextButton(onClick = { missingApp = null }) { Text("Agora não") } })
    }
    editCamera?.let { camera ->
        EditDialog(camera, onDismiss = { editCamera = null }, onSave = { name, location ->
            vm.edit(camera.id, name, location); editCamera = null
        })
    }
    deleteCamera?.let { camera ->
        AlertDialog(onDismissRequest = { deleteCamera = null }, title = { Text("Remover ${camera.name}?") },
            text = { Text("Remove apenas o acesso deste painel. A câmera e suas gravações continuam no aplicativo oficial.") },
            confirmButton = { TextButton(onClick = { vm.remove(camera.id); deleteCamera = null }) { Text("Remover acesso") } },
            dismissButton = { TextButton(onClick = { deleteCamera = null }) { Text("Cancelar") } })
    }
    if (backupInfo) AlertDialog(onDismissRequest = { backupInfo = false }, title = { Text("Salvar seu painel") },
        text = { Text("O arquivo contém nomes, locais e aplicativos das câmeras. Não inclui senhas nem gravações e não é criptografado. Escolha um local privado para guardá-lo.") },
        confirmButton = { TextButton(onClick = { backupInfo = false; export.launch("Enterprise-Cams-backup.json") }) { Text("Escolher local") } },
        dismissButton = { TextButton(onClick = { backupInfo = false }) { Text("Cancelar") } })
    pendingImport?.let { backup ->
        AlertDialog(onDismissRequest = { pendingImport = null }, title = { Text("Importar ${backup.cameras.size} câmeras?") },
            text = { Text("Os acessos serão acrescentados ao painel. Seus cadastros atuais serão preservados; entradas iguais não serão repetidas.") },
            confirmButton = { TextButton(onClick = { vm.importBackup(backup); pendingImport = null }) { Text("Importar") } },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("Cancelar") } })
    }
    if (about) AlertDialog(onDismissRequest = { about = false }, title = { Text("Enterprise Cams") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Suas câmeras, organizadas por você.", fontWeight = FontWeight.Bold)
            Text("Versão ${BuildConfig.VERSION_NAME} · primeira versão de testes")
            Text("O painel guarda nomes, locais e favoritos neste aparelho. Não possui anúncios, conta própria, telemetria ou conexão de rede.")
            Text("O vídeo e os controles abrem no aplicativo oficial correspondente. Nesta versão, a seleção da câmera ainda acontece nele.")
            Text("Internet, login, anúncios e funcionamento das câmeras dependem de cada aplicativo oficial. O Enterprise Cams não altera esses aplicativos.")
            Text("Código aberto · licença CC0\nBobby Dias & Andrew Vox")
        } }, confirmButton = { TextButton(onClick = { about = false }) { Text("Fechar") } })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeScreen(
    cameras: List<CameraEntry>, draft: CameraDraft?, apps: Map<String, AppAvailability>, enabled: Boolean,
    onAdd: () -> Unit, onResumeDraft: () -> Unit, onOpen: (CameraEntry) -> Unit,
    onFavorite: (CameraEntry) -> Unit, onEdit: (CameraEntry) -> Unit, onRemove: (CameraEntry) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    var favorites by rememberSaveable { mutableStateOf(false) }
    var location by rememberSaveable { mutableStateOf("") }
    val locations = cameras.map { it.location }.filter { it.isNotBlank() }.distinct().sorted()
    LaunchedEffect(locations) { if (location !in locations) location = "" }
    val filtered = cameras.filter {
        (!favorites || it.favorite) && (location.isBlank() || it.location == location) &&
            (query.isBlank() || listOf(it.name, it.location, Providers.find(it.providerId)?.name.orEmpty()).any { value -> value.contains(query.trim(), ignoreCase = true) })
    }.sortedWith(compareByDescending<CameraEntry> { it.favorite }.thenBy { it.name.lowercase() })

    LazyColumn(Modifier.fillMaxSize().widthIn(max = 760.dp), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF163E51), Color(0xFF10202F)))).padding(24.dp)) {
                Text("SEU POSTO DE OBSERVAÇÃO", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Text("Minhas câmeras", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(if (cameras.isEmpty()) "Cada lugar, ao seu alcance." else "${cameras.size} ${if (cameras.size == 1) "acesso salvo" else "acessos salvos"} neste aparelho.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (draft != null) item {
            OutlinedCard(onClick = onResumeDraft, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("Continuar cadastro", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Text(draft.name.ifBlank { "Você tem uma câmera para terminar de adicionar." })
                }
            }
        }
        if (cameras.isEmpty()) item {
            Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Image(painterResource(R.drawable.enterprise_badge), null, Modifier.size(152.dp).clip(RoundedCornerShape(24.dp)))
                Text("Suas câmeras, no mesmo lugar.", style = MaterialTheme.typography.titleLarge)
                Text("Escolha o aplicativo oficial, configure sua câmera e dê um nome ao acesso.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onAdd, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Adicionar câmera")
                }
            }
        } else {
            item {
                OutlinedTextField(query, { query = it.take(HubCodec.MAX_TEXT) }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("Buscar câmera ou local") }, leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = ""; focusManager.clearFocus() }) { Icon(Icons.Default.Close, "Limpar busca") } })
            }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !favorites, onClick = { favorites = false }, label = { Text("Todas") })
                    FilterChip(selected = favorites, onClick = { favorites = !favorites }, label = { Text("Favoritas") })
                    if (locations.isNotEmpty()) {
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            FilterChip(selected = location.isNotEmpty(), onClick = { expanded = true }, label = { Text(location.ifBlank { "Todos os locais" }) })
                            DropdownMenu(expanded, { expanded = false }) {
                                DropdownMenuItem(text = { Text("Todos os locais") }, onClick = { location = ""; expanded = false })
                                locations.forEach { loc -> DropdownMenuItem(text = { Text(loc) }, onClick = { location = loc; expanded = false }) }
                            }
                        }
                    }
                }
            }
            if (filtered.isEmpty()) item {
                Text("Nenhuma câmera neste filtro. Experimente outro nome ou local.", Modifier.padding(vertical = 20.dp))
            }
            items(filtered, key = { it.id }) { camera ->
                CameraCard(camera, apps[camera.providerId] ?: AppAvailability.MISSING, enabled,
                    onOpen = { onOpen(camera) }, onFavorite = { onFavorite(camera) }, onEdit = { onEdit(camera) }, onRemove = { onRemove(camera) })
            }
        }
    }
}

@Composable
private fun CameraCard(camera: CameraEntry, availability: AppAvailability, enabled: Boolean,
    onOpen: () -> Unit, onFavorite: () -> Unit, onEdit: () -> Unit, onRemove: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val providerName = Providers.find(camera.providerId)?.name.orEmpty()
    Card(onClick = onOpen, enabled = enabled, modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(start = 18.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(camera.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(listOf(camera.location, providerName).filter { it.isNotBlank() }.joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(when (availability) {
                    AppAvailability.READY -> "Abrir no $providerName"
                    AppAvailability.MISSING -> "Instalar $providerName"
                    AppAvailability.UNAVAILABLE -> "$providerName indisponível"
                }, color = if (availability == AppAvailability.READY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge)
            }
            Column {
                IconButton(onClick = onFavorite, enabled = enabled) {
                    Icon(if (camera.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        if (camera.favorite) "Desfavoritar ${camera.name}" else "Favoritar ${camera.name}",
                        tint = if (camera.favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box {
                    IconButton(onClick = { menu = true }, enabled = enabled) { Icon(Icons.Default.MoreVert, "Opções de ${camera.name}") }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(text = { Text("Editar nome e local") }, onClick = { menu = false; onEdit() })
                        DropdownMenuItem(text = { Text("Remover do painel") }, onClick = { menu = false; onRemove() })
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupScreen(draft: CameraDraft, apps: Map<String, AppAvailability>, busy: Boolean,
    onContinue: (CameraDraft) -> Unit, onOpen: (CameraProvider) -> Unit, onInstall: (CameraProvider) -> Unit,
    onFinish: () -> Unit, onDiscard: () -> Unit) {
    var discard by remember { mutableStateOf(false) }
    Column(Modifier.widthIn(max = 680.dp).fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        if (!draft.setupStarted) {
            var name by rememberSaveable(draft) { mutableStateOf(draft.name) }
            var location by rememberSaveable(draft) { mutableStateOf(draft.location) }
            var providerId by rememberSaveable(draft) { mutableStateOf(draft.providerId) }
            Text("Qual câmera vamos adicionar?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Use um nome fácil de reconhecer, como Portão ou Garagem.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            NameField(name, { name = it }, "Nome da câmera")
            NameField(location, { location = it }, "Local (opcional)")
            IdentifyCameraApp(enabled = !busy, onIdentified = { providerId = it?.id.orEmpty() })
            Text("Aplicativo indicado no manual", style = MaterialTheme.typography.titleMedium)
            Providers.all.forEach { provider ->
                Surface(shape = RoundedCornerShape(14.dp), color = if (providerId == provider.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth().clickable(role = Role.RadioButton, enabled = !busy) { providerId = provider.id }) {
                    Row(Modifier.padding(12.dp).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = providerId == provider.id, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(provider.name, fontWeight = FontWeight.SemiBold)
                            Text(when (apps[provider.id]) {
                                AppAvailability.READY -> "Instalado neste aparelho"
                                AppAvailability.UNAVAILABLE -> "Instalado, mas indisponível"
                                else -> "Instalação necessária"
                            }, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            Button(onClick = { onContinue(CameraDraft(name.trim(), location.trim(), providerId, setupStarted = true)) },
                enabled = name.isNotBlank() && Providers.find(providerId) != null && !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) { Text("Continuar") }
        } else {
            val provider = requireNotNull(Providers.find(draft.providerId))
            val availability = apps[provider.id] ?: AppAvailability.MISSING
            Text(draft.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(listOf(draft.location, provider.name).filter { it.isNotBlank() }.joinToString(" · "), color = MaterialTheme.colorScheme.primary)
            SetupStep("1", "Configure no ${provider.name}",
                if (availability == AppAvailability.MISSING) "Instale o aplicativo oficial e adicione sua câmera nele. Se já instalou pelo fabricante, volte a esta tela."
                else "Abra o aplicativo oficial e adicione sua câmera. Se ela já está configurada, confira se a imagem aparece.")
            when (availability) {
                AppAvailability.READY -> OutlinedButton(onClick = { onOpen(provider) }, enabled = !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) { Text("Abrir ${provider.name}") }
                AppAvailability.MISSING -> Button(onClick = { onInstall(provider) }, enabled = !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) { Text("Instalar ${provider.name}") }
                AppAvailability.UNAVAILABLE -> Text("O aplicativo está desativado ou não pode ser aberto. Verifique-o nas configurações do Android e volte aqui.", color = MaterialTheme.colorScheme.error)
            }
            SetupStep("2", "Volte e salve o acesso", "Quando a câmera estiver funcionando, volte ao Enterprise Cams e confirme abaixo. Seu cadastro fica guardado enquanto isso.")
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                Text("Nesta versão, o acesso abre o ${provider.name}. Nele, selecione a câmera ${draft.name}.", Modifier.padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onFinish, enabled = availability == AppAvailability.READY && !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) { Text("Concluí a configuração") }
            TextButton(onClick = { onContinue(draft.copy(setupStarted = false)) }, enabled = !busy) { Text("Editar nome ou aplicativo") }
        }
        TextButton(onClick = { discard = true }, enabled = !busy) { Text("Descartar este cadastro") }
    }
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("Descartar cadastro?") },
        text = { Text("Isso descarta apenas este cadastro em andamento no Enterprise Cams.") },
        confirmButton = { TextButton(onClick = { discard = false; onDiscard() }) { Text("Descartar") } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text("Continuar cadastro") } })
}

@Composable
private fun SetupStep(number: String, title: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Text(number, Modifier.padding(horizontal = 14.dp, vertical = 8.dp), fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NameField(value: String, onValue: (String) -> Unit, label: String) {
    OutlinedTextField(value, { onValue(it.filterNot(Char::isISOControl).take(HubCodec.MAX_TEXT)) }, modifier = Modifier.fillMaxWidth(),
        label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
}

@Composable
private fun EditDialog(camera: CameraEntry, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by rememberSaveable(camera.id) { mutableStateOf(camera.name) }
    var location by rememberSaveable(camera.id) { mutableStateOf(camera.location) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Editar câmera") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            NameField(name, { name = it }, "Nome da câmera")
            NameField(location, { location = it }, "Local (opcional)")
        }
    }, confirmButton = { TextButton(onClick = { onSave(name, location) }, enabled = name.isNotBlank()) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}
