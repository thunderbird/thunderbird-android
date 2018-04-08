package com.fsck.k9.widget.unread

import android.content.Context
import com.fsck.k9.helper.UnreadWidgetProperties

class UnreadWidgetRepository(val context: Context) {

    fun saveWidgetProperties(properties: UnreadWidgetProperties) {
        val appWidgetId = properties.appWidgetId
        val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        editor.putString(PREF_PREFIX_KEY + appWidgetId, properties.accountUuid)
        editor.putString(PREF_PREFIX_KEY + appWidgetId + PREF_FOLDER_NAME_SUFFIX_KEY, properties.folderServerId)
        editor.apply()
    }

    fun getWidgetProperties(appWidgetId: Int): UnreadWidgetProperties? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val accountUuid = prefs.getString(PREF_PREFIX_KEY + appWidgetId, null) ?: return null
        val folderServerId = prefs.getString(PREF_PREFIX_KEY + appWidgetId + PREF_FOLDER_NAME_SUFFIX_KEY, null)
        return UnreadWidgetProperties(appWidgetId, accountUuid, folderServerId)
    }

    fun deleteWidgetConfiguration(appWidgetId: Int) {
        val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        editor.remove(PREF_PREFIX_KEY + appWidgetId)
        editor.remove(PREF_PREFIX_KEY + appWidgetId + PREF_FOLDER_NAME_SUFFIX_KEY)
        editor.apply()
    }

    companion object {
        private const val PREFS_NAME = "unread_widget_configuration.xml"

        private const val PREF_PREFIX_KEY = "unread_widget."
        private const val PREF_FOLDER_NAME_SUFFIX_KEY = ".folder_name"
    }
}
