package com.fsck.k9.ui.settings.general

import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceDataStore
import com.fsck.k9.K9
import com.fsck.k9.K9.Theme
import com.fsck.k9.Preferences
import com.fsck.k9.job.K9JobManager
import java.util.concurrent.ExecutorService

class GeneralSettingsDataStore(
        private val preferences: Preferences,
        private val jobManager: K9JobManager,
        private val executorService: ExecutorService
) : PreferenceDataStore() {
    var activity: FragmentActivity? = null

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        return when (key) {
            "fixed_message_view_theme" -> K9.useFixedMessageViewTheme()
            "animations" -> K9.showAnimations()
            "measure_accounts" -> K9.measureAccounts()
            "count_search" -> K9.countSearchMessages()
            "hide_special_accounts" -> K9.isHideSpecialAccounts()
            "folderlist_wrap_folder_name" -> K9.wrapFolderNames()
            "messagelist_stars" -> K9.messageListStars()
            "messagelist_checkboxes" -> K9.messageListCheckboxes()
            "messagelist_show_correspondent_names" -> K9.showCorrespondentNames()
            "messagelist_sender_above_subject" -> K9.messageListSenderAboveSubject()
            "messagelist_show_contact_name" -> K9.showContactName()
            "messagelist_change_contact_name_color" -> K9.changeContactNameColor()
            "messagelist_show_contact_picture" -> K9.showContactPicture()
            "messagelist_colorize_missing_contact_pictures" -> K9.isColorizeMissingContactPictures()
            "messagelist_background_as_unread_indicator" -> K9.useBackgroundAsUnreadIndicator()
            "threaded_view" -> K9.isThreadedViewEnabled()
            "messageview_fixedwidth_font" -> K9.messageViewFixedWidthFont()
            "messageview_autofit_width" -> K9.autofitWidth()
            "start_integrated_inbox" -> K9.startIntegratedInbox()
            "gestures" -> K9.gesturesEnabled()
            "messageview_return_to_list" -> K9.messageViewReturnToList()
            "messageview_show_next" -> K9.messageViewShowNext()
            "quiet_time_enabled" -> K9.getQuietTimeEnabled()
            "disable_notifications_during_quiet_time" -> !K9.isNotificationDuringQuietTimeEnabled()
            "privacy_hide_useragent" -> K9.hideUserAgent()
            "privacy_hide_timezone" -> K9.hideTimeZone()
            "debug_logging" -> K9.isDebug()
            "sensitive_logging" -> K9.DEBUG_SENSITIVE
            else -> defValue
        }
    }

    override fun putBoolean(key: String, value: Boolean) {
        when (key) {
            "fixed_message_view_theme" -> K9.setUseFixedMessageViewTheme(value)
            "animations" -> K9.setAnimations(value)
            "measure_accounts" -> K9.setMeasureAccounts(value)
            "count_search" -> K9.setCountSearchMessages(value)
            "hide_special_accounts" -> K9.setHideSpecialAccounts(value)
            "folderlist_wrap_folder_name" -> K9.setWrapFolderNames(value)
            "messagelist_stars" -> K9.setMessageListStars(value)
            "messagelist_checkboxes" -> K9.setMessageListCheckboxes(value)
            "messagelist_show_correspondent_names" -> K9.setShowCorrespondentNames(value)
            "messagelist_sender_above_subject" -> K9.setMessageListSenderAboveSubject(value)
            "messagelist_show_contact_name" -> K9.setShowContactName(value)
            "messagelist_change_contact_name_color" -> K9.setChangeContactNameColor(value)
            "messagelist_show_contact_picture" -> K9.setShowContactPicture(value)
            "messagelist_colorize_missing_contact_pictures" -> K9.setColorizeMissingContactPictures(value)
            "messagelist_background_as_unread_indicator" -> K9.setUseBackgroundAsUnreadIndicator(value)
            "threaded_view" -> K9.setThreadedViewEnabled(value)
            "messageview_fixedwidth_font" -> K9.setMessageViewFixedWidthFont(value)
            "messageview_autofit_width" -> K9.setAutofitWidth(value)
            "start_integrated_inbox" -> K9.setStartIntegratedInbox(value)
            "gestures" -> K9.setGesturesEnabled(value)
            "messageview_return_to_list" -> K9.setMessageViewReturnToList(value)
            "messageview_show_next" -> K9.setMessageViewShowNext(value)
            "quiet_time_enabled" -> K9.setQuietTimeEnabled(value)
            "disable_notifications_during_quiet_time" -> K9.setNotificationDuringQuietTimeEnabled(!value)
            "privacy_hide_useragent" -> K9.setHideUserAgent(value)
            "privacy_hide_timezone" -> K9.setHideTimeZone(value)
            "debug_logging" -> K9.setDebug(value)
            "sensitive_logging" -> K9.DEBUG_SENSITIVE = value
            else -> return
        }

        saveSettings()
    }

    override fun getInt(key: String?, defValue: Int): Int {
        return when (key) {
            "messagelist_contact_name_color" -> K9.getContactNameColor()
            else -> defValue
        }
    }

    override fun putInt(key: String?, value: Int) {
        when (key) {
            "messagelist_contact_name_color" -> K9.setContactNameColor(value)
            else -> return
        }

        saveSettings()
    }

    override fun getString(key: String, defValue: String?): String? {
        return when (key) {
            "language" -> K9.getK9Language()
            "theme" -> themeToString(K9.getK9Theme())
            "fixed_message_view_theme" -> themeToString(K9.getK9MessageViewThemeSetting())
            "message_compose_theme" -> themeToString(K9.getK9ComposerThemeSetting())
            "messageViewTheme" -> themeToString(K9.getK9MessageViewThemeSetting())
            "messagelist_preview_lines" -> K9.messageListPreviewLines().toString()
            "splitview_mode" -> K9.getSplitViewMode().name
            "notification_quick_delete" -> K9.getNotificationQuickDeleteBehaviour().name
            "lock_screen_notification_visibility" -> K9.getLockScreenNotificationVisibility().name
            "background_ops" -> K9.getBackgroundOps().name
            "notification_hide_subject" -> K9.getNotificationHideSubject().name
            "quiet_time_starts" -> K9.getQuietTimeStarts()
            "quiet_time_ends" -> K9.getQuietTimeEnds()
            else -> defValue
        }
    }

    override fun putString(key: String, value: String?) {
        if (value == null) return

        when (key) {
            "language" -> setLanguage(value)
            "theme" -> setTheme(value)
            "fixed_message_view_theme" -> K9.setK9MessageViewThemeSetting(stringToTheme(value))
            "message_compose_theme" -> K9.setK9ComposerThemeSetting(stringToTheme(value))
            "messageViewTheme" -> K9.setK9MessageViewThemeSetting(stringToTheme(value))
            "messagelist_preview_lines" -> K9.setMessageListPreviewLines(value.toInt())
            "splitview_mode" -> K9.setSplitViewMode(K9.SplitViewMode.valueOf(value))
            "notification_quick_delete" -> K9.setNotificationQuickDeleteBehaviour(K9.NotificationQuickDelete.valueOf(value))
            "lock_screen_notification_visibility" -> K9.setLockScreenNotificationVisibility(K9.LockScreenNotificationVisibility.valueOf(value))
            "background_ops" -> setBackgroundOps(value)
            "notification_hide_subject" -> K9.setNotificationHideSubject(K9.NotificationHideSubject.valueOf(value))
            "quiet_time_starts" -> K9.setQuietTimeStarts(value)
            "quiet_time_ends" -> K9.setQuietTimeEnds(value)
            else -> return
        }

        saveSettings()
    }

    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? {
        return when (key) {
            "confirm_actions" -> {
                mutableSetOf<String>().apply {
                    if (K9.confirmDelete()) add("delete")
                    if (K9.confirmDeleteStarred()) add("delete_starred")
                    if (K9.confirmDeleteFromNotification()) add("delete_notif")
                    if (K9.confirmSpam()) add("spam")
                    if (K9.confirmDiscardMessage()) add("discard")
                    if (K9.confirmMarkAllRead()) add("mark_all_read")
                }
            }
            "messageview_visible_refile_actions" -> {
                mutableSetOf<String>().apply {
                    if (K9.isMessageViewDeleteActionVisible()) add("delete")
                    if (K9.isMessageViewArchiveActionVisible()) add("archive")
                    if (K9.isMessageViewMoveActionVisible()) add("move")
                    if (K9.isMessageViewCopyActionVisible()) add("copy")
                    if (K9.isMessageViewSpamActionVisible()) add("spam")
                }
            }
            "volume_navigation" -> {
                mutableSetOf<String>().apply {
                    if (K9.useVolumeKeysForNavigationEnabled()) add("message")
                    if (K9.useVolumeKeysForListNavigationEnabled()) add("list")
                }
            }
            else -> defValues
        }
    }

    override fun putStringSet(key: String, values: MutableSet<String>?) {
        val checkedValues = values ?: emptySet<String>()
        when (key) {
            "confirm_actions" -> {
                K9.setConfirmDelete("delete" in checkedValues)
                K9.setConfirmDeleteStarred("delete_starred" in checkedValues)
                K9.setConfirmDeleteFromNotification("delete_notif" in checkedValues)
                K9.setConfirmSpam("spam" in checkedValues)
                K9.setConfirmDiscardMessage("discard" in checkedValues)
                K9.setConfirmMarkAllRead("mark_all_read" in checkedValues)
            }
            "messageview_visible_refile_actions" -> {
                K9.setMessageViewDeleteActionVisible("delete" in checkedValues)
                K9.setMessageViewArchiveActionVisible("archive" in checkedValues)
                K9.setMessageViewMoveActionVisible("move" in checkedValues)
                K9.setMessageViewCopyActionVisible("copy" in checkedValues)
                K9.setMessageViewSpamActionVisible("spam" in checkedValues)
            }
            "volume_navigation" -> {
                K9.setUseVolumeKeysForNavigation("message" in checkedValues)
                K9.setUseVolumeKeysForListNavigation("list" in checkedValues)
            }
            else -> return
        }

        saveSettings()
    }

    private fun saveSettings() {
        val editor = preferences.createStorageEditor()
        K9.save(editor)

        executorService.execute {
            editor.commit()
        }
    }

    private fun setTheme(value: String?) {
        K9.setK9Theme(stringToTheme(value))
        recreateActivity()
    }

    private fun setLanguage(language: String?) {
        K9.setK9Language(language)
        recreateActivity()
    }

    private fun themeToString(theme: Theme) = when (theme) {
        Theme.LIGHT -> "light"
        Theme.DARK -> "dark"
        Theme.USE_GLOBAL -> "global"
    }

    private fun stringToTheme(theme: String?) = when (theme) {
        "light" -> Theme.LIGHT
        "dark" -> Theme.DARK
        "global" -> Theme.USE_GLOBAL
        else -> throw AssertionError()
    }

    private fun setBackgroundOps(value: String) {
        val newBackgroundOps = K9.BACKGROUND_OPS.valueOf(value)
        if (newBackgroundOps != K9.getBackgroundOps()) {
            K9.setBackgroundOps(value)
            jobManager.scheduleAllMailJobs()
        }
    }

    private fun recreateActivity() {
        activity?.recreate()
    }
}
