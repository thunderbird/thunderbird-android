package net.thunderbird.app.common.account

import app.k9mail.core.featureflag.FeatureFlagProvider
import app.k9mail.core.featureflag.toFeatureFlagKey
import app.k9mail.legacy.account.AccountDefaultsProvider
import app.k9mail.legacy.account.AccountDefaultsProvider.Companion.DEFAULT_MAXIMUM_AUTO_DOWNLOAD_MESSAGE_SIZE
import app.k9mail.legacy.account.AccountDefaultsProvider.Companion.DEFAULT_MESSAGE_FORMAT
import app.k9mail.legacy.account.AccountDefaultsProvider.Companion.DEFAULT_MESSAGE_FORMAT_AUTO
import app.k9mail.legacy.account.AccountDefaultsProvider.Companion.DEFAULT_MESSAGE_READ_RECEIPT
import app.k9mail.legacy.account.AccountDefaultsProvider.Companion.DEFAULT_QUOTED_TEXT_SHOWN
import app.k9mail.legacy.account.AccountDefaultsProvider.Companion.DEFAULT_QUOTE_PREFIX
import app.k9mail.legacy.account.AccountDefaultsProvider.Companion.DEFAULT_QUOTE_STYLE
import app.k9mail.legacy.account.AccountDefaultsProvider.Companion.DEFAULT_REMOTE_SEARCH_NUM_RESULTS
import app.k9mail.legacy.account.AccountDefaultsProvider.Companion.DEFAULT_REPLY_AFTER_QUOTE
import app.k9mail.legacy.account.AccountDefaultsProvider.Companion.DEFAULT_RINGTONE_URI
import app.k9mail.legacy.account.AccountDefaultsProvider.Companion.DEFAULT_SORT_ASCENDING
import app.k9mail.legacy.account.AccountDefaultsProvider.Companion.DEFAULT_SORT_TYPE
import app.k9mail.legacy.account.AccountDefaultsProvider.Companion.DEFAULT_STRIP_SIGNATURE
import app.k9mail.legacy.account.AccountDefaultsProvider.Companion.DEFAULT_SYNC_INTERVAL
import app.k9mail.legacy.account.AccountDefaultsProvider.Companion.NO_OPENPGP_KEY
import app.k9mail.legacy.account.AccountDefaultsProvider.Companion.UNASSIGNED_ACCOUNT_NUMBER
import app.k9mail.legacy.account.Expunge
import app.k9mail.legacy.account.FolderMode
import app.k9mail.legacy.account.Identity
import app.k9mail.legacy.account.LegacyAccount
import app.k9mail.legacy.account.ShowPictures
import com.fsck.k9.CoreResourceProvider
import com.fsck.k9.K9
import net.thunderbird.core.mail.folder.api.SpecialFolderSelection
import net.thunderbird.feature.notification.NotificationLight
import net.thunderbird.feature.notification.NotificationSettings
import net.thunderbird.feature.notification.NotificationVibration

@Suppress("MagicNumber")
internal class CommonAccountDefaultsProvider(
    private val resourceProvider: CoreResourceProvider,
    private val featureFlagProvider: FeatureFlagProvider,
) : AccountDefaultsProvider {

    override fun applyDefaults(account: LegacyAccount) = with(account) {
        applyLegacyDefaults()
    }

    override fun applyOverwrites(account: LegacyAccount) = with(account) {
        isNotifyNewMail = featureFlagProvider.provide(
            "email_notification_default".toFeatureFlagKey(),
        ).whenEnabledOrNot(
            onEnabled = { true },
            onDisabledOrUnavailable = { false },
        )

        isNotifySelfNewMail = featureFlagProvider.provide(
            "email_notification_default".toFeatureFlagKey(),
        ).whenEnabledOrNot(
            onEnabled = { true },
            onDisabledOrUnavailable = { false },
        )
    }

    @Suppress("LongMethod")
    private fun LegacyAccount.applyLegacyDefaults() {
        automaticCheckIntervalMinutes = DEFAULT_SYNC_INTERVAL
        idleRefreshMinutes = 24
        displayCount = K9.DEFAULT_VISIBLE_LIMIT
        accountNumber = UNASSIGNED_ACCOUNT_NUMBER
        isNotifyNewMail = true
        folderNotifyNewMailMode = FolderMode.ALL
        isNotifySync = false
        isNotifySelfNewMail = true
        isNotifyContactsMailOnly = false
        isIgnoreChatMessages = false
        messagesNotificationChannelVersion = 0
        folderDisplayMode = FolderMode.NOT_SECOND_CLASS
        folderSyncMode = FolderMode.FIRST_CLASS
        folderPushMode = FolderMode.NONE
        sortType = DEFAULT_SORT_TYPE
        setSortAscending(DEFAULT_SORT_TYPE, DEFAULT_SORT_ASCENDING)
        showPictures = ShowPictures.NEVER
        isSignatureBeforeQuotedText = false
        expungePolicy = Expunge.EXPUNGE_IMMEDIATELY
        importedAutoExpandFolder = null
        legacyInboxFolder = null
        maxPushFolders = 10
        isSubscribedFoldersOnly = false
        maximumPolledMessageAge = -1
        maximumAutoDownloadMessageSize = DEFAULT_MAXIMUM_AUTO_DOWNLOAD_MESSAGE_SIZE
        messageFormat = DEFAULT_MESSAGE_FORMAT
        isMessageFormatAuto = DEFAULT_MESSAGE_FORMAT_AUTO
        isMessageReadReceipt = DEFAULT_MESSAGE_READ_RECEIPT
        quoteStyle = DEFAULT_QUOTE_STYLE
        quotePrefix = DEFAULT_QUOTE_PREFIX
        isDefaultQuotedTextShown = DEFAULT_QUOTED_TEXT_SHOWN
        isReplyAfterQuote = DEFAULT_REPLY_AFTER_QUOTE
        isStripSignature = DEFAULT_STRIP_SIGNATURE
        isSyncRemoteDeletions = true
        openPgpKey = NO_OPENPGP_KEY
        isRemoteSearchFullText = false
        remoteSearchNumResults = DEFAULT_REMOTE_SEARCH_NUM_RESULTS
        isUploadSentMessages = true
        isMarkMessageAsReadOnView = true
        isMarkMessageAsReadOnDelete = true
        isAlwaysShowCcBcc = false
        lastSyncTime = 0L
        lastFolderListRefreshTime = 0L

        setArchiveFolderId(null, SpecialFolderSelection.AUTOMATIC)
        setDraftsFolderId(null, SpecialFolderSelection.AUTOMATIC)
        setSentFolderId(null, SpecialFolderSelection.AUTOMATIC)
        setSpamFolderId(null, SpecialFolderSelection.AUTOMATIC)
        setTrashFolderId(null, SpecialFolderSelection.AUTOMATIC)

        identities = ArrayList<Identity>()

        val identity = Identity(
            signatureUse = false,
            signature = null,
            description = resourceProvider.defaultIdentityDescription(),
        )
        identities.add(identity)

        updateNotificationSettings {
            NotificationSettings(
                isRingEnabled = true,
                ringtone = DEFAULT_RINGTONE_URI,
                light = NotificationLight.Disabled,
                vibration = NotificationVibration.DEFAULT,
            )
        }

        resetChangeMarkers()
    }
}
