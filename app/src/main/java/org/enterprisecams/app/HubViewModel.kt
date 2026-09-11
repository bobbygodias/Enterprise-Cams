package org.enterprisecams.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.enterprisecams.app.data.*
import org.enterprisecams.app.platform.*

data class HubUiState(
    val loaded: Boolean = false,
    val hub: HubState = HubState(),
    val loadError: Boolean = false,
    val busy: Boolean = false,
    val message: String? = null,
    val apps: Map<String, AppAvailability> = emptyMap(),
)

class HubViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CameraRepository(application)
    private val officialApps = OfficialApps(application)
    private val mutable = MutableStateFlow(HubUiState())
    val ui = mutable.asStateFlow()
    private var watchJob: Job? = null

    init { observe(); refreshApps() }

    fun observe() {
        watchJob?.cancel()
        watchJob = viewModelScope.launch {
            try {
                repository.states.collect { hub -> mutable.value = mutable.value.copy(loaded = true, hub = hub, loadError = false) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutable.value = mutable.value.copy(loadError = true, loaded = false) }
        }
    }

    fun refreshApps() {
        mutable.value = mutable.value.copy(apps = Providers.all.associate { it.id to officialApps.availability(it) })
    }

    fun dismissMessage() { mutable.value = mutable.value.copy(message = null) }
    fun message(text: String) { mutable.value = mutable.value.copy(message = text) }

    private fun mutate(success: String? = null, action: suspend () -> Unit) {
        if (mutable.value.busy) return
        mutable.value = mutable.value.copy(busy = true)
        viewModelScope.launch {
            try {
                action()
                if (success != null) message(success)
            } catch (e: CancellationException) { throw e }
            catch (e: IllegalArgumentException) { message(e.message ?: "Confira os dados e tente novamente.") }
            catch (_: Exception) { message("Não foi possível salvar. Seus cadastros anteriores foram preservados.") }
            finally { mutable.value = mutable.value.copy(busy = false) }
        }
    }

    fun startDraft() = mutate { repository.update { it.copy(draft = it.draft ?: CameraDraft()) } }
    fun saveDraft(draft: CameraDraft) = mutate { repository.update { it.copy(draft = draft) } }
    fun discardDraft() = mutate { repository.update { it.copy(draft = null) } }

    fun finishDraft() {
        val provider = mutable.value.hub.draft?.providerId?.let(Providers::find) ?: return
        if (officialApps.availability(provider) != AppAvailability.READY) {
            refreshApps()
            message("Instale e habilite ${provider.name} para concluir o cadastro.")
            return
        }
        mutate("Câmera adicionada ao seu painel.") { repository.update(HubCodec::finishDraft) }
    }

    fun favorite(id: String) = mutate { repository.update { state ->
        state.copy(cameras = state.cameras.map { if (it.id == id) it.copy(favorite = !it.favorite) else it })
    } }

    fun edit(id: String, name: String, location: String) = mutate("Cadastro atualizado.") { repository.update { state ->
        state.copy(cameras = state.cameras.map { if (it.id == id) it.copy(name = name.trim(), location = location.trim()) else it })
    } }

    fun remove(id: String) = mutate("Acesso removido deste painel.") { repository.update { state ->
        state.copy(cameras = state.cameras.filterNot { it.id == id })
    } }

    fun open(provider: CameraProvider) {
        when (officialApps.open(provider)) {
            LaunchResult.OPENED_APP -> Unit
            LaunchResult.MISSING -> { refreshApps(); message("${provider.name} não está instalado. Use Instalar para continuar.") }
            LaunchResult.UNAVAILABLE -> { refreshApps(); message("${provider.name} está desativado ou indisponível. Verifique o aplicativo no Android.") }
            else -> message("Não foi possível abrir ${provider.name}. Tente novamente.")
        }
    }

    fun install(provider: CameraProvider) {
        if (officialApps.install(provider) == LaunchResult.FAILED) {
            message("Não foi possível abrir a loja. Instale ${provider.name} pela Play Store ou pelo canal oficial indicado no manual da câmera e volte aqui.")
        }
    }

    suspend fun readImport(uri: Uri): HubState = withContext(Dispatchers.IO) {
        val text = getApplication<Application>().contentResolver.openInputStream(uri)?.use(HubCodec::readBounded)
            ?: throw IllegalArgumentException("Não foi possível ler o arquivo.")
        try { HubCodec.decode(text).copy(draft = null) }
        catch (_: Exception) { throw IllegalArgumentException("Este arquivo não é um backup compatível do Enterprise Cams.") }
    }

    fun importBackup(backup: HubState) = mutate("Backup importado. Câmeras existentes foram preservadas.") {
        repository.update { HubCodec.merge(it, backup) }
    }

    fun exportBackup(uri: Uri) = mutate("Backup salvo no local escolhido.") {
        val text = HubCodec.encode(mutable.value.hub.copy(draft = null))
        withContext(Dispatchers.IO) {
            val stream = getApplication<Application>().contentResolver.openOutputStream(uri, "wt")
                ?: throw java.io.IOException("No output stream")
            stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
        }
    }
}
