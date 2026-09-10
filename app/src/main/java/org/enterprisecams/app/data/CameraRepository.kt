package org.enterprisecams.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.cameraStore by preferencesDataStore(name = "enterprise_cams")

class CameraRepository(context: Context) {
    private val store = context.applicationContext.cameraStore
    private val key = stringPreferencesKey("hub_state_v1")
    val states: Flow<HubState> = store.data.map { prefs ->
        prefs[key]?.let(HubCodec::decode) ?: HubState()
    }

    // DataStore serializes all read/modify/write transactions. Corrupt data is
    // surfaced to the UI, never silently replaced with an empty camera list.
    suspend fun update(transform: (HubState) -> HubState) {
        store.edit { prefs ->
            val current = prefs[key]?.let(HubCodec::decode) ?: HubState()
            prefs[key] = HubCodec.encode(transform(current))
        }
    }
}
