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
import net.thunderbird.core.common.notification.NotificationActionTokens
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.preference.NotificationQuickDelete
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageEditor
import net.thunderbird.core.preference.storage.StoragePersister
import net.thunderbird.core.preference.storage.getEnumOrDefault
import net.thunderbird.core.preference.storage.putEnum

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
    private val initialConfig: NotificationPreference
        get() = getConfigFromStorage(storage)
    private val configState = MutableStateFlow(
        value = initialConfig,
    )

    private fun getConfigFromStorage(storage: Storage): NotificationPreference {
        val notificationQuickDeleteBehaviour = storage.getEnumOrDefault(
            key = KEY_NOTIFICATION_QUICK_DELETE_BEHAVIOUR,
            default = NOTIFICATION_PREFERENCE_DEFAULT_QUICK_DELETE_BEHAVIOUR,
        )
        val isSummaryDeleteActionEnabled = resolveSummaryDeleteActionEnabled(
            storage = storage,
            quickDeleteBehaviour = notificationQuickDeleteBehaviour,
        )

        return NotificationPreference(
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
            messageActionsOrder = NotificationActionTokens.parseOrder(
                storage.getStringOrDefault(
                    key = KEY_MESSAGE_ACTIONS_ORDER,
                    defValue = NotificationActionTokens.serializeOrder(
                        NOTIFICATION_PREFERENCE_DEFAULT_MESSAGE_ACTIONS_ORDER,
                    ),
                ),
            ),
            messageActionsCutoff = storage.getInt(
                key = KEY_MESSAGE_ACTIONS_CUTOFF,
                defValue = NOTIFICATION_PREFERENCE_DEFAULT_MESSAGE_ACTIONS_CUTOFF,
            ),
            isSummaryDeleteActionEnabled = isSummaryDeleteActionEnabled,
            notificationQuickDeleteBehaviour = notificationQuickDeleteBehaviour,
            lockScreenNotificationVisibility = storage.getEnumOrDefault(
                key = KEY_LOCK_SCREEN_NOTIFICATION_VISIBILITY,
                default = NOTIFICATION_PREFERENCE_DEFAULT_LOCK_SCREEN_NOTIFICATION_VISIBILITY,
            ),
        )
    }

    private fun resolveSummaryDeleteActionEnabled(
        storage: Storage,
        quickDeleteBehaviour: NotificationQuickDelete,
    ): Boolean {
        return if (storage.contains(KEY_IS_SUMMARY_DELETE_ACTION_ENABLED)) {
            storage.getBoolean(
                key = KEY_IS_SUMMARY_DELETE_ACTION_ENABLED,
                defValue = NOTIFICATION_PREFERENCE_DEFAULT_IS_SUMMARY_DELETE_ACTION_ENABLED,
            )
        } else {
            val derivedValue = quickDeleteBehaviour == NotificationQuickDelete.ALWAYS
            storageEditor.putBoolean(KEY_IS_SUMMARY_DELETE_ACTION_ENABLED, derivedValue)
            storageEditor.commit()
            derivedValue
        }
    }

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
                storageEditor.putString(
                    KEY_MESSAGE_ACTIONS_ORDER,
                    NotificationActionTokens.serializeOrder(config.messageActionsOrder),
                )
                storageEditor.putInt(KEY_MESSAGE_ACTIONS_CUTOFF, config.messageActionsCutoff)
                storageEditor.putBoolean(KEY_IS_SUMMARY_DELETE_ACTION_ENABLED, config.isSummaryDeleteActionEnabled)
                storageEditor.putEnum(
                    KEY_NOTIFICATION_QUICK_DELETE_BEHAVIOUR,
                    config.notificationQuickDeleteBehaviour,
                )
                storageEditor.putEnum(
                    KEY_LOCK_SCREEN_NOTIFICATION_VISIBILITY,
                    config.lockScreenNotificationVisibility,
                )
                storageEditor.commit().also { commited ->
                    logger.verbose(TAG) { "writeConfig: storageEditor.commit() resulted in: $commited" }
                }
            }
            configState.update { config }
        }
    }
}
