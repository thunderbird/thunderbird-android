package com.fsck.k9.ui.settings.general

import androidx.preference.PreferenceDataStore
import app.k9mail.feature.telemetry.api.TelemetryManager
import com.fsck.k9.K9
import com.fsck.k9.K9.PostMarkAsUnreadNavigation
import com.fsck.k9.K9.PostRemoveNavigation
import com.fsck.k9.UiDensity
import com.fsck.k9.job.K9JobManager
import com.fsck.k9.ui.base.AppLanguageManager
import net.thunderbird.core.common.action.SwipeAction
import net.thunderbird.core.preference.AppTheme
import net.thunderbird.core.preference.BackgroundOps
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.preference.SplitViewMode
import net.thunderbird.core.preference.SubTheme
import net.thunderbird.core.preference.update

@Suppress("LargeClass")
class GeneralSettingsDataStore(
    private val jobManager: K9JobManager,
    private val appLanguageManager: AppLanguageManager,
    private val generalSettingsManager: GeneralSettingsManager,
    private val telemetryManager: TelemetryManager,
) : PreferenceDataStore() {

    private var skipSaveSettings = false

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        return when (key) {
            "fixed_message_view_theme" -> generalSettingsManager.getConfig().display.coreSettings.fixedMessageViewTheme
            "animations" -> generalSettingsManager.getConfig().display.visualSettings.isShowAnimations
            "show_unified_inbox" -> generalSettingsManager.getConfig().display.inboxSettings.isShowUnifiedInbox
            "show_starred_count" -> generalSettingsManager.getConfig().display.inboxSettings.isShowStarredCount
            "messagelist_stars" -> generalSettingsManager.getConfig().display.inboxSettings.isShowMessageListStars
            "messagelist_show_correspondent_names" -> generalSettingsManager.getConfig()
                .display.visualSettings.isShowCorrespondentNames

            "messagelist_sender_above_subject" -> generalSettingsManager.getConfig()
                .display.inboxSettings.isMessageListSenderAboveSubject

            "messagelist_show_contact_name" -> generalSettingsManager.getConfig()
                .display.visualSettings.isShowContactName

            "messagelist_change_contact_name_color" -> generalSettingsManager.getConfig()
                .display.visualSettings.isChangeContactNameColor

            "messagelist_show_contact_picture" -> generalSettingsManager.getConfig()
                .display.visualSettings.isShowContactPicture

            "messagelist_colorize_missing_contact_pictures" -> generalSettingsManager.getConfig()
                .display.visualSettings.isColorizeMissingContactPictures

            "messagelist_background_as_unread_indicator" -> generalSettingsManager.getConfig()
                .display.visualSettings.isUseBackgroundAsUnreadIndicator

            "show_compose_button" -> generalSettingsManager.getConfig()
                .display.inboxSettings.isShowComposeButtonOnMessageList

            "threaded_view" -> generalSettingsManager.getConfig()
                .display.inboxSettings.isThreadedViewEnabled

            "messageview_fixedwidth_font" -> generalSettingsManager.getConfig()
                .display.visualSettings.isUseMessageViewFixedWidthFont

            "messageview_autofit_width" -> generalSettingsManager.getConfig()
                .display.visualSettings.isAutoFitWidth

            "quiet_time_enabled" -> generalSettingsManager.getConfig()
                .notification.isQuietTimeEnabled

            "disable_notifications_during_quiet_time" -> !K9.isNotificationDuringQuietTimeEnabled
            "privacy_hide_useragent" -> generalSettingsManager.getConfig().privacy.isHideUserAgent
            "privacy_hide_timezone" -> generalSettingsManager.getConfig().privacy.isHideTimeZone
            "debug_logging" -> generalSettingsManager.getConfig().debugging.isDebugLoggingEnabled
            "sync_debug_logging" -> generalSettingsManager.getConfig().debugging.isSyncLoggingEnabled
            "sensitive_logging" -> generalSettingsManager.getConfig().debugging.isSensitiveLoggingEnabled
            "volume_navigation" -> K9.isUseVolumeKeysForNavigation
            "enable_telemetry" -> K9.isTelemetryEnabled
            else -> defValue
        }
    }

    override fun putBoolean(key: String, value: Boolean) {
        when (key) {
            "fixed_message_view_theme" -> setFixedMessageViewTheme(value)
            "animations" -> setIsShowAnimations(isShowAnimations = value)
            "show_unified_inbox" -> setIsShowUnifiedInbox(value)
            "show_starred_count" -> setIsShowStarredCount(isShowStarredCount = value)
            "messagelist_stars" -> setIsShowMessageListStars(isShowMessageListStars = value)
            "messagelist_show_correspondent_names" -> setIsShowCorrespondentNames(isShowCorrespondentNames = value)
            "messagelist_sender_above_subject" -> setIsMessageListSenderAboveSubject(
                isMessageListSenderAboveSubject = value,
            )

            "messagelist_show_contact_name" -> setIsShowContactName(isShowContactName = value)
            "messagelist_change_contact_name_color" -> setIsChangeContactNameColor(isChangeContactNameColor = value)
            "messagelist_show_contact_picture" -> setIsShowContactPicture(isShowContactPicture = value)
            "messagelist_colorize_missing_contact_pictures" -> setIsColorizeMissingContactPictures(
                isColorizeMissingContactPictures = value,
            )

            "messagelist_background_as_unread_indicator" -> setIsUseBackgroundAsUnreadIndicator(
                isUseBackgroundAsUnreadIndicator = value,
            )

            "show_compose_button" -> setIsShowComposeButtonOnMessageList(isShowComposeButtonOnMessageList = value)
            "threaded_view" -> setIsThreadedViewEnabled(isThreadedViewEnabled = value)
            "messageview_fixedwidth_font" -> setIsUseMessageViewFixedWidthFont(isUseMessageViewFixedWidthFont = value)
            "messageview_autofit_width" -> setIsAutoFitWidth(isAutoFitWidth = value)
            "quiet_time_enabled" -> setIsQuietTimeEnabled(isQuietTimeEnabled = value)
            "disable_notifications_during_quiet_time" -> K9.isNotificationDuringQuietTimeEnabled = !value
            "privacy_hide_useragent" -> setIsHideUserAgent(isHideUserAgent = value)
            "privacy_hide_timezone" -> setIsHideTimeZone(isHideTimeZone = value)
            "debug_logging" -> setIsDebugLoggingEnabled(isDebugLoggingEnabled = value)
            "sync_debug_logging" -> setIsSyncLoggingEnabled(isSyncLoggingEnabled = value)
            "sensitive_logging" -> setIsSensitiveLoggingEnabled(isSensitiveLoggingEnabled = value)
            "volume_navigation" -> K9.isUseVolumeKeysForNavigation = value
            "enable_telemetry" -> setTelemetryEnabled(value)
            else -> return
        }

        saveSettings()
    }

    override fun getInt(key: String?, defValue: Int): Int {
        return when (key) {
            "messagelist_contact_name_color" -> K9.contactNameColor
            "message_view_content_font_slider" -> K9.fontSizes.messageViewContentAsPercent
            else -> defValue
        }
    }

    override fun putInt(key: String?, value: Int) {
        when (key) {
            "messagelist_contact_name_color" -> K9.contactNameColor = value
            "message_view_content_font_slider" -> K9.fontSizes.messageViewContentAsPercent = value
            else -> return
        }

        saveSettings()
    }

    override fun getString(key: String, defValue: String?): String? {
        return when (key) {
            "language" -> appLanguageManager.getAppLanguage()
            "theme" -> appThemeToString(generalSettingsManager.getConfig().display.coreSettings.appTheme)
            "message_compose_theme" -> subThemeToString(
                generalSettingsManager.getConfig().display.coreSettings.messageComposeTheme,
            )

            "messageViewTheme" -> subThemeToString(
                generalSettingsManager.getConfig().display.coreSettings.messageViewTheme,
            )

            "messagelist_preview_lines" -> K9.messageListPreviewLines.toString()
            "splitview_mode" -> generalSettingsManager.getConfig().display.coreSettings.splitViewMode.name
            "notification_quick_delete" -> K9.notificationQuickDeleteBehaviour.name
            "lock_screen_notification_visibility" -> K9.lockScreenNotificationVisibility.name
            "background_ops" -> generalSettingsManager.getConfig().network.backgroundOps.name
            "quiet_time_starts" -> generalSettingsManager.getConfig().notification.quietTimeStarts
            "quiet_time_ends" -> generalSettingsManager.getConfig().notification.quietTimeEnds
            "message_list_subject_font" -> K9.fontSizes.messageListSubject.toString()
            "message_list_sender_font" -> K9.fontSizes.messageListSender.toString()
            "message_list_date_font" -> K9.fontSizes.messageListDate.toString()
            "message_list_preview_font" -> K9.fontSizes.messageListPreview.toString()
            "message_view_account_name_font" -> K9.fontSizes.messageViewAccountName.toString()
            "message_view_sender_font" -> K9.fontSizes.messageViewSender.toString()
            "message_view_recipients_font" -> K9.fontSizes.messageViewRecipients.toString()
            "message_view_subject_font" -> K9.fontSizes.messageViewSubject.toString()
            "message_view_date_font" -> K9.fontSizes.messageViewDate.toString()
            "message_compose_input_font" -> K9.fontSizes.messageComposeInput.toString()
            "swipe_action_right" -> swipeActionToString(K9.swipeRightAction)
            "swipe_action_left" -> swipeActionToString(K9.swipeLeftAction)
            "message_list_density" -> K9.messageListDensity.toString()
            "post_remove_navigation" -> K9.messageViewPostRemoveNavigation.name
            "post_mark_as_unread_navigation" -> K9.messageViewPostMarkAsUnreadNavigation.name
            else -> defValue
        }
    }

    override fun putString(key: String, value: String?) {
        if (value == null) return

        when (key) {
            "language" -> {
                skipSaveSettings = true
                appLanguageManager.setAppLanguage(value)
            }

            "theme" -> setTheme(value)
            "message_compose_theme" -> setMessageComposeTheme(value)
            "messageViewTheme" -> setMessageViewTheme(value)
            "messagelist_preview_lines" -> K9.messageListPreviewLines = value.toInt()
            "splitview_mode" -> setSplitViewModel(SplitViewMode.valueOf(value.uppercase()))
            "notification_quick_delete" -> {
                K9.notificationQuickDeleteBehaviour = K9.NotificationQuickDelete.valueOf(value)
            }

            "lock_screen_notification_visibility" -> {
                K9.lockScreenNotificationVisibility = K9.LockScreenNotificationVisibility.valueOf(value)
            }

            "background_ops" -> setBackgroundOps(value)
            "quiet_time_starts" -> setQuietTimeStarts(quietTimeStarts = value)
            "quiet_time_ends" -> setQuietTimeEnds(quietTimeEnds = value)
            "message_list_subject_font" -> K9.fontSizes.messageListSubject = value.toInt()
            "message_list_sender_font" -> K9.fontSizes.messageListSender = value.toInt()
            "message_list_date_font" -> K9.fontSizes.messageListDate = value.toInt()
            "message_list_preview_font" -> K9.fontSizes.messageListPreview = value.toInt()
            "message_view_account_name_font" -> K9.fontSizes.messageViewAccountName = value.toInt()
            "message_view_sender_font" -> K9.fontSizes.messageViewSender = value.toInt()
            "message_view_recipients_font" -> K9.fontSizes.messageViewRecipients = value.toInt()
            "message_view_subject_font" -> K9.fontSizes.messageViewSubject = value.toInt()
            "message_view_date_font" -> K9.fontSizes.messageViewDate = value.toInt()
            "message_compose_input_font" -> K9.fontSizes.messageComposeInput = value.toInt()
            "swipe_action_right" -> K9.swipeRightAction = stringToSwipeAction(value)
            "swipe_action_left" -> K9.swipeLeftAction = stringToSwipeAction(value)
            "message_list_density" -> K9.messageListDensity = UiDensity.valueOf(value)
            "post_remove_navigation" -> K9.messageViewPostRemoveNavigation = PostRemoveNavigation.valueOf(value)
            "post_mark_as_unread_navigation" -> {
                K9.messageViewPostMarkAsUnreadNavigation = PostMarkAsUnreadNavigation.valueOf(value)
            }

            else -> return
        }

        saveSettings()
    }

    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? {
        return when (key) {
            "confirm_actions" -> {
                mutableSetOf<String>().apply {
                    if (K9.isConfirmDelete) add("delete")
                    if (K9.isConfirmDeleteStarred) add("delete_starred")
                    if (K9.isConfirmDeleteFromNotification) add("delete_notif")
                    if (K9.isConfirmSpam) add("spam")
                    if (K9.isConfirmDiscardMessage) add("discard")
                    if (K9.isConfirmMarkAllRead) add("mark_all_read")
                }
            }

            "messageview_visible_refile_actions" -> {
                mutableSetOf<String>().apply {
                    if (K9.isMessageViewDeleteActionVisible) add("delete")
                    if (K9.isMessageViewArchiveActionVisible) add("archive")
                    if (K9.isMessageViewMoveActionVisible) add("move")
                    if (K9.isMessageViewCopyActionVisible) add("copy")
                    if (K9.isMessageViewSpamActionVisible) add("spam")
                }
            }

            else -> defValues
        }
    }

    override fun putStringSet(key: String, values: MutableSet<String>?) {
        val checkedValues = values ?: emptySet<String>()
        when (key) {
            "confirm_actions" -> {
                K9.isConfirmDelete = "delete" in checkedValues
                K9.isConfirmDeleteStarred = "delete_starred" in checkedValues
                K9.isConfirmDeleteFromNotification = "delete_notif" in checkedValues
                K9.isConfirmSpam = "spam" in checkedValues
                K9.isConfirmDiscardMessage = "discard" in checkedValues
                K9.isConfirmMarkAllRead = "mark_all_read" in checkedValues
            }

            "messageview_visible_refile_actions" -> {
                K9.isMessageViewDeleteActionVisible = "delete" in checkedValues
                K9.isMessageViewArchiveActionVisible = "archive" in checkedValues
                K9.isMessageViewMoveActionVisible = "move" in checkedValues
                K9.isMessageViewCopyActionVisible = "copy" in checkedValues
                K9.isMessageViewSpamActionVisible = "spam" in checkedValues
            }

            else -> return
        }

        saveSettings()
    }

    private fun saveSettings() {
        if (skipSaveSettings) {
            skipSaveSettings = false
        } else {
            K9.saveSettingsAsync()
        }
    }

    private fun setTheme(value: String) {
        skipSaveSettings = true
        generalSettingsManager.update { settings ->
            settings.copy(
                display = settings.display.copy(
                    coreSettings = settings.display.coreSettings.copy(
                        appTheme = stringToAppTheme(value),
                    ),
                ),
            )
        }
    }

    private fun setMessageComposeTheme(subThemeString: String) {
        skipSaveSettings = true
        generalSettingsManager.update { settings ->
            settings.copy(
                display = settings.display.copy(
                    coreSettings = settings.display.coreSettings.copy(
                        messageComposeTheme = stringToSubTheme(
                            subThemeString,
                        ),
                    ),
                ),
            )
        }
    }

    private fun setMessageViewTheme(subThemeString: String) {
        skipSaveSettings = true
        generalSettingsManager.update { settings ->
            settings.copy(
                display = settings.display.copy(
                    coreSettings = settings.display.coreSettings.copy(
                        messageViewTheme = stringToSubTheme(
                            subThemeString,
                        ),
                    ),
                ),
            )
        }
    }

    private fun setSplitViewModel(mode: SplitViewMode) {
        skipSaveSettings = true
        generalSettingsManager.update { settings ->
            settings.copy(
                display = settings.display.copy(
                    coreSettings = settings.display.coreSettings.copy(
                        splitViewMode = mode,
                    ),
                ),
            )
        }
    }

    private fun setFixedMessageViewTheme(fixedMessageViewTheme: Boolean) {
        skipSaveSettings = true
        generalSettingsManager.update { settings ->
            settings.copy(
                display = settings.display.copy(
                    coreSettings = settings.display.coreSettings.copy(
                        fixedMessageViewTheme = fixedMessageViewTheme,
                    ),
                ),
            )
        }
    }

    private fun setIsShowStarredCount(isShowStarredCount: Boolean) {
        skipSaveSettings = true
        generalSettingsManager.update { settings ->
            settings.copy(
                display = settings.display.copy(
                    inboxSettings = settings.display.inboxSettings.copy(
                        isShowStarredCount = isShowStarredCount,
                    ),
                ),
            )
        }
    }

    private fun setIsShowUnifiedInbox(isShowUnifiedInbox: Boolean) {
        skipSaveSettings = true
        generalSettingsManager.update { settings ->
            settings.copy(
                display = settings.display.copy(
                    inboxSettings = settings.display.inboxSettings.copy(
                        isShowUnifiedInbox = isShowUnifiedInbox,
                    ),
                ),
            )
        }
    }

    private fun setIsShowMessageListStars(isShowMessageListStars: Boolean) {
        skipSaveSettings = true
        generalSettingsManager.update { settings ->
            settings.copy(
                display = settings.display.copy(
                    inboxSettings = settings.display.inboxSettings.copy(
                        isShowMessageListStars = isShowMessageListStars,
                    ),
                ),
            )
        }
    }

    private fun setIsShowAnimations(isShowAnimations: Boolean) {
        skipSaveSettings = true
        generalSettingsManager.update { settings ->
            settings.copy(
                display = settings.display.copy(
                    visualSettings = settings.display.visualSettings.copy(
                        isShowAnimations = isShowAnimations,
                    ),
                ),
            )
        }
    }

    private fun setIsShowCorrespondentNames(isShowCorrespondentNames: Boolean) {
        skipSaveSettings = true
        generalSettingsManager.update { settings ->
            settings.copy(
                display = settings.display.copy(
                    visualSettings = settings.display.visualSettings.copy(
                        isShowCorrespondentNames = isShowCorrespondentNames,
                    ),
                ),
            )
        }
    }

    private fun setIsMessageListSenderAboveSubject(isMessageListSenderAboveSubject: Boolean) {
        skipSaveSettings = true
        generalSettingsManager.update { settings ->
            settings.copy(
                display = settings.display.copy(
                    inboxSettings = settings.display.inboxSettings.copy(
                        isMessageListSenderAboveSubject = isMessageListSenderAboveSubject,
                    ),
                ),
            )
        }
    }

    private fun setIsShowContactName(isShowContactName: Boolean) {
        skipSaveSettings = true
        generalSettingsManager.update { settings ->
            settings.copy(
                display = settings.display.copy(
                    visualSettings = settings.display.visualSettings.copy(
                        isShowContactName = isShowContactName,
                    ),
                ),
            )
        }
    }

    private fun setIsShowContactPicture(isShowContactPicture: Boolean) {
        skipSaveSettings = true
        generalSettingsManager.update { settings ->
            settings.copy(
                display = settings.display.copy(
                    visualSettings = settings.display.visualSettings.copy(
                        isShowContactPicture = isShowContactPicture,
                    ),
                ),
            )
        }
    }

    private fun setIsChangeContactNameColor(isChangeContactNameColor: Boolean) {
        skipSaveSettings = true
        generalSettingsManager.update { settings ->
            settings.copy(
                display = settings.display.copy(
                    visualSettings = settings.display.visualSettings.copy(
                        isChangeContactNameColor = isChangeContactNameColor,
                    ),
                ),
            )
        }
    }

    private fun setIsColorizeMissingContactPictures(isColorizeMissingContactPictures: Boolean) {
        skipSaveSettings = true
        generalSettingsManager.update { settings ->
            settings.copy(
                display = settings.display.copy(
                    visualSettings = settings.display.visualSettings.copy(
                        isColorizeMissingContactPictures = isColorizeMissingContactPictures,
                    ),
                ),
            )
        }
    }

    private fun setIsUseBackgroundAsUnreadIndicator(isUseBackgroundAsUnreadIndicator: Boolean) {
        skipSaveSettings = true
        generalSettingsManager.update { settings ->
            settings.copy(
                display = settings.display.copy(
                    visualSettings = settings.display.visualSettings.copy(
                        isUseBackgroundAsUnreadIndicator = isUseBackgroundAsUnreadIndicator,
                    ),
                ),
            )
        }
    }

    private fun setIsShowComposeButtonOnMessageList(isShowComposeButtonOnMessageList: Boolean) {
        skipSaveSettings = true
        generalSettingsManager.update { settings ->
            settings.copy(
                display = settings.display.copy(
                    inboxSettings = settings.display.inboxSettings.copy(
                        isShowComposeButtonOnMessageList = isShowComposeButtonOnMessageList,
                    ),
                ),
            )
        }
    }

    private fun setIsThreadedViewEnabled(isThreadedViewEnabled: Boolean) {
        skipSaveSettings = true
        generalSettingsManager.update { settings ->
            settings.copy(
                display = settings.display.copy(
                    inboxSettings = settings.display.inboxSettings.copy(
                        isThreadedViewEnabled = isThreadedViewEnabled,
                    ),
                ),
            )
        }
    }

    private fun setIsUseMessageViewFixedWidthFont(isUseMessageViewFixedWidthFont: Boolean) {
        skipSaveSettings = true
        generalSettingsManager.update { settings ->
            settings.copy(
                display = settings.display.copy(
                    visualSettings = settings.display.visualSettings.copy(
                        isUseMessageViewFixedWidthFont = isUseMessageViewFixedWidthFont,
                    ),
                ),
            )
        }
    }

    private fun setQuietTimeStarts(quietTimeStarts: String) {
        skipSaveSettings = true
        generalSettingsManager.update { settings ->
            settings.copy(
                notification = settings.notification.copy(
                    quietTimeStarts = quietTimeStarts,
                ),
            )
        }
    }

    private fun setQuietTimeEnds(quietTimeEnds: String) {
        skipSaveSettings = true
        generalSettingsManager.update { settings ->
            settings.copy(
                notification = settings.notification.copy(
                    quietTimeEnds = quietTimeEnds,
                ),
            )
        }
    }

    private fun setIsAutoFitWidth(isAutoFitWidth: Boolean) {
        skipSaveSettings = true
        generalSettingsManager.update { settings ->
            settings.copy(
                display = settings.display.copy(
                    visualSettings = settings.display.visualSettings.copy(
                        isAutoFitWidth = isAutoFitWidth,
                    ),
                ),
            )
        }
    }

    private fun setIsQuietTimeEnabled(isQuietTimeEnabled: Boolean) {
        skipSaveSettings = true
        generalSettingsManager.update { settings ->
            settings.copy(
                notification = settings.notification.copy(
                    isQuietTimeEnabled = isQuietTimeEnabled,
                ),
            )
        }
    }

    private fun setIsHideTimeZone(isHideTimeZone: Boolean) {
        skipSaveSettings = true
        generalSettingsManager.update { settings ->
            settings.copy(
                privacy = settings.privacy.copy(
                    isHideTimeZone = isHideTimeZone,
                ),
            )
        }
    }

    private fun setIsDebugLoggingEnabled(isDebugLoggingEnabled: Boolean) {
        skipSaveSettings = true
        generalSettingsManager.update { settings ->
            settings.copy(
                debugging = settings.debugging.copy(
                    isDebugLoggingEnabled = isDebugLoggingEnabled,
                ),
            )
        }
    }

    private fun setIsSyncLoggingEnabled(isSyncLoggingEnabled: Boolean) {
        skipSaveSettings = true
        generalSettingsManager.update { settings ->
            settings.copy(
                debugging = settings.debugging.copy(
                    isSyncLoggingEnabled = isSyncLoggingEnabled,
                ),
            )
        }
    }

    private fun setIsSensitiveLoggingEnabled(isSensitiveLoggingEnabled: Boolean) {
        skipSaveSettings = true
        generalSettingsManager.update { settings ->
            settings.copy(
                debugging = settings.debugging.copy(
                    isSensitiveLoggingEnabled = isSensitiveLoggingEnabled,
                ),
            )
        }
    }

    private fun setIsHideUserAgent(isHideUserAgent: Boolean) {
        skipSaveSettings = true
        generalSettingsManager.update { settings ->
            settings.copy(
                privacy = settings.privacy.copy(
                    isHideUserAgent = isHideUserAgent,
                ),
            )
        }
    }

    private fun appThemeToString(theme: AppTheme) = when (theme) {
        AppTheme.LIGHT -> "light"
        AppTheme.DARK -> "dark"
        AppTheme.FOLLOW_SYSTEM -> "follow_system"
    }

    private fun subThemeToString(theme: SubTheme) = when (theme) {
        SubTheme.LIGHT -> "light"
        SubTheme.DARK -> "dark"
        SubTheme.USE_GLOBAL -> "global"
    }

    private fun stringToAppTheme(theme: String?) = when (theme) {
        "light" -> AppTheme.LIGHT
        "dark" -> AppTheme.DARK
        "follow_system" -> AppTheme.FOLLOW_SYSTEM
        else -> throw AssertionError()
    }

    private fun stringToSubTheme(theme: String?) = when (theme) {
        "light" -> SubTheme.LIGHT
        "dark" -> SubTheme.DARK
        "global" -> SubTheme.USE_GLOBAL
        else -> throw AssertionError()
    }

    private fun setBackgroundOps(value: String) {
        val newBackgroundOps = BackgroundOps.valueOf(value)
        if (newBackgroundOps != generalSettingsManager.getConfig().network.backgroundOps) {
            skipSaveSettings = true
            generalSettingsManager.update { settings ->
                settings.copy(network = settings.network.copy(backgroundOps = newBackgroundOps))
            }
            jobManager.scheduleAllMailJobs()
        }
    }

    private fun swipeActionToString(action: SwipeAction) = when (action) {
        SwipeAction.None -> "none"
        SwipeAction.ToggleSelection -> "toggle_selection"
        SwipeAction.ToggleRead -> "toggle_read"
        SwipeAction.ToggleStar -> "toggle_star"
        SwipeAction.Archive, SwipeAction.ArchiveDisabled, SwipeAction.ArchiveSetupArchiveFolder -> "archive"
        SwipeAction.Delete -> "delete"
        SwipeAction.Spam -> "spam"
        SwipeAction.Move -> "move"
    }

    private fun stringToSwipeAction(action: String) = when (action) {
        "none" -> SwipeAction.None
        "toggle_selection" -> SwipeAction.ToggleSelection
        "toggle_read" -> SwipeAction.ToggleRead
        "toggle_star" -> SwipeAction.ToggleStar
        "archive" -> SwipeAction.Archive
        "delete" -> SwipeAction.Delete
        "spam" -> SwipeAction.Spam
        "move" -> SwipeAction.Move
        else -> throw AssertionError()
    }

    private fun setTelemetryEnabled(enable: Boolean) {
        K9.isTelemetryEnabled = enable
        telemetryManager.setEnabled(enable)
    }
}
