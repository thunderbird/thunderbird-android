package net.thunderbird.feature.account.storage.legacy

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import kotlin.test.Test
import net.thunderbird.account.fake.FakeAccountData
import net.thunderbird.components.core.logging.testing.TestLogger
import net.thunderbird.core.android.account.AccountDefaultsProvider
import net.thunderbird.core.android.account.DeletePolicy
import net.thunderbird.core.android.account.Expunge
import net.thunderbird.core.android.account.FolderMode
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.MessageFormat
import net.thunderbird.core.android.account.QuoteStyle
import net.thunderbird.core.android.account.ShowPictures
import net.thunderbird.core.android.account.SortType
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.storage.legacy.fake.FakeStorage
import net.thunderbird.feature.account.storage.legacy.fake.FakeStorageEditor
import net.thunderbird.feature.account.storage.legacy.serializer.ServerSettingsDtoSerializer
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import net.thunderbird.feature.account.storage.profile.ProfileDto
import net.thunderbird.feature.mail.folder.api.SpecialFolderSelection
import net.thunderbird.feature.notification.NotificationLight
import net.thunderbird.feature.notification.NotificationSettings
import net.thunderbird.feature.notification.NotificationVibration
import net.thunderbird.feature.notification.VibratePattern

class LegacyAccountStorageHandlerTest {
    private val serverSettingsDtoSerializer = ServerSettingsDtoSerializer()
    private val avatarDtoStorageHandler = LegacyAvatarDtoStorageHandler()
    private val profileDtoStorageHandler = LegacyProfileDtoStorageHandler(avatarDtoStorageHandler)
    private val logger = TestLogger()

    private val testSubject = LegacyAccountStorageHandler(
        serverSettingsDtoSerializer = serverSettingsDtoSerializer,
        profileDtoStorageHandler = profileDtoStorageHandler,
        logger = logger,
    )

    @Test
    fun `load should populate email from first identity`() {
        // Arrange
        val storage = createStorageWithAccount(
            accountId = ACCOUNT_ID,
            additionalValues = mapOf(
                "$ACCOUNT_ID.name.0" to "John Doe",
                "$ACCOUNT_ID.email.0" to "john@example.com",
            ),
        )

        // Act
        val result = testSubject.load(ACCOUNT_ID, storage)

        // Assert
        assertThat(result.email).isEqualTo("john@example.com")
        assertThat(result.senderName).isEqualTo("John Doe")
        assertThat(result.identities.size).isEqualTo(1)
        assertThat(result.identities.first().email).isEqualTo("john@example.com")
    }

    @Test
    fun `load should set name to null when profile description is empty`() {
        // Arrange
        val storage = createStorageWithAccount(
            accountId = ACCOUNT_ID,
            additionalValues = mapOf(
                "$ACCOUNT_ID.description" to "",
                "$ACCOUNT_ID.email.0" to "test@example.com",
            ),
        )

        // Act
        val result = testSubject.load(ACCOUNT_ID, storage)

        // Assert
        assertThat(result.name).isNull()
    }

    @Test
    fun `load should set name when profile description is present`() {
        // Arrange
        val storage = createStorageWithAccount(
            accountId = ACCOUNT_ID,
            additionalValues = mapOf(
                "$ACCOUNT_ID.description" to "Work Account",
                "$ACCOUNT_ID.email.0" to "test@example.com",
            ),
        )

        // Act
        val result = testSubject.load(ACCOUNT_ID, storage)

        // Assert
        assertThat(result.name).isEqualTo("Work Account")
    }

    @Test
    fun `load should set openPgpProvider to null when not stored`() {
        // Arrange
        val storage = createStorageWithAccount(
            accountId = ACCOUNT_ID,
            additionalValues = mapOf(
                "$ACCOUNT_ID.email.0" to "test@example.com",
            ),
        )

        // Act
        val result = testSubject.load(ACCOUNT_ID, storage)

        // Assert
        assertThat(result.openPgpProvider).isNull()
        assertThat(result.isOpenPgpProviderConfigured()).isFalse()
    }

    @Test
    fun `load should set openPgpProvider to null when stored value is empty`() {
        // Arrange
        val storage = createStorageWithAccount(
            accountId = ACCOUNT_ID,
            additionalValues = mapOf(
                "$ACCOUNT_ID.openPgpProvider" to "",
                "$ACCOUNT_ID.email.0" to "test@example.com",
            ),
        )

        // Act
        val result = testSubject.load(ACCOUNT_ID, storage)

        // Assert
        assertThat(result.openPgpProvider).isNull()
        assertThat(result.isOpenPgpProviderConfigured()).isFalse()
    }

    @Test
    fun `load should set openPgpProvider when stored value is present`() {
        // Arrange
        val storage = createStorageWithAccount(
            accountId = ACCOUNT_ID,
            additionalValues = mapOf(
                "$ACCOUNT_ID.openPgpProvider" to "org.sufficientlysecure.keychain",
                "$ACCOUNT_ID.email.0" to "test@example.com",
            ),
        )

        // Act
        val result = testSubject.load(ACCOUNT_ID, storage)

        // Assert
        assertThat(result.openPgpProvider).isEqualTo("org.sufficientlysecure.keychain")
        assertThat(result.isOpenPgpProviderConfigured()).isTrue()
    }

    @Test
    fun `load should read AUTO message format from storage`() {
        // Arrange
        val storage = createStorageWithAccount(
            accountId = ACCOUNT_ID,
            additionalValues = mapOf(
                "$ACCOUNT_ID.messageFormat" to "AUTO",
                "$ACCOUNT_ID.email.0" to "test@example.com",
            ),
        )

        // Act
        val result = testSubject.load(ACCOUNT_ID, storage)

        // Assert
        assertThat(result.isMessageFormatAuto).isTrue()
        assertThat(result.messageFormat).isEqualTo(MessageFormat.AUTO)
    }

    @Test
    fun `load should preserve sort directions for all sort types`() {
        // Arrange
        val storage = createStorageWithAccount(
            accountId = ACCOUNT_ID,
            additionalValues = mapOf(
                "$ACCOUNT_ID.sortTypeEnum" to SortType.SORT_SUBJECT.name,
                "$ACCOUNT_ID.sortAscending" to "false",
                "$ACCOUNT_ID.sortAscending.${SortType.SORT_DATE.name}" to "true",
            ),
        )

        // Act
        val result = testSubject.load(ACCOUNT_ID, storage)

        // Assert
        assertThat(result.sortAscending[SortType.SORT_SUBJECT]).isEqualTo(false)
        assertThat(result.sortAscending[SortType.SORT_DATE]).isEqualTo(true)
    }

    @Test
    fun `save should persist sort directions for all sort types`() {
        // Arrange
        val account = createTestAccount(ACCOUNT_ID).copy(
            sortType = SortType.SORT_SUBJECT,
            sortAscending = mapOf(SortType.SORT_SUBJECT to false, SortType.SORT_DATE to true),
        )
        val storage = FakeStorage()
        val editor = FakeStorageEditor()

        // Act
        testSubject.save(account, storage, editor)

        // Assert
        assertThat(editor.values["$ACCOUNT_ID.sortAscending"]).isEqualTo("false")
        assertThat(editor.values["$ACCOUNT_ID.sortAscending.${SortType.SORT_DATE.name}"]).isEqualTo("true")
    }

    @Test
    fun `save should persist account settings and identities`() {
        // Arrange
        val account = createTestAccount(ACCOUNT_ID)
        val storage = FakeStorage()
        val editor = FakeStorageEditor()

        // Act
        testSubject.save(account, storage, editor)

        // Assert
        assertThat(editor.values["accountUuids"]).isEqualTo(ACCOUNT_ID.toString())
        assertThat(editor.values["$ACCOUNT_ID.name.0"]).isEqualTo("Test User")
        assertThat(editor.values["$ACCOUNT_ID.email.0"]).isEqualTo("user@example.com")
        assertThat(editor.values["$ACCOUNT_ID.description"]).isEqualTo("Test Account")
    }

    @Test
    fun `load should return every saved value`() {
        // Arrange
        val account = createAccountWithNonDefaultValues(ACCOUNT_ID)

        // Act
        val result = saveAndLoad(account)

        // Assert
        assertThat(result).isEqualTo(account)
    }

    @Test
    fun `load should return null for saved null values`() {
        // Arrange
        val account = createAccountWithNonDefaultValues(ACCOUNT_ID).copy(
            oAuthState = null,
            alwaysBcc = null,
            legacyInboxFolder = null,
            importedDraftsFolder = null,
            importedSentFolder = null,
            importedTrashFolder = null,
            importedArchiveFolder = null,
            importedSpamFolder = null,
            importedAutoExpandFolder = null,
            inboxFolderId = null,
            draftsFolderId = null,
            sentFolderId = null,
            trashFolderId = null,
            archiveFolderId = null,
            spamFolderId = null,
            autoExpandFolderId = null,
            openPgpProvider = null,
        )

        // Act
        val result = saveAndLoad(account)

        // Assert
        assertThat(result).isEqualTo(account)
    }

    @Test
    fun `load should return defaults for saved null quote prefix and ringtone`() {
        // Arrange
        val account = createAccountWithNonDefaultValues(ACCOUNT_ID).copy(
            quotePrefix = null,
            notificationSettings = NotificationSettings(isRingEnabled = true, ringtone = null),
        )

        // Act
        val result = saveAndLoad(account)

        // Assert
        assertThat(result.quotePrefix).isEqualTo(AccountDefaultsProvider.DEFAULT_QUOTE_PREFIX)
        assertThat(result.notificationSettings.ringtone).isEqualTo(AccountDefaultsProvider.DEFAULT_RINGTONE_URI)
    }

    @Test
    fun `delete should remove account keys without affecting other accounts`() {
        // Arrange
        val otherAccountId = "other_account_id"
        val initialValues = mapOf(
            "accountUuids" to "$ACCOUNT_ID,$otherAccountId",
            "$ACCOUNT_ID.email" to "test@example.com",
            "$ACCOUNT_ID.description" to "Test Account",
            "$ACCOUNT_ID.cryptoApp" to "legacy_crypto_app",
            "$ACCOUNT_ID.folder.1.displayMode" to "FIRST_CLASS",
            "$ACCOUNT_ID.identity.email.0" to "identity@example.com",
            "$otherAccountId.email" to "other@example.com",
        )
        val storage = FakeStorage(initialValues)
        val editor = FakeStorageEditor()

        // Act
        testSubject.delete(ACCOUNT_ID, storage, editor)

        // Assert
        assertThat(editor.removedKeys).contains("$ACCOUNT_ID.email")
        assertThat(editor.removedKeys).contains("$ACCOUNT_ID.description")
        assertThat(editor.removedKeys).contains("$ACCOUNT_ID.cryptoApp")
        assertThat(editor.removedKeys).contains("$ACCOUNT_ID.folder.1.displayMode")
        assertThat(editor.removedKeys).contains("$ACCOUNT_ID.identity.email.0")
        assertThat(editor.removedKeys.all { it.startsWith("$ACCOUNT_ID.") }).isEqualTo(true)
        val remainingValues = (initialValues - editor.removedKeys.toSet()) + editor.values
            .filterValues { it != null }
            .mapValues { (_, value) -> requireNotNull(value) }
        assertThat(remainingValues).isEqualTo(
            mapOf(
                "accountUuids" to otherAccountId,
                "$otherAccountId.email" to "other@example.com",
            ),
        )
    }

    /**
     * Saves [account] the way the real storage does (a null string removes the key) and loads it again.
     */
    private fun saveAndLoad(account: LegacyAccount): LegacyAccount {
        val editor = FakeStorageEditor()
        testSubject.save(account, FakeStorage(), editor)
        val storedValues = editor.values.filterValues { it != null }.mapValues { (_, value) -> requireNotNull(value) }

        return testSubject.load(account.id, FakeStorage(storedValues))
    }

    @Suppress("LongMethod")
    private fun createAccountWithNonDefaultValues(accountId: AccountId): LegacyAccount {
        val identities = listOf(
            Identity(
                name = "Test User",
                email = "user@example.com",
                signatureUse = true,
                signature = "-- Test",
                signatureIsHtml = true,
                description = "Main identity",
                replyTo = "reply@example.com",
            ),
            Identity(
                name = "Alias",
                email = "alias@example.com",
                description = "Alias identity",
            ),
        )
        return LegacyAccount(
            id = accountId,
            name = "Test Account",
            email = "user@example.com",
            profile = ProfileDto(
                id = accountId,
                name = "Test Account",
                color = 0xFF0000,
                avatar = AvatarDto(
                    accountId = accountId,
                    avatarType = AvatarTypeDto.MONOGRAM,
                    avatarMonogram = "TA",
                    avatarImageUri = null,
                    avatarIconName = null,
                ),
            ),
            deletePolicy = DeletePolicy.ON_DELETE,
            incomingServerSettings = DEFAULT_SERVER_SETTINGS,
            outgoingServerSettings = DEFAULT_SERVER_SETTINGS.copy(type = "smtp", port = 465),
            oAuthState = "oauth-state",
            alwaysBcc = "bcc@example.com",
            automaticCheckIntervalMinutes = 30,
            displayCount = 100,
            isNotifyNewMail = true,
            folderNotifyNewMailMode = FolderMode.FIRST_CLASS,
            isNotifySelfNewMail = false,
            isNotifyContactsMailOnly = true,
            isIgnoreChatMessages = true,
            legacyInboxFolder = "INBOX",
            importedDraftsFolder = "Drafts",
            importedSentFolder = "Sent",
            importedTrashFolder = "Trash",
            importedArchiveFolder = "Archive",
            importedSpamFolder = "Spam",
            inboxFolderId = 1,
            draftsFolderId = 2,
            sentFolderId = 3,
            trashFolderId = 4,
            archiveFolderId = 5,
            spamFolderId = 6,
            draftsFolderSelection = SpecialFolderSelection.MANUAL,
            sentFolderSelection = SpecialFolderSelection.MANUAL,
            trashFolderSelection = SpecialFolderSelection.MANUAL,
            archiveFolderSelection = SpecialFolderSelection.MANUAL,
            spamFolderSelection = SpecialFolderSelection.MANUAL,
            importedAutoExpandFolder = "Expand",
            autoExpandFolderId = 7,
            folderDisplayMode = FolderMode.ALL,
            folderSyncMode = FolderMode.ALL,
            folderPushMode = FolderMode.FIRST_CLASS,
            accountNumber = 3,
            isNotifySync = true,
            sortType = SortType.SORT_SUBJECT,
            sortAscending = mapOf(SortType.SORT_SUBJECT to false, SortType.SORT_DATE to true),
            showPictures = ShowPictures.ALWAYS,
            isSignatureBeforeQuotedText = true,
            expungePolicy = Expunge.EXPUNGE_MANUALLY,
            maxPushFolders = 5,
            idleRefreshMinutes = 12,
            useCompression = false,
            isSendClientInfoEnabled = false,
            isSubscribedFoldersOnly = true,
            maximumPolledMessageAge = 28,
            maximumAutoDownloadMessageSize = 65536,
            messageFormat = MessageFormat.AUTO,
            isMessageFormatAuto = true,
            isMessageReadReceipt = true,
            quoteStyle = QuoteStyle.HEADER,
            quotePrefix = "| ",
            isDefaultQuotedTextShown = false,
            isReplyAfterQuote = true,
            isStripSignature = false,
            isSyncRemoteDeletions = false,
            openPgpProvider = "org.sufficientlysecure.keychain",
            openPgpKey = 42,
            autocryptPreferEncryptMutual = true,
            isOpenPgpHideSignOnly = false,
            isOpenPgpEncryptSubject = false,
            isOpenPgpEncryptAllDrafts = false,
            isMarkMessageAsReadOnView = false,
            isMarkMessageAsReadOnDelete = false,
            isAlwaysShowCcBcc = true,
            isRemoteSearchFullText = true,
            remoteSearchNumResults = 50,
            isUploadSentMessages = false,
            lastSyncTime = 1_700_000_000_000,
            lastFolderListRefreshTime = 1_700_000_001_000,
            isFinishedSetup = false,
            messagesNotificationChannelVersion = 4,
            identities = identities,
            notificationSettings = NotificationSettings(
                isRingEnabled = false,
                ringtone = "content://ringtone",
                light = NotificationLight.Blue,
                vibration = NotificationVibration(isEnabled = true, pattern = VibratePattern.Pattern3, repeatCount = 2),
            ),
            shouldMigrateToOAuth = true,
            folderPathDelimiter = ".",
        )
    }

    private fun createStorageWithAccount(
        accountId: AccountId,
        additionalValues: Map<String, String> = emptyMap(),
    ): FakeStorage {
        val serverSettingsJson = serverSettingsDtoSerializer.serialize(DEFAULT_SERVER_SETTINGS)
        val baseValues = mapOf(
            "$accountId.incomingServerSettings" to serverSettingsJson,
            "$accountId.outgoingServerSettings" to serverSettingsJson,
        )
        return FakeStorage(baseValues + additionalValues)
    }

    private fun createTestAccount(accountId: AccountId): LegacyAccount {
        return LegacyAccount(
            id = accountId,
            name = "Test Account",
            email = "user@example.com",
            profile = ProfileDto(
                id = accountId,
                name = "Test Account",
                color = 0,
                avatar = AvatarDto(
                    accountId = accountId,
                    avatarType = AvatarTypeDto.MONOGRAM,
                    avatarMonogram = "T",
                    avatarImageUri = null,
                    avatarIconName = null,
                ),
            ),
            incomingServerSettings = DEFAULT_SERVER_SETTINGS,
            outgoingServerSettings = DEFAULT_SERVER_SETTINGS,
            identities = listOf(
                Identity(
                    name = "Test User",
                    email = "user@example.com",
                ),
            ),
        )
    }

    private companion object {
        val ACCOUNT_ID = FakeAccountData.ACCOUNT_ID

        val DEFAULT_SERVER_SETTINGS = ServerSettings(
            type = "imap",
            host = "imap.example.com",
            port = 993,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user@example.com",
            password = "password",
            clientCertificateAlias = null,
        )
    }
}
