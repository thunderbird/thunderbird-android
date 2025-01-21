package app.k9mail.feature.navigation.drawer.domain.usecase

import app.k9mail.legacy.preferences.AppTheme
import app.k9mail.legacy.preferences.GeneralSettings
import app.k9mail.legacy.preferences.GeneralSettingsChangeListener
import app.k9mail.legacy.preferences.GeneralSettingsManager
import app.k9mail.legacy.preferences.SubTheme
import java.util.concurrent.CopyOnWriteArraySet
import kotlinx.coroutines.flow.Flow

internal class FakeGeneralSettingsManager : GeneralSettingsManager {
    private val listeners = CopyOnWriteArraySet<GeneralSettingsChangeListener>()

    override fun getSettings(): GeneralSettings {
        TODO("Not yet implemented")
    }

    override fun getSettingsFlow(): Flow<GeneralSettings> {
        TODO("Not yet implemented")
    }

    override fun setShowRecentChanges(showRecentChanges: Boolean) {
        TODO("Not yet implemented")
    }

    override fun setAppTheme(appTheme: AppTheme) {
        TODO("Not yet implemented")
    }

    override fun setMessageViewTheme(subTheme: SubTheme) {
        TODO("Not yet implemented")
    }

    override fun setMessageComposeTheme(subTheme: SubTheme) {
        TODO("Not yet implemented")
    }

    override fun setFixedMessageViewTheme(fixedMessageViewTheme: Boolean) {
        TODO("Not yet implemented")
    }

    override fun addListener(listener: GeneralSettingsChangeListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: GeneralSettingsChangeListener) {
        listeners.remove(listener)
    }

    fun notifyListeners() {
        for (listener in listeners) {
            listener.onGeneralSettingsChanged()
        }
    }
}
