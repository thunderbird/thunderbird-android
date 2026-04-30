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
import net.thunderbird.core.preference.PreferenceChangeBroker
import net.thunderbird.core.preference.PreferenceChangeSubscriber
import net.thunderbird.core.preference.PreferenceScope
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
    preferenceChangeBroker: PreferenceChangeBroker,
) : NotificationPreferenceManager, PreferenceChangeSubscriber  {

    init {
        preferenceChangeBroker.subscribe(this)
    }
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
            key = NotificationSettingKey.NotificationQuickDeleteBehaviour.value,
            default = NOTIFICATION_PREFERENCE_DEFAULT_QUICK_DELETE_BEHAVIOUR,
        )
        val isSummaryDeleteActionEnabled = resolveSummaryDeleteActionEnabled(
            storage = storage,
            quickDeleteBehaviour = notificationQuickDeleteBehaviour,
        )

        return NotificationPreference(
            isQuietTimeEnabled = storage.getBoolean(
                key = NotificationSettingKey.QuietTimeEnabled.value,
                defValue = NOTIFICATION_PREFERENCE_DEFAULT_IS_QUIET_TIME_ENABLED,
            ),
            quietTimeStarts = storage.getStringOrDefault(
                key = NotificationSettingKey.QuietTimeStarts.value,
                defValue = NOTIFICATION_PREFERENCE_DEFAULT_QUIET_TIME_STARTS,
            ),
            quietTimeEnds = storage.getStringOrDefault(
                key = NotificationSettingKey.QuietTimeEnds.value,
                defValue = NOTIFICATION_PREFERENCE_DEFAULT_QUIET_TIME_END,
            ),
            isNotificationDuringQuietTimeEnabled = storage.getBoolean(
                key = NotificationSettingKey.NotificationDuringQuietTimeEnabled.value,
                defValue = NOTIFICATION_PREFERENCE_DEFAULT_IS_NOTIFICATION_DURING_QUIET_TIME_ENABLED,
            ),
            messageActionsOrder = NotificationActionTokens.parseOrder(
                storage.getStringOrDefault(
                    key = NotificationSettingKey.MessageActionsOrder.value,
                    defValue = NotificationActionTokens.serializeOrder(
                        NOTIFICATION_PREFERENCE_DEFAULT_MESSAGE_ACTIONS_ORDER,
                    ),
                ),
            ),
            messageActionsCutoff = storage.getInt(
                key = NotificationSettingKey.MessageActionsCutoff.value,
                defValue = NOTIFICATION_PREFERENCE_DEFAULT_MESSAGE_ACTIONS_CUTOFF,
            ),
            isSummaryDeleteActionEnabled = isSummaryDeleteActionEnabled,
            notificationQuickDeleteBehaviour = notificationQuickDeleteBehaviour,
            lockScreenNotificationVisibility = storage.getEnumOrDefault(
                key = NotificationSettingKey.LockScreenNotificationVisibility.value,
                default = NOTIFICATION_PREFERENCE_DEFAULT_LOCK_SCREEN_NOTIFICATION_VISIBILITY,
            ),
        )
    }

    private fun resolveSummaryDeleteActionEnabled(
        storage: Storage,
        quickDeleteBehaviour: NotificationQuickDelete,
    ): Boolean {
        return if (storage.contains(NotificationSettingKey.IsSummaryDeleteActionEnabled.value)) {
            storage.getBoolean(
                key = NotificationSettingKey.IsSummaryDeleteActionEnabled.value,
                defValue = NOTIFICATION_PREFERENCE_DEFAULT_IS_SUMMARY_DELETE_ACTION_ENABLED,
            )
        } else {
            val derivedValue = quickDeleteBehaviour == NotificationQuickDelete.ALWAYS
            storageEditor.putBoolean(NotificationSettingKey.IsSummaryDeleteActionEnabled.value, derivedValue)
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
                storageEditor.putString(NotificationSettingKey.QuietTimeEnds.value, config.quietTimeEnds)
                storageEditor.putString(NotificationSettingKey.QuietTimeStarts.value, config.quietTimeStarts)
                storageEditor.putBoolean(
                    NotificationSettingKey.QuietTimeEnabled.value,
                    config.isQuietTimeEnabled,
                )
                storageEditor.putBoolean(
                    NotificationSettingKey.NotificationDuringQuietTimeEnabled.value,
                    config.isNotificationDuringQuietTimeEnabled,
                )
                storageEditor.putString(
                    NotificationSettingKey.MessageActionsOrder.value,
                    NotificationActionTokens.serializeOrder(config.messageActionsOrder),
                )
                storageEditor.putInt(NotificationSettingKey.MessageActionsCutoff.value, config.messageActionsCutoff)
                storageEditor.putBoolean(
                    NotificationSettingKey.IsSummaryDeleteActionEnabled.value,
                    config.isSummaryDeleteActionEnabled,
                )
                storageEditor.putEnum(
                    NotificationSettingKey.NotificationQuickDeleteBehaviour.value,
                    config.notificationQuickDeleteBehaviour,
                )
                storageEditor.putEnum(
                    NotificationSettingKey.LockScreenNotificationVisibility.value,
                    config.lockScreenNotificationVisibility,
                )
                storageEditor.commit().also { commited ->
                    logger.verbose(TAG) { "writeConfig: storageEditor.commit() resulted in: $commited" }
                }
            }
            configState.update { config }
        }
    }

    override fun receive(scope: PreferenceScope) {
        if (scope == PreferenceScope.ALL || scope == PreferenceScope.NOTIFICATION) {
            configState.update { getConfigFromStorage(storage) }
        }
    }
}
