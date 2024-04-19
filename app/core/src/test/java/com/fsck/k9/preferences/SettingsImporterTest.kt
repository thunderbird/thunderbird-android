package com.fsck.k9.preferences

import android.content.Context
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
import com.fsck.k9.K9RobolectricTest
import com.fsck.k9.Preferences
import java.util.UUID
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment

class SettingsImporterTest : K9RobolectricTest() {
    private val context: Context = RuntimeEnvironment.getApplication()

    @Before
    fun before() {
        deletePreExistingAccounts()
    }

    private fun deletePreExistingAccounts() {
        val preferences = Preferences.getPreferences()
        preferences.clearAccounts()
    }

    @Test
    fun `importSettings() should throw on empty file`() {
        val inputStream = "".byteInputStream()
        val accountUuids = emptyList<String>()

        assertFailure {
            SettingsImporter.importSettings(context, inputStream, true, accountUuids)
        }.isInstanceOf<SettingsImportExportException>()
    }

    @Test
    fun `importSettings() should throw on missing format attribute`() {
        val inputStream = """<k9settings version="1"></k9settings>""".byteInputStream()
        val accountUuids = emptyList<String>()

        assertFailure {
            SettingsImporter.importSettings(context, inputStream, true, accountUuids)
        }.isInstanceOf<SettingsImportExportException>()
    }

    @Test
    fun `importSettings() should throw on invalid format attribute value`() {
        val inputStream = """<k9settings version="1" format="A"></k9settings>""".byteInputStream()
        val accountUuids = emptyList<String>()

        assertFailure {
            SettingsImporter.importSettings(context, inputStream, true, accountUuids)
        }.isInstanceOf<SettingsImportExportException>()
    }

    @Test
    fun `importSettings() should throw on invalid format version`() {
        val inputStream = """<k9settings version="1" format="0"></k9settings>""".byteInputStream()
        val accountUuids = emptyList<String>()

        assertFailure {
            SettingsImporter.importSettings(context, inputStream, true, accountUuids)
        }.isInstanceOf<SettingsImportExportException>()
    }

    @Test
    fun `importSettings() should throw on missing version attribute`() {
        val inputStream = """<k9settings format="1"></k9settings>""".byteInputStream()
        val accountUuids = emptyList<String>()

        assertFailure {
            SettingsImporter.importSettings(context, inputStream, true, accountUuids)
        }.isInstanceOf<SettingsImportExportException>()
    }

    @Test
    fun `importSettings() should throws on invalid version attribute value`() {
        val inputStream = """<k9settings format="1" version="A"></k9settings>""".byteInputStream()
        val accountUuids = emptyList<String>()

        assertFailure {
            SettingsImporter.importSettings(context, inputStream, true, accountUuids)
        }.isInstanceOf<SettingsImportExportException>()
    }

    @Test
    fun `importSettings() should throw on invalid version`() {
        val inputStream = """<k9settings format="1" version="0"></k9settings>""".byteInputStream()
        val accountUuids = emptyList<String>()

        assertFailure {
            SettingsImporter.importSettings(context, inputStream, true, accountUuids)
        }.isInstanceOf<SettingsImportExportException>()
    }

    @Test
    fun `importSettings() should disable accounts needing passwords`() {
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

        val results = SettingsImporter.importSettings(context, inputStream, true, accountUuids)

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

        val results = SettingsImporter.getImportStreamContents(inputStream)

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

        val results = SettingsImporter.getImportStreamContents(inputStream)

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
}
