package com.fsck.k9.preferences

import android.util.Base64
import androidx.core.net.toUri
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.fsck.k9.K9RobolectricTest
import com.fsck.k9.account.FakeStorageBackedAccountManager
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.android.account.AccountDefaultsProvider
import net.thunderbird.core.android.account.Identity
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.storage.legacy.AccountStorageHandler
import net.thunderbird.feature.account.storage.profile.ProfileDto
import net.thunderbird.feature.mail.folder.api.data.repository.FolderQueryRepository
import kotlinx.coroutines.runBlocking
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.core.preference.storage.StorageProvider
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import org.jdom2.Document
import org.jdom2.input.SAXBuilder
import org.junit.Before
import org.junit.Test
import org.koin.core.component.inject
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.mockito.kotlin.mock
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

class SettingsExporterTest : K9RobolectricTest() {
    private val contentResolver = RuntimeEnvironment.getApplication().contentResolver
    private val storageProvider: StorageProvider by inject()
    private val accountManager: LegacyAccountManager by inject()
    private val folderSettingsProvider: FolderSettingsProvider by inject()
    private val folderQueryRepository: FolderQueryRepository by inject()
    private val accountManagerModule = module {
        single<LegacyAccountManager> {
            FakeStorageBackedAccountManager(
                storageProvider = get<StorageProvider>(),
                accountStorageHandler = get<AccountStorageHandler>(),
                accountDefaultsProvider = get<AccountDefaultsProvider>(),
            )
        }
    }
    private val settingsExporter by lazy {
        SettingsExporter(
            contentResolver,
            accountManager,
            storageProvider,
            folderSettingsProvider,
            folderQueryRepository,
            notificationSettingsUpdater = mock(),
            filePrefixProvider = mock(),
        )
    }

    @Before
    fun setUp() {
        loadKoinModules(accountManagerModule)
    }

    @Test
    fun exportPreferences_producesXML() = runTest {
        val document = exportPreferences(false, emptySet())

        assertThat(document.rootElement.name).isEqualTo("k9settings")
    }

    @Test
    fun exportPreferences_setsVersionToLatest() = runTest {
        val document = exportPreferences(false, emptySet())

        assertThat(document.rootElement.getAttributeValue("version")).isEqualTo(Settings.VERSION.toString())
    }

    @Test
    fun exportPreferences_setsFormatTo1() = runTest {
        val document = exportPreferences(false, emptySet())

        assertThat(document.rootElement.getAttributeValue("format")).isEqualTo("1")
    }

    @Test
    fun exportPreferences_exportsGlobalSettingsWhenRequested() = runTest {
        val document = exportPreferences(true, emptySet())

        assertThat(document.rootElement.getChild("global")).isNotNull()
    }

    @Test
    fun exportPreferences_ignoresGlobalSettingsWhenRequested() = runTest {
        val document = exportPreferences(false, emptySet())

        assertThat(document.rootElement.getChild("global")).isNull()
    }

    @Test
    fun exportPreferences_includesAvatarImageForImageAvatar() = runTest {
        val avatarUri = "content://test/avatar".toUri()
        val imageBytes = byteArrayOf(1, 2, 3, 4)
        val expectedEncoded = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

        shadowOf(contentResolver).registerInputStream(avatarUri, imageBytes.inputStream())
        val accountUuid = saveAccount(avatarType = AvatarTypeDto.IMAGE, avatarImageUri = avatarUri.toString())

        val document = exportPreferences(false, setOf(accountUuid))

        val account = document.rootElement.getChild("accounts").getChild("account")
        val avatarImage = account.getChild("avatar-image")
        assertThat(avatarImage).isNotNull()
        assertThat(avatarImage.text).isEqualTo(expectedEncoded)
    }

    @Test
    fun exportPreferences_omitsAvatarImageForMonogramAvatar() = runTest {
        val accountUuid = saveAccount(avatarType = AvatarTypeDto.MONOGRAM, avatarMonogram = "XX")

        val document = exportPreferences(false, setOf(accountUuid))
        val exported = document.rootElement.getChild("accounts").getChild("account")
        assertThat(exported.getChild("avatar-image")).isNull()
    }

    @Test
    fun exportPreferences_omitsAvatarImageForImagePlaceholderUri() = runTest {
        val accountUuid = saveAccount(
            avatarType = AvatarTypeDto.IMAGE,
            avatarImageUri = AvatarDto.PLACEHOLDER_IMAGE_URI,
        )

        val document = exportPreferences(false, setOf(accountUuid))
        val exported = document.rootElement.getChild("accounts").getChild("account")
        assertThat(exported.getChild("avatar-image")).isNull()
    }

    @Test
    fun exportPreferences_omitsAvatarImageForImageWithNullUri() = runTest {
        val accountUuid = saveAccount(avatarType = AvatarTypeDto.IMAGE)

        val document = exportPreferences(false, setOf(accountUuid))
        val exported = document.rootElement.getChild("accounts").getChild("account")
        assertThat(exported.getChild("avatar-image")).isNull()
    }

    @Test
    fun exportPreferences_omitsAvatarImageForIconAvatar() = runTest {
        val accountUuid = saveAccount(avatarType = AvatarTypeDto.ICON, avatarIconName = "star")

        val document = exportPreferences(false, setOf(accountUuid))
        val exported = document.rootElement.getChild("accounts").getChild("account")
        assertThat(exported.getChild("avatar-image")).isNull()
    }


    private fun exportPreferences(globalSettings: Boolean, accounts: Set<String>): Document = runBlocking {
        ByteArrayOutputStream().use { outputStream ->
            settingsExporter.exportPreferences(outputStream, globalSettings, accounts, includePasswords = false)
            parseXml(outputStream.toByteArray())
        }
    }

    private fun parseXml(xml: ByteArray): Document {
        return SAXBuilder().build(xml.inputStream())
    }

    private fun saveAccount(
        avatarType: AvatarTypeDto,
        avatarMonogram: String? = null,
        avatarImageUri: String? = null,
        avatarIconName: String? = null,
    ): String {
        val accountId = AccountIdFactory.create()
        val account = LegacyAccount(
            id = accountId,
            name = "Account",
            email = "user@example.com",
            profile = ProfileDto(
                id = accountId,
                name = "Account",
                color = 0,
                avatar = AvatarDto(
                    accountId = accountId,
                    avatarType = avatarType,
                    avatarMonogram = avatarMonogram,
                    avatarImageUri = avatarImageUri,
                    avatarIconName = avatarIconName,
                ),
            ),
            incomingServerSettings = SERVER_SETTINGS,
            outgoingServerSettings = SERVER_SETTINGS,
            identities = listOf(Identity(email = "user@example.com")),
        )
        accountManager.createSync(account)
        return accountId.toString()
    }

    companion object {
        private val SERVER_SETTINGS = ServerSettings(
            type = Protocols.IMAP,
            host = "irrelevant",
            port = 993,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "username",
            password = null,
            clientCertificateAlias = null,
        )
    }
}
