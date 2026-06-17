package net.thunderbird.core.preference.network

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.preference.PreferenceChangeBroker
import net.thunderbird.core.preference.PreferenceChangeSubscriber
import net.thunderbird.core.preference.PreferenceScope
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageEditor
import net.thunderbird.core.preference.storage.StoragePersister
import net.thunderbird.core.preference.storage.getEnumOrDefault
import net.thunderbird.core.preference.storage.putEnum

private const val TAG = "DefaultNetworkSettingsPreferenceManager"

class DefaultNetworkSettingsPreferenceManager(
    private val logger: Logger,
    private val storagePersister: StoragePersister,
    private val storageEditor: StorageEditor,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private var scope: CoroutineScope = CoroutineScope(SupervisorJob()),
    preferenceChangeBroker: PreferenceChangeBroker,
) : NetworkSettingsPreferenceManager, PreferenceChangeSubscriber {

    init {
        preferenceChangeBroker.subscribe(this)
    }
    private val configState: MutableStateFlow<NetworkSettings> = MutableStateFlow(value = loadConfig())
    private val mutex = Mutex()
    private val storage: Storage
        get() = storagePersister.loadValues()

    override fun getConfig(): NetworkSettings = configState.value
    override fun getConfigFlow(): Flow<NetworkSettings> = configState

    override fun save(config: NetworkSettings) {
        logger.debug(TAG) { "save() called with: config = $config" }
        writeConfig(config)
        configState.update { config }
    }

    private fun loadConfig(): NetworkSettings = NetworkSettings(
        backgroundOps = storage.getEnumOrDefault(
            NetworkSettingKey.BackgroundOperations.value,
            NETWORK_SETTINGS_DEFAULT_BACKGROUND_OPS,
        ),
        isProxyEnabled = storage.getBoolean(
            NetworkSettingKey.IsProxyEnabled.value,
            NETWORK_SETTINGS_DEFAULT_IS_PROXY_ENABLED,
        ),
        proxyType = storage.getEnumOrDefault(
            NetworkSettingKey.DefaultProxyType.value,
            NETWORK_SETTINGS_DEFAULT_PROXY_TYPE,
        ),
        proxyHost = storage.getStringOrDefault(
            NetworkSettingKey.DefaultProxyHost.value,
            NETWORK_SETTINGS_DEFAULT_PROXY_HOST,
        ),
        proxyPort = storage.getInt(
            NetworkSettingKey.DefaultProxyPort.value,
            NETWORK_SETTINGS_DEFAULT_PROXY_PORT,
        ),
        proxyDns = storage.getBoolean(
            NetworkSettingKey.DefaultProxyDns.value,
            NETWORK_SETTINGS_DEFAULT_PROXY_DNS,
        ),
        proxyUsername = storage.getStringOrDefault(
            NetworkSettingKey.DefaultProxyUsername.value,
            NETWORK_SETTINGS_DEFAULT_PROXY_USERNAME,
        ),
        proxyPassword = storage.getStringOrDefault(
            NetworkSettingKey.DefaultProxyPassword.value,
            NETWORK_SETTINGS_DEFAULT_PROXY_PASSWORD,
        ),
    )

    private fun writeConfig(config: NetworkSettings) {
        logger.debug(TAG) { "writeConfig() called with: config = $config" }
        scope.launch(ioDispatcher) {
            mutex.withLock {
                storageEditor.putEnum(NetworkSettingKey.BackgroundOperations.value, config.backgroundOps)
                storageEditor.putBoolean(NetworkSettingKey.IsProxyEnabled.value, config.isProxyEnabled)
                storageEditor.putEnum(NetworkSettingKey.DefaultProxyType.value, config.proxyType)
                storageEditor.putString(NetworkSettingKey.DefaultProxyHost.value, config.proxyHost)
                storageEditor.putInt(NetworkSettingKey.DefaultProxyPort.value, config.proxyPort)
                storageEditor.putBoolean(NetworkSettingKey.DefaultProxyDns.value, config.proxyDns)
                storageEditor.putString(NetworkSettingKey.DefaultProxyUsername.value, config.proxyUsername)
                storageEditor.putString(NetworkSettingKey.DefaultProxyPassword.value, config.proxyPassword)
                storageEditor.commit().also { commited ->
                    logger.verbose(TAG) { "writeConfig: storageEditor.commit() resulted in: $commited" }
                }
            }
        }
    }

    override fun receive(scope: PreferenceScope) {
        if (scope == PreferenceScope.ALL || scope == PreferenceScope.NETWORK) {
            configState.update { loadConfig() }
        }
    }
}
