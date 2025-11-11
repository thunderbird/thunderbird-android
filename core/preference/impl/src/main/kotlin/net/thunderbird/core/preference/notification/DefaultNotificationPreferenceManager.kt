package net.thunderbird.core.preference.notification

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
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageEditor

private const val TAG = "DefaultNotificationPreferenceManager"

class DefaultNotificationPreferenceManager(
    private val logger: Logger,
    storage: Storage,
    private val storageEditor: StorageEditor,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private var scope: CoroutineScope = CoroutineScope(SupervisorJob()),
) : NotificationPreferenceManager {
    private val mutex = Mutex()
    private val configState = MutableStateFlow(
        value = NotificationPreference(
            isQuietTimeEnabled = storage.getBoolean(
                key = KEY_QUIET_TIME_ENABLED,
                defValue = NOTIFICATION_PREFERENCE_DEFAULT_IS_QUIET_TIME_ENABLED,
            ),
            quietTimeStarts = storage.getStringOrDefault(
                key = KEY_QUIET_TIME_STARTS,
                defValue = NOTIFICATION_PREFERENCE_DEFAULT_QUIET_TIME_STARTS,
            ),
            quietTimeEnds = storage.getStringOrDefault(
                key = KEY_QUIET_TIME_ENDS,
                defValue = NOTIFICATION_PREFERENCE_DEFAULT_QUIET_TIME_END,
            ),
            isNotificationDuringQuietTimeEnabled = storage.getBoolean(
                key = KEY_NOTIFICATION_DURING_QUIET_TIME_ENABLED,
                defValue = NOTIFICATION_PREFERENCE_DEFAULT_IS_NOTIFICATION_DURING_QUIET_TIME_ENABLED,
            ),
        ),
    )

    override fun getConfig(): NotificationPreference = configState.value
    override fun getConfigFlow(): Flow<NotificationPreference> = configState

    override fun save(config: NotificationPreference) {
        logger.debug(TAG) { "writeConfig() called with: config = $config" }
        scope.launch(ioDispatcher) {
            mutex.withLock {
                storageEditor.putString(KEY_QUIET_TIME_ENDS, config.quietTimeEnds)
                storageEditor.putString(KEY_QUIET_TIME_STARTS, config.quietTimeStarts)
                storageEditor.putBoolean(
                    KEY_QUIET_TIME_ENABLED,
                    config.isQuietTimeEnabled,
                )
                storageEditor.putBoolean(
                    KEY_NOTIFICATION_DURING_QUIET_TIME_ENABLED,
                    config.isNotificationDuringQuietTimeEnabled,
                )
                storageEditor.commit().also { commited ->
                    logger.verbose(TAG) { "writeConfig: storageEditor.commit() resulted in: $commited" }
                }
            }
            configState.update { config }
        }
    }
}
