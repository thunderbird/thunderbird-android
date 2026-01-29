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
import net.thunderbird.core.preference.storage.StoragePersister

private const val TAG = "DefaultNotificationPreferenceManager"

class DefaultNotificationPreferenceManager(
    private val logger: Logger,
    private val storagePersister: StoragePersister,
    private val storageEditor: StorageEditor,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private var scope: CoroutineScope = CoroutineScope(SupervisorJob()),
) : NotificationPreferenceManager {
    private val mutex = Mutex()
    private val storage: Storage
        get() = storagePersister.loadValues()
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
            messageActionsOrder = storage.getStringOrDefault(
                key = KEY_MESSAGE_ACTIONS_ORDER,
                defValue = NOTIFICATION_PREFERENCE_DEFAULT_MESSAGE_ACTIONS_ORDER,
            ),
            messageActionsCutoff = storage.getInt(
                key = KEY_MESSAGE_ACTIONS_CUTOFF,
                defValue = NOTIFICATION_PREFERENCE_DEFAULT_MESSAGE_ACTIONS_CUTOFF,
            ),
            isSummaryDeleteActionEnabled = storage.getBoolean(
                key = KEY_IS_SUMMARY_DELETE_ACTION_ENABLED,
                defValue = NOTIFICATION_PREFERENCE_DEFAULT_IS_SUMMARY_DELETE_ACTION_ENABLED,
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
                storageEditor.putString(KEY_MESSAGE_ACTIONS_ORDER, config.messageActionsOrder)
                storageEditor.putInt(KEY_MESSAGE_ACTIONS_CUTOFF, config.messageActionsCutoff)
                storageEditor.putBoolean(KEY_IS_SUMMARY_DELETE_ACTION_ENABLED, config.isSummaryDeleteActionEnabled)
                storageEditor.commit().also { commited ->
                    logger.verbose(TAG) { "writeConfig: storageEditor.commit() resulted in: $commited" }
                }
            }
            configState.update { config }
        }
    }
}
