package com.fsck.k9.preferences

import app.k9mail.core.android.testing.RobolectricTest
import assertk.all
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.extracting
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.key
import assertk.assertions.prop
import com.fsck.k9.mail.AuthType
import java.util.UUID
import org.junit.Test

class SettingsFileParserTest : RobolectricTest() {
    private val parser = SettingsFileParser()

    @Test
    fun `parseSettings() should return accounts`() {
        val accountUuid = UUID.randomUUID().toString()
        val inputStream =
            """
            <k9settings format="1" version="1">
              <accounts>
                <account uuid="$accountUuid">
                  <name>Account</name>
                </account>
              </accounts>
            </k9settings>
            """.trimIndent().byteInputStream()
        val accountUuids = listOf("1")

        val results = parser.parseSettings(inputStream, true, accountUuids, true)

        assertThat(results.accounts).isNotNull().all {
            hasSize(1)
            key(accountUuid).all {
                prop(ImportedAccount::uuid).isEqualTo(accountUuid)
                prop(ImportedAccount::name).isEqualTo("Account")
            }
        }
    }

    @Test
    fun `parseSettings() should return identities`() {
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
        val accountUuids = listOf("1")

        val results = parser.parseSettings(inputStream, true, accountUuids, true)

        assertThat(results.accounts).isNotNull().all {
            hasSize(1)
            key(accountUuid).all {
                prop(ImportedAccount::uuid).isEqualTo(accountUuid)
                prop(ImportedAccount::identities).isNotNull()
                    .extracting(ImportedIdentity::email).containsExactly("user@gmail.com")
            }
        }
    }

    @Test
    fun `parseSettings() should parse incoming server authentication type`() {
        val accountUuid = UUID.randomUUID().toString()
        val inputStream =
            """
            <k9settings format="1" version="1">
              <accounts>
                <account uuid="$accountUuid">
                  <name>Account</name>
                  <incoming-server>
                    <authentication-type>CRAM_MD5</authentication-type>
                  </incoming-server>
                </account>
              </accounts>
            </k9settings>
            """.trimIndent().byteInputStream()
        val accountUuids = listOf(accountUuid)

        val results = parser.parseSettings(inputStream, true, accountUuids, false)

        assertThat(results.accounts)
            .isNotNull()
            .key(accountUuid)
            .prop(ImportedAccount::incoming)
            .isNotNull()
            .prop(ImportedServer::authenticationType)
            .isEqualTo(AuthType.CRAM_MD5)
    }
}
