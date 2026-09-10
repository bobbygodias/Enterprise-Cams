package org.enterprisecams.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.util.UUID

data class CameraProvider(val id: String, val name: String, val packageName: String) {
    val storeUrl: String get() = "https://play.google.com/store/apps/details?id=$packageName"
}

object Providers {
    // Identities verified against the publishers' Play listings on 2026-09-10.
    // No vendor-specific device URI has yet been verified. Never fabricate one.
    val all = listOf(
        CameraProvider("yoosee", "Yoosee", "com.yoosee"),
        CameraProvider("icsee", "iCSee", "com.xm.csee"),
        CameraProvider("v380", "V380", "com.macrovideo.v380"),
        CameraProvider("v380pro", "V380 Pro", "com.macrovideo.v380pro"),
        CameraProvider("hilevel", "Hilevel", "com.sotaviz.hilevelen"),
    )
    fun find(id: String): CameraProvider? = all.find { it.id == id }
}

@Serializable
data class CameraEntry(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val location: String = "",
    val providerId: String,
    val favorite: Boolean = false,
)

@Serializable
data class CameraDraft(
    val name: String = "",
    val location: String = "",
    val providerId: String = "yoosee",
    val setupStarted: Boolean = false,
)

@Serializable
data class HubState(
    val schemaVersion: Int = 1,
    val cameras: List<CameraEntry> = emptyList(),
    val draft: CameraDraft? = null,
)

object HubCodec {
    const val MAX_BYTES = 262_144
    const val MAX_CAMERAS = 500
    const val MAX_TEXT = 80
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = false }

    fun encode(state: HubState): String {
        validate(state)
        return json.encodeToString(state).also {
            require(it.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Cadastro grande demais." }
        }
    }

    fun decode(text: String): HubState {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Arquivo grande demais." }
        return json.decodeFromString<HubState>(text).also(::validate)
    }

    fun validate(state: HubState) {
        require(state.schemaVersion == 1) { "Versão de arquivo não compatível." }
        require(state.cameras.size <= MAX_CAMERAS) { "O limite é de 500 câmeras." }
        require(state.cameras.map { it.id }.toSet().size == state.cameras.size) { "Identificadores repetidos." }
        state.cameras.forEach {
            require(runCatching { UUID.fromString(it.id).toString() == it.id }.getOrDefault(false)) { "Identificador inválido." }
            checkText(it.name, required = true)
            checkText(it.location)
            require(Providers.find(it.providerId) != null) { "Aplicativo de câmera desconhecido." }
        }
        state.draft?.let {
            checkText(it.name)
            checkText(it.location)
            require(Providers.find(it.providerId) != null) { "Aplicativo de câmera desconhecido." }
            if (it.setupStarted) checkText(it.name, required = true)
        }
    }

    private fun checkText(value: String, required: Boolean = false) {
        require(value.length <= MAX_TEXT && value.none { it.isISOControl() }) { "Nome ou local inválido." }
        require(!required || value.isNotBlank()) { "Informe o nome da câmera." }
    }

    fun readBounded(input: InputStream): String {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            require(output.size() + count <= MAX_BYTES) { "Arquivo grande demais." }
            output.write(buffer, 0, count)
        }
        val decoder = Charsets.UTF_8.newDecoder()
        return decoder.decode(java.nio.ByteBuffer.wrap(output.toByteArray())).toString()
    }

    // Restore is additive. Conflicting IDs with changed contents receive a new ID.
    // Importing the same backup twice doesn't multiply matching records.
    fun merge(current: HubState, backup: HubState): HubState {
        validate(backup)
        val merged = current.cameras.toMutableList()
        for (incoming in backup.cameras) {
            val same = merged.any {
                it.name == incoming.name && it.location == incoming.location && it.providerId == incoming.providerId
            }
            if (same) continue
            merged.add(if (merged.any { it.id == incoming.id }) incoming.copy(id = UUID.randomUUID().toString()) else incoming)
        }
        return current.copy(cameras = merged).also(::validate)
    }

    fun finishDraft(state: HubState): HubState {
        val draft = requireNotNull(state.draft) { "Cadastro não encontrado." }
        require(draft.setupStarted) { "Complete o cadastro primeiro." }
        val name = draft.name.trim()
        val location = draft.location.trim()
        val duplicate = state.cameras.any { it.name == name && it.location == location && it.providerId == draft.providerId }
        require(!duplicate) { "Esta câmera já está no painel. Use outro nome se for uma câmera diferente." }
        return state.copy(
            cameras = state.cameras + CameraEntry(name = name, location = location, providerId = draft.providerId),
            draft = null,
        ).also(::validate)
    }
}
