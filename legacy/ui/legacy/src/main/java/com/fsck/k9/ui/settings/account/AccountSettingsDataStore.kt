package com.fsck.k9.ui.settings.account

import androidx.preference.PreferenceDataStore
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.job.K9JobManager
import com.fsck.k9.notification.NotificationChannelManager
import com.fsck.k9.notification.NotificationController
import java.util.concurrent.ExecutorService
import net.thunderbird.core.android.account.DeletePolicy
import net.thunderbird.core.android.account.Expunge
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.android.account.MessageFormat
import net.thunderbird.core.android.account.QuoteStyle
import net.thunderbird.core.android.account.ShowPictures
import net.thunderbird.feature.mail.folder.api.SpecialFolderSelection
import net.thunderbird.feature.notification.NotificationLight
import net.thunderbird.feature.notification.NotificationVibration

class AccountSettingsDataStore(
    private val accountManager: LegacyAccountManager,
    private val executorService: ExecutorService,
    private var account: LegacyAccount,
    private val jobManager: K9JobManager,
    private val notificationChannelManager: NotificationChannelManager,
    private val notificationController: NotificationController,
    private val messagingController: MessagingController,
) : PreferenceDataStore() {
    private var notificationSettingsChanged = false

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        return when (key) {
            "mark_message_as_read_on_view" -> account.isMarkMessageAsReadOnView
            "mark_message_as_read_on_delete" -> account.isMarkMessageAsReadOnDelete
            "account_sync_remote_deletetions" -> account.isSyncRemoteDeletions
            "always_show_cc_bcc" -> account.isAlwaysShowCcBcc
            "message_read_receipt" -> account.isMessageReadReceipt
            "default_quoted_text_shown" -> account.isDefaultQuotedTextShown
            "reply_after_quote" -> account.isReplyAfterQuote
            "strip_signature" -> account.isStripSignature
            "account_notify" -> account.isNotifyNewMail
            "account_notify_self" -> account.isNotifySelfNewMail
            "account_notify_contacts_mail_only" -> account.isNotifyContactsMailOnly
            "account_notify_sync" -> account.isNotifySync
            "openpgp_hide_sign_only" -> account.isOpenPgpHideSignOnly
            "openpgp_encrypt_subject" -> account.isOpenPgpEncryptSubject
            "openpgp_encrypt_all_drafts" -> account.isOpenPgpEncryptAllDrafts
            "autocrypt_prefer_encrypt" -> account.autocryptPreferEncryptMutual
            "upload_sent_messages" -> account.isUploadSentMessages
            "ignore_chat_messages" -> account.isIgnoreChatMessages
            "subscribed_folders_only" -> account.isSubscribedFoldersOnly
            else -> defValue
        }
    }

    override fun putBoolean(key: String, value: Boolean) {
        when (key) {
            "mark_message_as_read_on_view" -> updateValue { it.copy(isMarkMessageAsReadOnView = value) }
            "mark_message_as_read_on_delete" -> updateValue { it.copy(isMarkMessageAsReadOnDelete = value) }
            "account_sync_remote_deletetions" -> updateValue { it.copy(isSyncRemoteDeletions = value) }
            "always_show_cc_bcc" -> updateValue { it.copy(isAlwaysShowCcBcc = value) }
            "message_read_receipt" -> updateValue { it.copy(isMessageReadReceipt = value) }
            "default_quoted_text_shown" -> updateValue { it.copy(isDefaultQuotedTextShown = value) }
            "reply_after_quote" -> updateValue { it.copy(isReplyAfterQuote = value) }
            "strip_signature" -> updateValue { it.copy(isStripSignature = value) }
            "account_notify" -> updateValue { it.copy(isNotifyNewMail = value) }
            "account_notify_self" -> updateValue { it.copy(isNotifySelfNewMail = value) }
            "account_notify_contacts_mail_only" -> updateValue { it.copy(isNotifyContactsMailOnly = value) }
            "account_notify_sync" -> updateValue { it.copy(isNotifySync = value) }
            "openpgp_hide_sign_only" -> updateValue { it.copy(isOpenPgpHideSignOnly = value) }
            "openpgp_encrypt_subject" -> updateValue { it.copy(isOpenPgpEncryptSubject = value) }
            "openpgp_encrypt_all_drafts" -> updateValue { it.copy(isOpenPgpEncryptAllDrafts = value) }
            "autocrypt_prefer_encrypt" -> updateValue { it.copy(autocryptPreferEncryptMutual = value) }
            "upload_sent_messages" -> updateValue { it.copy(isUploadSentMessages = value) }
            "ignore_chat_messages" -> updateValue { it.copy(isIgnoreChatMessages = value) }
            "subscribed_folders_only" -> updateSubscribedFoldersOnly(value)
            else -> return
        }

        saveSettingsInBackground()
    }

    private fun updateValue(update: (LegacyAccount) -> LegacyAccount) {
        account = update(account)
    }

    override fun getInt(key: String?, defValue: Int): Int {
        return when (key) {
            "chip_color" -> account.profile.color
            else -> defValue
        }
    }

    override fun putInt(key: String?, value: Int) {
        when (key) {
            "chip_color" -> setAccountColor(value)
            else -> return
        }

        saveSettingsInBackground()
    }

    override fun getLong(key: String?, defValue: Long): Long {
        return when (key) {
            "openpgp_key" -> account.openPgpKey
            else -> defValue
        }
    }

    override fun putLong(key: String?, value: Long) {
        when (key) {
            "openpgp_key" -> updateValue { it.copy(openPgpKey = value) }
            else -> return
        }

        saveSettingsInBackground()
    }

    override fun getString(key: String, defValue: String?): String? {
        return when (key) {
            "account_description" -> account.name
            "show_pictures_enum" -> account.showPictures.name
            "account_display_count" -> account.displayCount.toString()
            "account_message_age" -> account.maximumPolledMessageAge.toString()
            "account_autodownload_size" -> account.maximumAutoDownloadMessageSize.toString()
            "account_check_frequency" -> account.automaticCheckIntervalMinutes.toString()
            "delete_policy" -> account.deletePolicy.name
            "expunge_policy" -> account.expungePolicy.name
            "max_push_folders" -> account.maxPushFolders.toString()
            "idle_refresh_period" -> account.idleRefreshMinutes.toString()
            "message_format" -> account.messageFormat.name
            "quote_style" -> account.quoteStyle.name
            "account_quote_prefix" -> account.quotePrefix
            "auto_select_folder" -> {
                loadSpecialFolder(account.autoExpandFolderId, SpecialFolderSelection.MANUAL)
            }

            "archive_folder" -> loadSpecialFolder(account.archiveFolderId, account.archiveFolderSelection)
            "drafts_folder" -> loadSpecialFolder(account.draftsFolderId, account.draftsFolderSelection)
            "sent_folder" -> loadSpecialFolder(account.sentFolderId, account.sentFolderSelection)
            "spam_folder" -> loadSpecialFolder(account.spamFolderId, account.spamFolderSelection)
            "trash_folder" -> loadSpecialFolder(account.trashFolderId, account.trashFolderSelection)
            "account_combined_vibration" -> getCombinedVibrationValue()
            "account_remote_search_num_results" -> account.remoteSearchNumResults.toString()
            "account_ringtone" -> account.notificationSettings.ringtone
            "notification_light" -> account.notificationSettings.light.name
            else -> defValue
        }
    }

    override fun putString(key: String, value: String?) {
        if (value == null) return

        when (key) {
            "account_description" -> updateValue { it.copy(name = value) }
            "show_pictures_enum" -> updateValue { it.copy(showPictures = ShowPictures.valueOf(value)) }
            "account_display_count" -> updateValue { it.copy(displayCount = value.toInt()) }
            "account_message_age" -> updateValue { it.copy(maximumPolledMessageAge = value.toInt()) }
            "account_autodownload_size" -> updateValue { it.copy(maximumAutoDownloadMessageSize = value.toInt()) }
            "account_check_frequency" -> {
                val newInterval = value.toInt()
                if (account.automaticCheckIntervalMinutes != newInterval) {
                    updateValue { it.copy(automaticCheckIntervalMinutes = newInterval) }
                    reschedulePoll()
                }
            }

            "delete_policy" -> updateValue { it.copy(deletePolicy = DeletePolicy.valueOf(value)) }
            "expunge_policy" -> updateValue { it.copy(expungePolicy = Expunge.valueOf(value)) }
            "max_push_folders" -> updateValue { it.copy(maxPushFolders = value.toInt()) }
            "idle_refresh_period" -> updateValue { it.copy(idleRefreshMinutes = value.toInt()) }
            "message_format" -> updateValue { it.copy(messageFormat = MessageFormat.valueOf(value)) }
            "quote_style" -> updateValue { it.copy(quoteStyle = QuoteStyle.valueOf(value)) }
            "account_quote_prefix" -> updateValue { it.copy(quotePrefix = value) }
            "auto_select_folder" -> updateValue { it.copy(autoExpandFolderId = extractFolderId(value)) }
            "archive_folder" -> saveSpecialFolderSelection(value) { folderId, selection ->
                updateValue { it.copy(archiveFolderId = folderId, archiveFolderSelection = selection) }
            }

            "drafts_folder" -> saveSpecialFolderSelection(value) { folderId, selection ->
                updateValue { it.copy(draftsFolderId = folderId, draftsFolderSelection = selection) }
            }

            "sent_folder" -> saveSpecialFolderSelection(value) { folderId, selection ->
                updateValue { it.copy(sentFolderId = folderId, sentFolderSelection = selection) }
            }

            "spam_folder" -> saveSpecialFolderSelection(value) { folderId, selection ->
                updateValue { it.copy(spamFolderId = folderId, spamFolderSelection = selection) }
            }

            "trash_folder" -> saveSpecialFolderSelection(value) { folderId, selection ->
                updateValue { it.copy(trashFolderId = folderId, trashFolderSelection = selection) }
            }

            "account_combined_vibration" -> setCombinedVibrationValue(value)
            "account_remote_search_num_results" -> updateValue { it.copy(remoteSearchNumResults = value.toInt()) }
            "account_ringtone" -> setNotificationSound(value)
            "notification_light" -> setNotificationLight(value)
            else -> return
        }

        saveSettingsInBackground()
    }

    private fun setAccountColor(color: Int) {
        if (color != account.profile.color) {
            updateValue { it.copy(profile = it.profile.copy(color = color)) }

            if (account.notificationSettings.light == NotificationLight.AccountColor) {
                notificationSettingsChanged = true
            }
        }
    }

    private fun setNotificationSound(value: String) {
        val notificationSettings = account.notificationSettings
        if (!notificationSettings.isRingEnabled || notificationSettings.ringtone != value) {
            updateValue {
                it.copy(
                    notificationSettings = it.notificationSettings.copy(
                        isRingEnabled = true,
                        ringtone = value,
                    ),
                )
            }
            notificationSettingsChanged = true
        }
    }

    private fun setNotificationLight(value: String) {
        val light = NotificationLight.valueOf(value)
        if (light != account.notificationSettings.light) {
            updateValue {
                it.copy(
                    notificationSettings = it.notificationSettings.copy(light = light),
                )
            }
            notificationSettingsChanged = true
        }
    }

    fun saveSettingsInBackground() {
        executorService.execute {
            if (notificationSettingsChanged) {
                notificationChannelManager.recreateMessagesNotificationChannel(
                    account.id,
                )
                notificationController.restoreNewMailNotifications(listOf(account.id))
            }

            notificationSettingsChanged = false
            saveSettings()
        }
    }

    private fun saveSettings() {
        accountManager.updateSync(account)
    }

    private fun reschedulePoll() {
        jobManager.scheduleMailSync(account)
    }

    private fun extractFolderId(preferenceValue: String): Long? {
        val folderValue = preferenceValue.substringAfter(FolderListPreference.FOLDER_VALUE_DELIMITER)
        return if (folderValue == FolderListPreference.NO_FOLDER_VALUE) null else folderValue.toLongOrNull()
    }

    private fun saveSpecialFolderSelection(
        preferenceValue: String,
        specialFolderSetter: (Long?, SpecialFolderSelection) -> Unit,
    ) {
        val specialFolder = extractFolderId(preferenceValue)

        val specialFolderSelection = if (preferenceValue.startsWith(FolderListPreference.AUTOMATIC_PREFIX)) {
            SpecialFolderSelection.AUTOMATIC
        } else {
            SpecialFolderSelection.MANUAL
        }

        specialFolderSetter(specialFolder, specialFolderSelection)
    }

    private fun loadSpecialFolder(specialFolderId: Long?, specialFolderSelection: SpecialFolderSelection): String {
        val prefix = when (specialFolderSelection) {
            SpecialFolderSelection.AUTOMATIC -> FolderListPreference.AUTOMATIC_PREFIX
            SpecialFolderSelection.MANUAL -> FolderListPreference.MANUAL_PREFIX
        }

        return prefix + (specialFolderId?.toString() ?: FolderListPreference.NO_FOLDER_VALUE)
    }

    private fun getCombinedVibrationValue(): String {
        return with(account.notificationSettings.vibration) {
            VibrationPreference.encode(
                isVibrationEnabled = isEnabled,
                vibratePattern = pattern,
                vibrationTimes = repeatCount,
            )
        }
    }

    private fun setCombinedVibrationValue(value: String) {
        val (isVibrationEnabled, vibrationPattern, vibrationTimes) = VibrationPreference.decode(value)
        updateValue {
            it.copy(
                notificationSettings = it.notificationSettings.copy(
                    vibration = NotificationVibration(
                        isEnabled = isVibrationEnabled,
                        pattern = vibrationPattern,
                        repeatCount = vibrationTimes,
                    ),
                ),
            )
        }
        notificationSettingsChanged = true
    }

    private fun updateSubscribedFoldersOnly(value: Boolean) {
        if (account.isSubscribedFoldersOnly != value) {
            updateValue { it.copy(isSubscribedFoldersOnly = value) }

            messagingController.refreshFolderList(account.id)
        }
    }
}
