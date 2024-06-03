package app.k9mail.feature.widget.unread

import android.content.Context
import android.content.SharedPreferences

internal class UnreadWidgetRepository(
    private val context: Context,
    private val dataRetriever: UnreadWidgetDataProvider,
    private val migrations: UnreadWidgetMigrations,
) {

    fun saveWidgetConfiguration(configuration: UnreadWidgetConfiguration) {
        val appWidgetId = configuration.appWidgetId
        val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        editor.putString(PREF_PREFIX_KEY + appWidgetId, configuration.accountUuid)
        editor.putString(PREF_PREFIX_KEY + appWidgetId + PREF_FOLDER_ID_SUFFIX_KEY, configuration.folderId?.toString())
        editor.apply()
    }

    fun getWidgetData(appWidgetId: Int): UnreadWidgetData? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val version = prefs.getInt(PREF_VERSION_KEY, 1)
        if (version != PREFS_VERSION) {
            upgradePreferences(version, prefs)
        }

        val accountUuid = prefs.getString(PREF_PREFIX_KEY + appWidgetId, null) ?: return null
        val folderId = prefs.getString(PREF_PREFIX_KEY + appWidgetId + PREF_FOLDER_ID_SUFFIX_KEY, null)?.toLongOrNull()

        val configuration = UnreadWidgetConfiguration(appWidgetId, accountUuid, folderId)

        return dataRetriever.loadUnreadWidgetData(configuration)
    }

    private fun upgradePreferences(version: Int, preferences: SharedPreferences) {
        if (version > PREFS_VERSION) {
            error("UnreadWidgetRepository: Version downgrades are not supported")
        } else {
            migrations.upgradePreferences(preferences, version)
        }
    }

    fun deleteWidgetConfiguration(appWidgetId: Int) {
        val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        editor.remove(PREF_PREFIX_KEY + appWidgetId)
        editor.remove(PREF_PREFIX_KEY + appWidgetId + PREF_FOLDER_ID_SUFFIX_KEY)
        editor.apply()
    }

    companion object {
        internal const val PREFS_VERSION = 2
        internal const val PREF_VERSION_KEY = "version"

        private const val PREFS_NAME = "unread_widget_configuration.xml"

        private const val PREF_PREFIX_KEY = "unread_widget."
        private const val PREF_FOLDER_ID_SUFFIX_KEY = ".folder_id"
    }
}

data class UnreadWidgetConfiguration(val appWidgetId: Int, val accountUuid: String, val folderId: Long?)
