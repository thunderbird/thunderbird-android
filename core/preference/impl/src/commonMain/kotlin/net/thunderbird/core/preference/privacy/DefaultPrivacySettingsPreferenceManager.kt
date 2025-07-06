package net.thunderbird.core.preference.privacy

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.thunderbird.core.preference.PreferenceChangeBroker
import net.thunderbird.core.preference.PreferenceChangeSubscriber
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageEditor

class DefaultPrivacySettingsPreferenceManager(
    private val storage: Storage,
    private val storageEditor: StorageEditor,
    private val changeBroker: PreferenceChangeBroker,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private var scope: CoroutineScope = CoroutineScope(SupervisorJob()),
) : PrivacySettingsPreferenceManager {
    private val configState: MutableStateFlow<PrivacySettings> = MutableStateFlow(value = loadConfig())
    private val mutex = Mutex()

    init {
        asSettingsFlow()
            .onEach { config ->
                configState.update { config }
            }
            .launchIn(scope)
    }

    override fun save(config: PrivacySettings) {
        configState.update { config }
        writeConfig(config)
    }

    override fun getConfig(): PrivacySettings = configState.value
    override fun getConfigFlow(): Flow<PrivacySettings> = configState

    // Not 100% sure if this method is really required, but applied the same logic
    // present in the RealDrawerConfigManager implementation
    private fun asSettingsFlow(): Flow<PrivacySettings> {
        return callbackFlow {
            send(loadConfig())

            val subscriber = PreferenceChangeSubscriber {
                configState.update { loadConfig() }
            }

            changeBroker.subscribe(subscriber)

            awaitClose {
                changeBroker.unsubscribe(subscriber)
            }
        }
    }

    private fun loadConfig(): PrivacySettings = PrivacySettings(
        isHideTimeZone = storage.getBoolean(KEY_HIDE_TIME_ZONE, false),
    )

    private fun writeConfig(config: PrivacySettings) {
        scope.launch(ioDispatcher) {
            mutex.withLock {
                storageEditor.putBoolean(KEY_HIDE_TIME_ZONE, config.isHideTimeZone)
                storageEditor.commit()
            }
        }
    }

    companion object {
        private const val KEY_HIDE_TIME_ZONE = "hideTimeZone"
    }
}
