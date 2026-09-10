package com.fsck.k9.preferences

import android.util.Base64
import androidx.core.net.toUri
import app.k9mail.legacy.mailstore.FolderRepository
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.fsck.k9.K9RobolectricTest
import com.fsck.k9.Preferences
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.test.runTest
import net.thunderbird.feature.mail.folder.api.data.repository.FolderQueryRepository
import kotlinx.coroutines.runBlocking
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import org.jdom2.Document
import org.jdom2.input.SAXBuilder
import org.junit.Test
import org.koin.core.component.inject
import org.mockito.kotlin.mock
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

class SettingsExporterTest : K9RobolectricTest() {
    private val contentResolver = RuntimeEnvironment.getApplication().contentResolver
    private val preferences: Preferences by inject()
    private val folderSettingsProvider: FolderSettingsProvider by inject()
    private val folderQueryRepository: FolderQueryRepository by inject()
    private val settingsExporter = SettingsExporter(
        contentResolver,
        preferences,
        folderSettingsProvider,
        folderQueryRepository,
        notificationSettingsUpdater = mock(),
        filePrefixProvider = mock(),
    )

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
    fun exportPreferences_includesAvatarImageForImageAvatar() =runTest{
        val avatarUri = "content://test/avatar".toUri()
        val imageBytes = byteArrayOf(1, 2, 3, 4)
        val expectedEncoded = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

        shadowOf(contentResolver).registerInputStream(avatarUri, imageBytes.inputStream())
        val mockAccount =preferences.newAccount().apply {
            incomingServerSettings = SERVER_SETTINGS
            outgoingServerSettings = SERVER_SETTINGS
            avatar = AvatarDto(
                avatarType = AvatarTypeDto.IMAGE,
                avatarMonogram = null,
                avatarImageUri = avatarUri.toString(),
                avatarIconName = null,
            )
        }
        preferences.saveAccount(mockAccount)

        val document = exportPreferences(false, setOf(mockAccount.uuid))

        val account = document.rootElement.getChild("accounts").getChild("account")
        val avatarImage = account.getChild("avatar-image")
        assertThat(avatarImage).isNotNull()
        assertThat(avatarImage.text).isEqualTo(expectedEncoded)
    }

    @Test
    fun exportPreferences_omitsAvatarImageForMonogramAvatar() =runTest{
        val account = preferences.newAccount().apply {
            incomingServerSettings = SERVER_SETTINGS
            outgoingServerSettings = SERVER_SETTINGS
            avatar = AvatarDto(AvatarTypeDto.MONOGRAM, "XX", null, null)
        }
        preferences.saveAccount(account)

        val document = exportPreferences(false, setOf(account.uuid))
        val exported = document.rootElement.getChild("accounts").getChild("account")
        assertThat(exported.getChild("avatar-image")).isNull()
    }


    private suspend fun exportPreferences(globalSettings: Boolean, accounts: Set<String>): Document = runBlocking {
        ByteArrayOutputStream().use { outputStream ->
            settingsExporter.exportPreferences(outputStream, globalSettings, accounts, includePasswords = false)
            parseXml(outputStream.toByteArray())
        }
    }

    private fun parseXml(xml: ByteArray): Document {
        return SAXBuilder().build(xml.inputStream())
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
