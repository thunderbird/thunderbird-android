package com.fsck.k9.ui.settings.account

import android.content.Context
import android.support.v7.preference.PreferenceDataStore
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.service.MailService
import com.fsck.k9.ui.settings.account.FolderListPreference.Companion.NO_FOLDER_SELECTED_VALUE
import java.util.concurrent.ExecutorService

class AccountSettingsDataStore(
        private val context: Context,
        private val preferences: Preferences,
        private val executorService: ExecutorService,
        private val account: Account
) : PreferenceDataStore() {

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        return when (key) {
            "account_default" -> account == preferences.defaultAccount
            "mark_message_as_read_on_view" -> account.isMarkMessageAsReadOnView
            "account_sync_remote_deletetions" -> account.syncRemoteDeletions()
            "push_poll_on_connect" -> account.isPushPollOnConnect
            "always_show_cc_bcc" -> account.isAlwaysShowCcBcc
            "message_read_receipt" -> account.isMessageReadReceiptAlways
            "default_quoted_text_shown" -> account.isDefaultQuotedTextShown
            "reply_after_quote" -> account.isReplyAfterQuote
            "strip_signature" -> account.isStripSignature
            "account_notify" -> account.isNotifyNewMail
            "account_notify_self" -> account.isNotifySelfNewMail
            "account_notify_contacts_mail_only" -> account.isNotifyContactsMailOnly
            "account_vibrate" -> account.notificationSetting.isVibrateEnabled
            "account_led" -> account.notificationSetting.isLedEnabled
            "account_notify_sync" -> account.isShowOngoing
            "notification_opens_unread" -> account.goToUnreadMessageSearch()
            "remote_search_enabled" -> account.allowRemoteSearch()
            "openpgp_hide_sign_only" -> account.openPgpHideSignOnly
            "openpgp_encrypt_subject" -> account.openPgpEncryptSubject
            "autocrypt_prefer_encrypt" -> account.autocryptPreferEncryptMutual
            "upload_sent_messages" -> account.isUploadSentMessages
            else -> defValue
        }
    }

    override fun putBoolean(key: String, value: Boolean) {
        when (key) {
            "account_default" -> {
                executorService.execute {
                    preferences.defaultAccount = account
                }
                return
            }
            "mark_message_as_read_on_view" -> account.isMarkMessageAsReadOnView = value
            "account_sync_remote_deletetions" -> account.setSyncRemoteDeletions(value)
            "push_poll_on_connect" -> account.isPushPollOnConnect = value
            "always_show_cc_bcc" -> account.isAlwaysShowCcBcc = value
            "message_read_receipt" -> account.setMessageReadReceipt(value)
            "default_quoted_text_shown" -> account.isDefaultQuotedTextShown = value
            "reply_after_quote" -> account.isReplyAfterQuote = value
            "strip_signature" -> account.isStripSignature = value
            "account_notify" -> account.isNotifyNewMail = value
            "account_notify_self" -> account.isNotifySelfNewMail = value
            "account_notify_contacts_mail_only" -> account.isNotifyContactsMailOnly = value
            "account_vibrate" -> account.notificationSetting.setVibrate(value)
            "account_led" -> account.notificationSetting.setLed(value)
            "account_notify_sync" -> account.isShowOngoing = value
            "notification_opens_unread" -> account.setGoToUnreadMessageSearch(value)
            "remote_search_enabled" -> account.setAllowRemoteSearch(value)
            "openpgp_hide_sign_only" -> account.openPgpHideSignOnly = value
            "openpgp_encrypt_subject" -> account.openPgpEncryptSubject = value
            "autocrypt_prefer_encrypt" -> account.autocryptPreferEncryptMutual = value
            "upload_sent_messages" -> account.isUploadSentMessages = value
            else -> return
        }

        saveSettingsInBackground()
    }

    override fun getInt(key: String?, defValue: Int): Int {
        return when (key) {
            "chip_color" -> account.chipColor
            "led_color" -> account.notificationSetting.ledColor
            else -> defValue
        }
    }

    override fun putInt(key: String?, value: Int) {
        when (key) {
            "chip_color" -> account.chipColor = value
            "led_color" -> account.notificationSetting.ledColor = value
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
            "openpgp_key" -> account.openPgpKey = value
            else -> return
        }

        saveSettingsInBackground()
    }

    override fun getString(key: String, defValue: String?): String? {
        return when (key) {
            "account_description" -> account.description
            "show_pictures_enum" -> account.showPictures.name
            "account_display_count" -> account.displayCount.toString()
            "account_message_age" -> account.maximumPolledMessageAge.toString()
            "account_autodownload_size" -> account.maximumAutoDownloadMessageSize.toString()
            "account_check_frequency" -> account.automaticCheckIntervalMinutes.toString()
            "folder_sync_mode" -> account.folderSyncMode.name
            "folder_push_mode" -> account.folderPushMode.name
            "delete_policy" -> account.deletePolicy.name
            "expunge_policy" -> account.expungePolicy.name
            "max_push_folders" -> account.maxPushFolders.toString()
            "idle_refresh_period" -> account.idleRefreshMinutes.toString()
            "message_format" -> account.messageFormat.name
            "quote_style" -> account.quoteStyle.name
            "account_quote_prefix" -> account.quotePrefix
            "account_setup_auto_expand_folder" -> account.autoExpandFolder.toFolderPreferenceValue()
            "folder_display_mode" -> account.folderDisplayMode.name
            "folder_target_mode" -> account.folderTargetMode.name
            "searchable_folders" -> account.searchableFolders.name
            "archive_folder" -> account.archiveFolder.toFolderPreferenceValue()
            "drafts_folder" -> account.draftsFolder.toFolderPreferenceValue()
            "sent_folder" -> account.sentFolder.toFolderPreferenceValue()
            "spam_folder" -> account.spamFolder.toFolderPreferenceValue()
            "trash_folder" -> account.trashFolder.toFolderPreferenceValue()
            "folder_notify_new_mail_mode" -> account.folderNotifyNewMailMode.name
            "account_vibrate_pattern" -> account.notificationSetting.vibratePattern.toString()
            "account_vibrate_times" -> account.notificationSetting.vibrateTimes.toString()
            "account_remote_search_num_results" -> account.remoteSearchNumResults.toString()
            "local_storage_provider" -> account.localStorageProviderId
            "account_ringtone" -> account.notificationSetting.ringtone
            else -> defValue
        }
    }

    override fun putString(key: String, value: String?) {
        if (value == null) return

        when (key) {
            "account_description" -> account.description = value
            "show_pictures_enum" -> account.showPictures = Account.ShowPictures.valueOf(value)
            "account_display_count" -> account.displayCount = value.toInt()
            "account_message_age" -> account.maximumPolledMessageAge = value.toInt()
            "account_autodownload_size" -> account.maximumAutoDownloadMessageSize = value.toInt()
            "account_check_frequency" -> {
                if (account.setAutomaticCheckIntervalMinutes(value.toInt())) {
                    reschedulePoll()
                }
            }
            "folder_sync_mode" -> {
                if (account.setFolderSyncMode(Account.FolderMode.valueOf(value))) {
                    reschedulePoll()
                }
            }
            "folder_push_mode" -> {
                if (account.setFolderPushMode(Account.FolderMode.valueOf(value))) {
                    restartPushers()
                }
            }
            "delete_policy" -> account.deletePolicy = Account.DeletePolicy.valueOf(value)
            "expunge_policy" -> account.expungePolicy = Account.Expunge.valueOf(value)
            "max_push_folders" -> account.maxPushFolders = value.toInt()
            "idle_refresh_period" -> account.idleRefreshMinutes = value.toInt()
            "message_format" -> account.messageFormat = Account.MessageFormat.valueOf(value)
            "quote_style" -> account.quoteStyle = Account.QuoteStyle.valueOf(value)
            "account_quote_prefix" -> account.quotePrefix = value
            "account_setup_auto_expand_folder" -> account.autoExpandFolder = value.toFolderStorageValue()
            "folder_display_mode" -> account.folderDisplayMode = Account.FolderMode.valueOf(value)
            "folder_target_mode" -> account.folderTargetMode = Account.FolderMode.valueOf(value)
            "searchable_folders" -> account.searchableFolders = Account.Searchable.valueOf(value)
            "archive_folder" -> account.archiveFolder = value.toFolderStorageValue()
            "drafts_folder" -> account.draftsFolder = value.toFolderStorageValue()
            "sent_folder" -> account.sentFolder = value.toFolderStorageValue()
            "spam_folder" -> account.spamFolder = value.toFolderStorageValue()
            "trash_folder" -> account.trashFolder = value.toFolderStorageValue()
            "folder_notify_new_mail_mode" -> account.folderNotifyNewMailMode = Account.FolderMode.valueOf(value)
            "account_vibrate_pattern" -> account.notificationSetting.vibratePattern = value.toInt()
            "account_vibrate_times" -> account.notificationSetting.vibrateTimes = value.toInt()
            "account_remote_search_num_results" -> account.remoteSearchNumResults = value.toInt()
            "local_storage_provider" -> {
                executorService.execute {
                    account.localStorageProviderId = value
                    saveSettings()
                }
                return
            }
            "account_ringtone" -> with(account.notificationSetting) {
                isRingEnabled = true
                ringtone = value
            }
            else -> return
        }

        saveSettingsInBackground()
    }

    private fun saveSettingsInBackground() {
        executorService.execute {
            saveSettings()
        }
    }

    private fun saveSettings() {
        account.save(preferences)
    }

    private fun reschedulePoll() {
        MailService.actionReschedulePoll(context, null)
    }

    private fun restartPushers() {
        MailService.actionRestartPushers(context, null)
    }

    private fun String.toFolderStorageValue() = if (this == NO_FOLDER_SELECTED_VALUE) null else this

    private fun String?.toFolderPreferenceValue() = this ?: NO_FOLDER_SELECTED_VALUE
}
