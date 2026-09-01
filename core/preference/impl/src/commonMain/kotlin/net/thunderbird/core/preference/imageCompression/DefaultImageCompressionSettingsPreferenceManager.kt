package net.thunderbird.core.preference.imageCompression

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
import net.thunderbird.components.core.logging.Logger
import net.thunderbird.core.preference.PreferenceChangeBroker
import net.thunderbird.core.preference.PreferenceChangeSubscriber
import net.thunderbird.core.preference.PreferenceScope
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageProvider

private const val TAG = "DefaultImageCompressionSettingsPreferenceManager"

class DefaultImageCompressionSettingsPreferenceManager(
    private val logger: Logger,
    private val storageProvider: StorageProvider,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private var scope: CoroutineScope = CoroutineScope(SupervisorJob()),
    preferenceChangeBroker: PreferenceChangeBroker,
) : ImageCompressionSettingsPreferenceManager, PreferenceChangeSubscriber {
    init {
        preferenceChangeBroker.subscribe(this)
    }

    private val configState = MutableStateFlow(loadConfig())
    private val mutex = Mutex()
    private val storage: Storage
        get() = storageProvider.loadLatestStorage()

    override fun getConfig(): ImageCompressionSettings = configState.value

    override fun getConfigFlow(): Flow<ImageCompressionSettings> = configState

    override fun save(config: ImageCompressionSettings) {
        logger.debug(TAG) { "save() called" }
        writeConfig(config)
        configState.update { config }
    }

    private fun loadConfig() = ImageCompressionSettings(
        jpegQuality = storage.getInt(
            ImageCompressionSettingKey.JpegQuality.value,
            IMAGE_COMPRESSION_SETTINGS_DEFAULT_JPEG_QUALITY,
        ).coerceIn(1, 100),
        longestEdge = storage.getInt(
            ImageCompressionSettingKey.LongestEdge.value,
            IMAGE_COMPRESSION_SETTINGS_DEFAULT_LONGEST_EDGE,
        ).coerceAtLeast(1),
    )

    private fun writeConfig(config: ImageCompressionSettings) {
        scope.launch(ioDispatcher) {
            mutex.withLock {
                val storageEditor = storageProvider.createStorageEditor()
                storageEditor.putInt(ImageCompressionSettingKey.JpegQuality.value, config.jpegQuality.coerceIn(1, 100))
                storageEditor.putInt(ImageCompressionSettingKey.LongestEdge.value, config.longestEdge.coerceAtLeast(1))
                storageEditor.commit().also { committed ->
                    logger.verbose(TAG) { "writeConfig: storageEditor.commit() resulted in: $committed" }
                }
            }
        }
    }

    override fun receive(scope: PreferenceScope) {
        if (scope == PreferenceScope.ALL || scope == PreferenceScope.IMAGE_COMPRESSION) {
            configState.update { loadConfig() }
        }
    }
}
