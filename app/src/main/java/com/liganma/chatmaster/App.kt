package com.liganma.chatmaster

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ven.assists.AssistsCore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

/**
 * APP上下文，主要功能是APP设置
 */

class App(
    private val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : Application() {
    private val dataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create(produceFile = {
            File(filesDir, "settings.preferences_pb")
        })
    }

    override fun onCreate() {
        super.onCreate()
        AssistsCore.init(this)
    }

    val settingsRepository by lazy { AppSettingsRepository(dataStore,applicationScope,this) }
}
class AppSettingsRepository(private val dataStore: DataStore<Preferences>,
                            private val applicationScope: CoroutineScope,
                            private val context: Context) {

    // 使用StateFlow管理状态，确保自动更新
    private val _overlayEnabled = MutableStateFlow(false)
    val overlayEnabled: StateFlow<Boolean> = _overlayEnabled.asStateFlow()

    private val _accessKey = MutableStateFlow("")
    val accessKey: StateFlow<String> = _accessKey.asStateFlow()

    private val _prompt = MutableStateFlow("")
    val prompt: StateFlow<String> = _prompt.asStateFlow()

    init {
        // 从DataStore初始化状态（自动持续监听）
        applicationScope.launch {
            dataStore.data
                .catch { e ->
                    if (e is IOException) {
                        Log.e("SettingsRepo", "Error reading preferences", e)
                        emit(emptyPreferences())
                    } else {
                        throw e
                    }
                }
                .collect { preferences ->
                    _overlayEnabled.value = preferences[OVERLAY_ENABLED] ?: false
                    _accessKey.value = preferences[ACCESS_KEY] ?: ""
                    _prompt.value = preferences[PROMPT] ?: ""
                }
        }
    }

    fun saveOverlayEnabled(value: Boolean) {
        applicationScope.launch {
            dataStore.edit { settings ->
                settings[OVERLAY_ENABLED] = value
            }
            // 注意：不需要手动更新_state，因为dataStore.data.collect会自动触发更新
        }

    }

    fun saveAccessKey(value: String) {
        applicationScope.launch {
            dataStore.edit { settings ->
                settings[ACCESS_KEY] = value
            }
        }
    }

    fun savePrompt(value: String) {
        applicationScope.launch {
            dataStore.edit { settings ->
                settings[PROMPT] = value
            }
        }

    }

    companion object {
        val OVERLAY_ENABLED = booleanPreferencesKey("overlayEnabled")
        val ACCESS_KEY = stringPreferencesKey("accessKey")
        val PROMPT = stringPreferencesKey("prompt")
    }
}