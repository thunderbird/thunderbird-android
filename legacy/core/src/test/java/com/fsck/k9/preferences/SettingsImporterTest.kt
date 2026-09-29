package com.fsck.k9.preferences

import app.k9mail.legacy.di.DI.get
import assertk.all
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.first
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import assertk.assertions.prop
import com.eygraber.uri.Uri
import com.fsck.k9.K9RobolectricTest
import com.fsck.k9.Preferences
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.thunderbird.feature.account.avatar.AvatarImageRepository
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SettingsImporterTest : K9RobolectricTest() {
    private val unifiedInboxConfigurator = mock<UnifiedInboxConfigurator>()
    private val settingsImporter = SettingsImporter(
        settingsFileParser = get(),
        generalSettingsValidator = get(),
        accountSettingsValidator = get(),
        generalSettingsUpgrader = get(),
        accountSettingsWriter = get(),
        accountSettingsUpgrader = get(),
        generalSettingsWriter = get(),
        unifiedInboxConfigurator = unifiedInboxConfigurator,
    )

    @Before
    fun before() {
        deletePreExistingAccounts()
    }

    private fun deletePreExistingAccounts() {
        val preferences = Preferences.getPreferences()
        preferences.clearAccounts()
    }

    @Test
    fun `importSettings() should throw on empty file`() = runTest {
        val inputStream = "".byteInputStream()
        val accountUuids = emptyList<String>()

        assertFailure {
            settingsImporter.importSettings(inputStream, globalSettings = true, accountUuids)
        }.isInstanceOf<SettingsImportExportException>()
    }

    @Test
    fun `importSettings() should throw on missing format attribute`() = runTest {
        val inputStream = """<k9settings version="1"></k9settings>""".byteInputStream()
        val accountUuids = emptyList<String>()

        assertFailure {
            settingsImporter.importSettings(inputStream, globalSettings = true, accountUuids)
        }.isInstanceOf<SettingsImportExportException>()
    }

    @Test
    fun `importSettings() should throw on invalid format attribute value`() = runTest {
        val inputStream = """<k9settings version="1" format="A"></k9settings>""".byteInputStream()
        val accountUuids = emptyList<String>()

        assertFailure {
            settingsImporter.importSettings(inputStream, globalSettings = true, accountUuids)
        }.isInstanceOf<SettingsImportExportException>()
    }

    @Test
    fun `importSettings() should throw on invalid format version`() = runTest {
        val inputStream = """<k9settings version="1" format="0"></k9settings>""".byteInputStream()
        val accountUuids = emptyList<String>()

        assertFailure {
            settingsImporter.importSettings(inputStream, globalSettings = true, accountUuids)
        }.isInstanceOf<SettingsImportExportException>()
    }

    @Test
    fun `importSettings() should throw on missing version attribute`() = runTest {
        val inputStream = """<k9settings format="1"></k9settings>""".byteInputStream()
        val accountUuids = emptyList<String>()

        assertFailure {
            settingsImporter.importSettings(inputStream, globalSettings = true, accountUuids)
        }.isInstanceOf<SettingsImportExportException>()
    }

    @Test
    fun `importSettings() should throws on invalid version attribute value`() = runTest {
        val inputStream = """<k9settings format="1" version="A"></k9settings>""".byteInputStream()
        val accountUuids = emptyList<String>()

        assertFailure {
            settingsImporter.importSettings(inputStream, globalSettings = true, accountUuids)
        }.isInstanceOf<SettingsImportExportException>()
    }

    @Test
    fun `importSettings() should throw on invalid version`() = runTest {
        val inputStream = """<k9settings format="1" version="0"></k9settings>""".byteInputStream()
        val accountUuids = emptyList<String>()

        assertFailure {
            settingsImporter.importSettings(inputStream, globalSettings = true, accountUuids)
        }.isInstanceOf<SettingsImportExportException>()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `importSettings() should disable accounts needing passwords`() = runTest(UnconfinedTestDispatcher()) {
        val accountUuid = UUID.randomUUID().toString()
        val inputStream =
            """
            <k9settings format="1" version="1">
              <accounts>
                <account uuid="$accountUuid">
                  <name>Account</name>
                  <incoming-server type="IMAP">
                    <connection-security>SSL_TLS_REQUIRED</connection-security>
                    <username>user@gmail.com</username>
                    <authentication-type>CRAM_MD5</authentication-type>
                    <host>googlemail.com</host>
                  </incoming-server>
                  <outgoing-server type="SMTP">
                    <connection-security>SSL_TLS_REQUIRED</connection-security>
                    <username>user@googlemail.com</username>
                    <authentication-type>CRAM_MD5</authentication-type>
                    <host>googlemail.com</host>
                  </outgoing-server>
                  <settings>
                    <value key="a">b</value>
                  </settings>
                  <identities>
                    <identity>
                      <email>user@gmail.com</email>
                    </identity>
                  </identities>
                </account>
              </accounts>
            </k9settings>
            """.trimIndent().byteInputStream()
        val accountUuids = listOf(accountUuid)

        val results = settingsImporter.importSettings(inputStream, globalSettings = true, accountUuids)

        assertThat(results).all {
            prop(ImportResults::erroneousAccounts).isEmpty()
            prop(ImportResults::importedAccounts).all {
                hasSize(1)
                first().all {
                    prop(AccountDescriptionPair::imported).all {
                        prop(AccountDescription::uuid).isEqualTo(accountUuid)
                        prop(AccountDescription::name).isEqualTo("Account")
                    }
                    prop(AccountDescriptionPair::incomingPasswordNeeded).isTrue()
                    prop(AccountDescriptionPair::outgoingPasswordNeeded).isTrue()
                }
            }
        }
    }

    @Test
    fun `importSettings()  configures unifiedInbox when globalSettingsImported is false`() = runTest {
        val accountUuid = UUID.randomUUID().toString()
        val inputStream =
            """
            <k9settings format="1" version="1">
              <accounts>
                <account uuid="$accountUuid">
                  <name>Account</name>
                  <incoming-server type="IMAP">
                    <connection-security>SSL_TLS_REQUIRED</connection-security>
                    <username>user@gmail.com</username>
                    <authentication-type>CRAM_MD5</authentication-type>
                    <host>googlemail.com</host>
                  </incoming-server>
                  <outgoing-server type="SMTP">
                    <connection-security>SSL_TLS_REQUIRED</connection-security>
                    <username>user@googlemail.com</username>
                    <authentication-type>CRAM_MD5</authentication-type>
                    <host>googlemail.com</host>
                  </outgoing-server>
                  <settings>
                    <value key="a">b</value>
                  </settings>
                  <identities>
                    <identity>
                      <email>user@gmail.com</email>
                    </identity>
                  </identities>
                </account>
              </accounts>
            </k9settings>
            """.trimIndent().byteInputStream()
        val accountUuids = listOf("uuid-1")

        val results = settingsImporter.importSettings(inputStream, globalSettings = false, accountUuids)

        assertThat(results.globalSettings).isFalse()
        verify(unifiedInboxConfigurator, times(1)).configureUnifiedInbox()
    }

    @Test
    fun `importSettings()  does not not configure unifiedInbox when globalSettingsImported is true`() = runTest {
        val accountUuid = UUID.randomUUID().toString()
        val inputStream =
            """
            <k9settings format="1" version="101">
            <global>
                <value key="confirmDelete">false</value>
                <value key="changeRegisteredNameColor">false</value>
                <value key="confirmSpam">false</value>
              </global>
              <accounts>
                <account uuid="$accountUuid">
                  <name>Account</name>
                  <incoming-server type="IMAP">
                    <connection-security>SSL_TLS_REQUIRED</connection-security>
                    <username>user@gmail.com</username>
                    <authentication-type>CRAM_MD5</authentication-type>
                    <host>googlemail.com</host>
                  </incoming-server>
                  <outgoing-server type="SMTP">
                    <connection-security>SSL_TLS_REQUIRED</connection-security>
                    <username>user@googlemail.com</username>
                    <authentication-type>CRAM_MD5</authentication-type>
                    <host>googlemail.com</host>
                  </outgoing-server>
                  <settings>
                    <value key="a">b</value>
                  </settings>
                  <identities>
                    <identity>
                      <email>user@gmail.com</email>
                    </identity>
                  </identities>
                </account>
              </accounts>
            </k9settings>
            """.trimIndent().byteInputStream()
        val accountUuids = listOf("uuid-1")

        val results = settingsImporter.importSettings(inputStream, globalSettings = true, accountUuids)

        assertThat(results.globalSettings).isTrue()
        verify(unifiedInboxConfigurator, never()).configureUnifiedInbox()
    }

    @Test
    fun `getImportStreamContents() should return list of accounts`() {
        val accountUuid = UUID.randomUUID().toString()
        val inputStream =
            """
            <k9settings format="1" version="1">
              <accounts>
                <account uuid="$accountUuid">
                  <name>Account</name>
                  <identities>
                    <identity>
                      <email>user@gmail.com</email>
                    </identity>
                  </identities>
                </account>
              </accounts>
            </k9settings>
            """.trimIndent().byteInputStream()

        val results = settingsImporter.getImportStreamContents(inputStream)

        assertThat(results).all {
            prop(ImportContents::globalSettings).isFalse()
            prop(ImportContents::accounts).all {
                hasSize(1)
                first().all {
                    prop(AccountDescription::uuid).isEqualTo(accountUuid)
                    prop(AccountDescription::name).isEqualTo("Account")
                }
            }
        }
    }

    @Test
    fun `getImportStreamContents() should return email address as account name when no account name provided`() {
        val accountUuid = UUID.randomUUID().toString()
        val inputStream =
            """
            <k9settings format="1" version="1">
              <accounts>
                <account uuid="$accountUuid">
                  <name></name>
                  <identities>
                    <identity>
                      <email>user@gmail.com</email>
                    </identity>
                  </identities>
                </account>
              </accounts>
            </k9settings>
            """.trimIndent().byteInputStream()

        val results = settingsImporter.getImportStreamContents(inputStream)

        assertThat(results).all {
            prop(ImportContents::globalSettings).isFalse()
            prop(ImportContents::accounts).all {
                hasSize(1)
                first().all {
                    prop(AccountDescription::uuid).isEqualTo(accountUuid)
                    prop(AccountDescription::name).isEqualTo("user@gmail.com")
                }
            }
        }
    }

    @Test
    fun `getImportStreamContents() should throw when no setting is present in inputStream`() {
        val inputStream =
            """
            <k9settings format="1" version="1">
              <accounts>
              </accounts>
            </k9settings>
            """.trimIndent().byteInputStream()

        assertFailure {
            settingsImporter.getImportStreamContents(inputStream)
        }.isInstanceOf<SettingsImportExportException>()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `importSettings() should import avatar image`() = runTest(UnconfinedTestDispatcher()) {
        val tinyAvatar =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
        val newAvatarUri = Uri.parse("file:///data/account_avatars/imported.png")
        whenever { get<AvatarImageRepository>().update(any(), any()) }.thenReturn(newAvatarUri)

        val accountUuid = UUID.randomUUID().toString()
        val inputStream = avatarAccountXml(
            accountUuid = accountUuid,
            settingsValues = """<value key="avatarType">IMAGE</value>""",
            avatarImageElement = "<avatar-image>$tinyAvatar</avatar-image>",
        ).byteInputStream()

        val results = settingsImporter.importSettings(inputStream, globalSettings = false, listOf(accountUuid))

        assertThat(results.erroneousAccounts).isEmpty()
        assertThat(results.importedAccounts).hasSize(1)

        val account = importedAccount(results)
        assertThat(account.avatar.avatarType).isEqualTo(AvatarTypeDto.IMAGE)
        assertThat(account.avatar.avatarImageUri).isEqualTo(newAvatarUri.toString())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `importSettings() should keep placeholder uri for image avatar without image`() =
        runTest(UnconfinedTestDispatcher()) {
            stubAvatarDecoding()

            val accountUuid = UUID.randomUUID().toString()
            val inputStream = avatarAccountXml(
                accountUuid = accountUuid,
                settingsValues = """
                    <value key="avatarType">IMAGE</value>
                    <value key="avatarImageUri">${AvatarDto.PLACEHOLDER_IMAGE_URI}</value>
                """.trimIndent(),
            ).byteInputStream()

            val results = settingsImporter.importSettings(inputStream, globalSettings = false, listOf(accountUuid))

            assertThat(results.erroneousAccounts).isEmpty()
            assertThat(results.importedAccounts).hasSize(1)

            val account = importedAccount(results)
            assertThat(account.avatar.avatarType).isEqualTo(AvatarTypeDto.IMAGE)
            assertThat(account.avatar.avatarImageUri).isEqualTo(AvatarDto.PLACEHOLDER_IMAGE_URI)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `importSettings() should not set avatar uri for image avatar with null uri`() =
        runTest(UnconfinedTestDispatcher()) {
            stubAvatarDecoding()

            val accountUuid = UUID.randomUUID().toString()
            val inputStream = avatarAccountXml(
                accountUuid = accountUuid,
                settingsValues = """<value key="avatarType">IMAGE</value>""",
            ).byteInputStream()

            val results = settingsImporter.importSettings(inputStream, globalSettings = false, listOf(accountUuid))

            assertThat(results.erroneousAccounts).isEmpty()
            assertThat(results.importedAccounts).hasSize(1)

            val account = importedAccount(results)
            assertThat(account.avatar.avatarType).isEqualTo(AvatarTypeDto.IMAGE)
            assertThat(account.avatar.avatarImageUri).isEqualTo(null)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `importSettings() should not decode avatar image for monogram avatar`() =
        runTest(UnconfinedTestDispatcher()) {
            stubAvatarDecoding()

            val accountUuid = UUID.randomUUID().toString()
            val inputStream = avatarAccountXml(
                accountUuid = accountUuid,
                settingsValues = """
                    <value key="avatarType">MONOGRAM</value>
                    <value key="avatarMonogram">AB</value>
                """.trimIndent(),
            ).byteInputStream()

            val results = settingsImporter.importSettings(inputStream, globalSettings = false, listOf(accountUuid))

            assertThat(results.erroneousAccounts).isEmpty()
            assertThat(results.importedAccounts).hasSize(1)

            val account = importedAccount(results)
            assertThat(account.avatar.avatarType).isEqualTo(AvatarTypeDto.MONOGRAM)
            assertThat(account.avatar.avatarMonogram).isEqualTo("AB")
            assertThat(account.avatar.avatarImageUri).isEqualTo(null)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `importSettings() should not decode avatar image for icon avatar`() =
        runTest(UnconfinedTestDispatcher()) {
            stubAvatarDecoding()

            val accountUuid = UUID.randomUUID().toString()
            val inputStream = avatarAccountXml(
                accountUuid = accountUuid,
                settingsValues = """
                    <value key="avatarType">ICON</value>
                    <value key="avatarIconName">star</value>
                """.trimIndent(),
            ).byteInputStream()

            val results = settingsImporter.importSettings(inputStream, globalSettings = false, listOf(accountUuid))

            assertThat(results.erroneousAccounts).isEmpty()
            assertThat(results.importedAccounts).hasSize(1)

            val account = importedAccount(results)
            assertThat(account.avatar.avatarType).isEqualTo(AvatarTypeDto.ICON)
            assertThat(account.avatar.avatarIconName).isEqualTo("star")
            assertThat(account.avatar.avatarImageUri).isEqualTo(null)
        }

    /**
     * Stubs avatar decoding so that if it were (wrongly) triggered, the resulting uri would differ from the
     * expected value, making the test fail instead of silently passing.
     */
    private fun stubAvatarDecoding() {
        val decodedUri = Uri.parse("file:///data/account_avatars/should_not_be_used.png")
        whenever { get<AvatarImageRepository>().update(any(), any()) }.thenReturn(decodedUri)
    }

    private fun importedAccount(results: ImportResults) =
        Preferences.getPreferences().getAccount(results.importedAccounts.first().imported.uuid)!!

    private fun avatarAccountXml(
        accountUuid: String,
        settingsValues: String,
        avatarImageElement: String = "",
    ): String =
        """
        <k9settings format="1" version="${Settings.VERSION}">
          <accounts>
            <account uuid="$accountUuid">
              <name>Account</name>
              <incoming-server type="IMAP">
                <connection-security>SSL_TLS_REQUIRED</connection-security>
                <username>user@gmail.com</username>
                <authentication-type>PLAIN</authentication-type>
                <host>imap.gmail.com</host>
              </incoming-server>
              <outgoing-server type="SMTP">
                <connection-security>SSL_TLS_REQUIRED</connection-security>
                <username>user@gmail.com</username>
                <authentication-type>PLAIN</authentication-type>
                <host>smtp.gmail.com</host>
              </outgoing-server>
              <settings>
                $settingsValues
              </settings>
              <identities>
                <identity>
                  <email>user@gmail.com</email>
                </identity>
              </identities>
              $avatarImageElement
            </account>
          </accounts>
        </k9settings>
        """.trimIndent()
}
