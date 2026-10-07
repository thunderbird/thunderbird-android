package net.thunderbird.feature.account.storage.legacy

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import kotlin.test.Test
import kotlinx.coroutines.flow.Flow
import net.thunderbird.account.fake.FakeAccountData
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.MessageFormat
import net.thunderbird.core.android.account.SortType
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.preference.GeneralSettings
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.storage.legacy.fake.FakeStorage
import net.thunderbird.feature.account.storage.legacy.fake.FakeStorageEditor
import net.thunderbird.feature.account.storage.legacy.serializer.ServerSettingsDtoSerializer
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import net.thunderbird.feature.account.storage.profile.ProfileDto

class LegacyAccountStorageHandlerTest {
    private val serverSettingsDtoSerializer = ServerSettingsDtoSerializer()
    private val avatarDtoStorageHandler = LegacyAvatarDtoStorageHandler()
    private val profileDtoStorageHandler = LegacyProfileDtoStorageHandler(avatarDtoStorageHandler)
    private val generalSettingsManager = StubGeneralSettingsManager()
    private val logger = TestLogger()

    private val testSubject = LegacyAccountStorageHandler(
        serverSettingsDtoSerializer = serverSettingsDtoSerializer,
        profileDtoStorageHandler = profileDtoStorageHandler,
        generalSettingsManager = generalSettingsManager,
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
    fun `load should read messageFormatAuto from storage`() {
        // Arrange
        val storage = createStorageWithAccount(
            accountId = ACCOUNT_ID,
            additionalValues = mapOf(
                "$ACCOUNT_ID.messageFormatAuto" to "true",
                "$ACCOUNT_ID.messageFormat" to "TEXT",
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
                    id = accountId,
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

    private class StubGeneralSettingsManager : GeneralSettingsManager {
        @Deprecated("Deprecated in Java", ReplaceWith("getConfig()"))
        override fun getSettings(): GeneralSettings = error("Not supported")

        @Deprecated("Deprecated in Java", ReplaceWith("getConfigFlow()"))
        override fun getSettingsFlow(): Flow<GeneralSettings> = error("Not supported")

        override fun save(config: GeneralSettings) = error("Not supported")

        override fun getConfig(): GeneralSettings = error("Not supported")

        override fun getConfigFlow(): Flow<GeneralSettings> = error("Not supported")
    }
}
