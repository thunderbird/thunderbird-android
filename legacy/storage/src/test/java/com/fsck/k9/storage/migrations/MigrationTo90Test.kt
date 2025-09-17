package com.fsck.k9.storage.migrations

import android.database.sqlite.SQLiteDatabase
import assertk.all
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.oauth.OAuth2TokenProviderFactory
import com.fsck.k9.mail.ssl.TrustedSocketFactory
import com.fsck.k9.mail.store.imap.ImapStore
import com.fsck.k9.mail.store.imap.ImapStoreFactory
import com.fsck.k9.mail.store.imap.ImapStoreSettings
import com.fsck.k9.mailstore.MigrationsHelper
import com.fsck.k9.storage.messages.createFolder
import com.fsck.k9.storage.messages.readFolders
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.feature.mail.folder.api.FOLDER_DEFAULT_PATH_DELIMITER
import net.thunderbird.feature.mail.folder.api.FolderPathDelimiter
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Suppress("MaxLineLength", "LongParameterList")
class MigrationTo90Test : KoinTest {
    private val testLogger = TestLogger()
    private val database = createDatabaseVersion89()
    private val trustedSocketFactory: TrustedSocketFactory = mock()
    private val oAuth2TokenProviderFactory: OAuth2TokenProviderFactory = mock()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        modules(
            module {
                single<TrustedSocketFactory> { trustedSocketFactory }
                single<OAuth2TokenProviderFactory> { oAuth2TokenProviderFactory }
                single(named("ClientInfoAppName")) { "MigrationTo90" }
                single(named("ClientInfoAppVersion")) { "1.0.0" }
            },
        )
    }

    @Before
    fun setup() {
        Log.logger = testLogger
    }

    @After
    fun tearDown() {
        testLogger.events.clear()
        wipeDatabase()
        database.close()
    }

    @Test
    fun `given the user set an empty imap prefix manually - when running the migration - server_id must keep the same value`() {
        // Arrange
        val folderCount = 100
        populateDatabase(
            folderCount = folderCount,
            serverIdPrefix = "INBOX",
            folderPathDelimiter = FOLDER_DEFAULT_PATH_DELIMITER,
        )
        val imapStore = createImapStoreSpy()
        val incomingServerSettings = createIncomingServerSettings(
            pathPrefix = "",
            autoDetectNamespace = false,
        )
        val account = createAccount(incomingServerSettings)
        val migrationHelper = createMigrationsHelper(account)
        val migration = MigrationTo90(
            db = database,
            migrationsHelper = migrationHelper,
            imapStoreFactory = createImapStoreFactory(imapStore),
        )
        val expected = database.readFolders().map { it.serverId }
        assert(expected.size == folderCount)

        // Act
        migration.removeImapPrefixFromFolderServerId()
        val actual = database.readFolders().mapNotNull { it.serverId }
        testLogger.dumpLogs()

        // Assert
        verify(imapStore, times(1)).fetchImapPrefix()
        assertThat(actual)
            .all {
                hasSize(expected.size)
                isEqualTo(expected)
            }
    }

    @Test
    fun `given the server returns an empty imap prefix - when running the migration - server_id must keep the same value`() {
        // Arrange
        val folderCount = 100
        populateDatabase(
            folderCount = folderCount,
            serverIdPrefix = "INBOX",
            folderPathDelimiter = FOLDER_DEFAULT_PATH_DELIMITER,
        )
        val imapStore = createImapStoreSpy(
            imapPrefix = "",
        )
        val incomingServerSettings = createIncomingServerSettings()
        val account = createAccount(incomingServerSettings)
        val migrationHelper = createMigrationsHelper(account)
        val migration = MigrationTo90(
            db = database,
            migrationsHelper = migrationHelper,
            imapStoreFactory = createImapStoreFactory(imapStore),
        )
        val expected = database.readFolders().map { it.serverId }
        assert(expected.size == folderCount)

        // Act
        migration.removeImapPrefixFromFolderServerId()
        val actual = database.readFolders().mapNotNull { it.serverId }
        testLogger.dumpLogs()

        // Assert
        verify(imapStore, times(1)).fetchImapPrefix()
        assertThat(actual)
            .all {
                hasSize(expected.size)
                isEqualTo(expected)
            }
    }

    @Test
    fun `given the server return an imap prefix - when folder's server_id includes imap prefix - server_id must remove the prefix`() {
        // Arrange
        val prefix = "INBOX"
        val folderDelimiter = "."
        populateDatabase(serverIdPrefix = prefix, folderPathDelimiter = folderDelimiter)
        val imapStore = createImapStoreSpy(
            imapPrefix = prefix,
            folderPathDelimiter = folderDelimiter,
        )
        val incomingServerSettings = createIncomingServerSettings(pathPrefix = prefix, autoDetectNamespace = false)
        val account = createAccount(
            incomingServerSettings = incomingServerSettings,
            folderPathDelimiter = folderDelimiter,
        )
        val migrationHelper = createMigrationsHelper(account)
        val migration = MigrationTo90(
            db = database,
            migrationsHelper = migrationHelper,
            imapStoreFactory = createImapStoreFactory(imapStore),
        )
        val expected = database.readFolders().map { it.serverId }

        // Act
        migration.removeImapPrefixFromFolderServerId()
        val actual = database.readFolders().map { it.serverId }
        testLogger.dumpLogs()

        // Assert
        verify(imapStore, times(1)).fetchImapPrefix()

        assertThat(actual)
            .all {
                hasSize(expected.size)
                isEqualTo(expected.map { it?.removePrefix("$prefix$folderDelimiter") })
            }
    }

    @Test
    fun `given a non-imap account - when running the migration - server_id must keep the same value`() {
        // Arrange
        populateDatabase()
        val imapStore = createImapStoreSpy()
        val incomingServerSettings = createIncomingServerSettings(
            protocolType = Protocols.POP3,
        )
        val account = createAccount(incomingServerSettings)
        val migrationHelper = createMigrationsHelper(account)
        val spyDb = spy<SQLiteDatabase> { database }
        val migration = MigrationTo90(
            db = spyDb,
            migrationsHelper = migrationHelper,
            imapStoreFactory = createImapStoreFactory(imapStore),
        )
        val expected = database.readFolders().map { it.serverId }

        // Act
        migration.removeImapPrefixFromFolderServerId()
        val actual = database.readFolders().mapNotNull { it.serverId }
        testLogger.dumpLogs()

        // Assert
        verify(imapStore, times(0)).fetchImapPrefix()
        verify(spyDb, times(0)).execSQL(any())
        assertThat(actual)
            .all {
                hasSize(expected.size)
                isEqualTo(expected)
            }
    }

    @Test
    fun `given an imap account - when can't fetch imap prefix during the migration - migration should not execute sql queries`() {
        // Arrange
        populateDatabase()
        val prefix = "INBOX."
        val imapStore = createImapStoreSpy(
            imapPrefix = prefix,
            folderPathDelimiter = ".",
            authenticationFailedException = AuthenticationFailedException(message = "failed authenticate"),
        )
        val incomingServerSettings = createIncomingServerSettings()
        val account = createAccount(incomingServerSettings)
        val migrationHelper = createMigrationsHelper(account)
        val dbSpy = spy<SQLiteDatabase> { database }
        val migration = MigrationTo90(
            db = dbSpy,
            migrationsHelper = migrationHelper,
            imapStoreFactory = createImapStoreFactory(imapStore),
        )
        val expected = database.readFolders().map { it.serverId }
        val updateQuery = migration.buildQuery(imapPrefix = prefix)

        // Act
        migration.removeImapPrefixFromFolderServerId()
        val actual = database.readFolders().mapNotNull { it.serverId }
        testLogger.dumpLogs()

        // Assert
        verify(imapStore, times(1)).fetchImapPrefix()
        verify(dbSpy, times(0)).execSQL(updateQuery)
        assertThat(actual)
            .all {
                hasSize(expected.size)
                isEqualTo(expected)
            }
    }

    private fun createIncomingServerSettings(
        protocolType: String = Protocols.IMAP,
        authType: AuthType = AuthType.NONE,
        autoDetectNamespace: Boolean = false,
        pathPrefix: String = "",
        useCompression: Boolean = false,
        isSendClientInfoEnabled: Boolean = false,
    ): ServerSettings = ServerSettings(
        type = protocolType,
        host = "imap.test.com",
        port = 993,
        connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
        authenticationType = authType,
        username = "user",
        password = "pass",
        clientCertificateAlias = null,
        extra = buildMap {
            put(ImapStoreSettings.AUTODETECT_NAMESPACE_KEY, autoDetectNamespace.toString())
            put(ImapStoreSettings.PATH_PREFIX_KEY, pathPrefix)
            put(ImapStoreSettings.USE_COMPRESSION, useCompression.toString())
            put(ImapStoreSettings.SEND_CLIENT_INFO, isSendClientInfoEnabled.toString())
        },
    )

    private fun createAccount(
        incomingServerSettings: ServerSettings = createIncomingServerSettings(),
        oAuthState: String? = null,
        folderPathDelimiter: FolderPathDelimiter = FOLDER_DEFAULT_PATH_DELIMITER,
    ): LegacyAccountDto {
        return mock {
            on { this.incomingServerSettings } doReturn incomingServerSettings
            on { this.oAuthState } doReturn oAuthState
            on { this.folderPathDelimiter } doReturn folderPathDelimiter
        }
    }

    private fun createMigrationsHelper(account: LegacyAccountDto): MigrationsHelper {
        return object : MigrationsHelper {
            override fun getAccount(): LegacyAccountDto {
                return account
            }

            override fun saveAccount() {
                throw UnsupportedOperationException("not implemented")
            }
        }
    }

    private fun createImapStoreSpy(
        imapPrefix: String? = null,
        folderPathDelimiter: FolderPathDelimiter = FOLDER_DEFAULT_PATH_DELIMITER,
        authenticationFailedException: AuthenticationFailedException? = null,
    ): ImapStore = spy<ImapStore> {
        on { this.combinedPrefix } doReturn imapPrefix
            ?.takeIf { it.isNotBlank() }
            ?.let { "$it$folderPathDelimiter" }

        on { fetchImapPrefix() } doAnswer {
            if (authenticationFailedException != null) {
                throw authenticationFailedException
            }
        }
    }

    private fun createImapStoreFactory(imapStore: ImapStore = createImapStoreSpy()): ImapStoreFactory =
        ImapStoreFactory { _, _, _, _ ->
            imapStore
        }

    private fun createDatabaseVersion89(): SQLiteDatabase = SQLiteDatabase.create(null).apply {
        execSQL(
            """
                CREATE TABLE folders (
                    id INTEGER PRIMARY KEY,
                    name TEXT,
                    last_updated INTEGER,
                    unread_count INTEGER,
                    visible_limit INTEGER,
                    status TEXT,
                    flagged_count INTEGER default 0,
                    integrate INTEGER,
                    top_group INTEGER,
                    sync_enabled INTEGER DEFAULT 0,
                    push_enabled INTEGER DEFAULT 0,
                    notifications_enabled INTEGER DEFAULT 0,
                    more_messages TEXT default "unknown",
                    server_id TEXT,
                    local_only INTEGER,
                    type TEXT DEFAULT "regular",
                    visible INTEGER DEFAULT 1
                )
            """.trimIndent(),
        )
    }

    private fun populateDatabase(
        folderCount: Int = 10,
        serverIdPrefix: String? = null,
        folderPathDelimiter: FolderPathDelimiter = FOLDER_DEFAULT_PATH_DELIMITER,
    ) {
        repeat(folderCount) { folderNumber ->
            val folderName = "Folder $folderNumber"
            database.createFolder(
                name = folderName,
                serverId = serverIdPrefix?.let { prefix -> "$prefix$folderPathDelimiter$folderName" } ?: folderName,
            )
        }
    }

    private fun wipeDatabase() {
        database.delete("folders", null, null)
    }

    private fun TestLogger.dumpLogs() {
        events.forEach { event -> println(event) }
    }
}
