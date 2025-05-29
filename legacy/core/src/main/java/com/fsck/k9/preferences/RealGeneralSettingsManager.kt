@file:Suppress("DEPRECATION")

package com.fsck.k9.preferences

import com.fsck.k9.K9
import com.fsck.k9.Preferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import net.thunderbird.core.preferences.AppTheme
import net.thunderbird.core.preferences.BackgroundSync
import net.thunderbird.core.preferences.GeneralSettings
import net.thunderbird.core.preferences.GeneralSettingsManager
import net.thunderbird.core.preferences.SettingsChangePublisher
import net.thunderbird.core.preferences.Storage
import net.thunderbird.core.preferences.SubTheme
import timber.log.Timber

/**
 * Retrieve and modify general settings.
 *
 * Currently general settings are split between [K9] and [GeneralSettings]. The goal is to move everything over to
 * [GeneralSettings] and get rid of [K9].
 *
 * The [GeneralSettings] instance managed by this class is updated with state from [K9] when [K9.saveSettingsAsync] is
 * called.
 */
internal class RealGeneralSettingsManager(
    private val preferences: Preferences,
    private val coroutineScope: CoroutineScope,
    private val changePublisher: SettingsChangePublisher,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : GeneralSettingsManager {
    private val settingsFlow = MutableSharedFlow<GeneralSettings>(replay = 1)
    private var generalSettings: GeneralSettings? = null

    @Deprecated("This only exists for collaboration with the K9 class")
    val storage: Storage
        get() = preferences.storage

    @Synchronized
    override fun getSettings(): GeneralSettings {
        return generalSettings ?: loadGeneralSettings().also { generalSettings = it }
    }

    override fun getSettingsFlow(): Flow<GeneralSettings> {
        // Make sure to load settings now if they haven't been loaded already. This will also update settingsFlow.
        getSettings()

        return settingsFlow.distinctUntilChanged()
    }

    @Synchronized
    fun loadSettings() {
        K9.loadPrefs(preferences.storage)
        generalSettings = loadGeneralSettings()
    }

    private fun updateSettingsFlow(settings: GeneralSettings) {
        coroutineScope.launch {
            settingsFlow.emit(settings)
        }
    }

    @Deprecated("This only exists for collaboration with the K9 class")
    fun saveSettingsAsync() {
        coroutineScope.launch(backgroundDispatcher) {
            val settings = updateGeneralSettingsWithStateFromK9()
            settingsFlow.emit(settings)

            saveSettings(settings)
        }
    }

    @Synchronized
    private fun updateGeneralSettingsWithStateFromK9(): GeneralSettings {
        return getSettings().copy(
            backgroundSync = K9.backgroundOps.toBackgroundSync(),
        ).also { generalSettings ->
            this.generalSettings = generalSettings
        }
    }

    @Synchronized
    private fun saveSettings(settings: GeneralSettings) {
        val editor = preferences.createStorageEditor()
        K9.save(editor)
        writeSettings(editor, settings)
        editor.commit()

        changePublisher.publish()
    }

    @Synchronized
    private fun GeneralSettings.persist() {
        generalSettings = this
        updateSettingsFlow(this)
        saveSettingsAsync(this)
    }

    private fun saveSettingsAsync(generalSettings: GeneralSettings) {
        coroutineScope.launch(backgroundDispatcher) {
            saveSettings(generalSettings)
        }
    }

    @Synchronized
    override fun setShowRecentChanges(showRecentChanges: Boolean) {
        getSettings().copy(showRecentChanges = showRecentChanges).persist()
    }

    @Synchronized
    override fun setAppTheme(appTheme: AppTheme) {
        getSettings().copy(appTheme = appTheme).persist()
    }

    @Synchronized
    override fun setMessageViewTheme(subTheme: SubTheme) {
        getSettings().copy(messageViewTheme = subTheme).persist()
    }

    @Synchronized
    override fun setMessageComposeTheme(subTheme: SubTheme) {
        getSettings().copy(messageComposeTheme = subTheme).persist()
    }

    @Synchronized
    override fun setFixedMessageViewTheme(fixedMessageViewTheme: Boolean) {
        getSettings().copy(fixedMessageViewTheme = fixedMessageViewTheme).persist()
    }

    private fun writeSettings(editor: StorageEditor, settings: GeneralSettings) {
        editor.putBoolean("showRecentChanges", settings.showRecentChanges)
        editor.putEnum("theme", settings.appTheme)
        editor.putEnum("messageViewTheme", settings.messageViewTheme)
        editor.putEnum("messageComposeTheme", settings.messageComposeTheme)
        editor.putBoolean("fixedMessageViewTheme", settings.fixedMessageViewTheme)
    }

    private fun loadGeneralSettings(): GeneralSettings {
        val storage = preferences.storage

        val settings = GeneralSettings(
            backgroundSync = K9.backgroundOps.toBackgroundSync(),
            showRecentChanges = storage.getBoolean("showRecentChanges", true),
            appTheme = storage.getEnum("theme", AppTheme.FOLLOW_SYSTEM),
            messageViewTheme = storage.getEnum(
                "messageViewTheme",
                SubTheme.USE_GLOBAL,
            ),
            messageComposeTheme = storage.getEnum(
                "messageComposeTheme",
                SubTheme.USE_GLOBAL,
            ),
            fixedMessageViewTheme = storage.getBoolean("fixedMessageViewTheme", true),
        )

        updateSettingsFlow(settings)

        return settings
    }
}

private fun K9.BACKGROUND_OPS.toBackgroundSync(): BackgroundSync {
    return when (this) {
        K9.BACKGROUND_OPS.ALWAYS -> BackgroundSync.ALWAYS
        K9.BACKGROUND_OPS.NEVER -> BackgroundSync.NEVER
        K9.BACKGROUND_OPS.WHEN_CHECKED_AUTO_SYNC -> BackgroundSync.FOLLOW_SYSTEM_AUTO_SYNC
    }
}

private inline fun <reified T : Enum<T>> Storage.getEnum(key: String, defaultValue: T): T {
    return try {
        val value = getStringOrNull(key)
        if (value != null) {
            enumValueOf(value)
        } else {
            defaultValue
        }
    } catch (e: Exception) {
        Timber.e(e, "Couldn't read setting '%s'. Using default value instead.", key)
        defaultValue
    }
}

private fun <T : Enum<T>> StorageEditor.putEnum(key: String, value: T) {
    putString(key, value.name)
}
