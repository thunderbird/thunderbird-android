package com.fsck.k9.preferences

import assertk.all
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.extracting
import assertk.assertions.hasSize
import assertk.assertions.index
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.prop
import com.fsck.k9.preferences.SettingsFile.Account
import com.fsck.k9.preferences.SettingsFile.Identity
import com.fsck.k9.preferences.SettingsFile.Server
import java.util.UUID
import net.thunderbird.core.android.testing.RobolectricTest
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

        val results = parser.parseSettings(inputStream)

        assertThat(results.accounts).isNotNull().all {
            hasSize(1)
            index(0).all {
                prop(Account::uuid).isEqualTo(accountUuid)
                prop(Account::name).isEqualTo("Account")
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

        val results = parser.parseSettings(inputStream)

        assertThat(results.accounts).isNotNull().all {
            hasSize(1)
            index(0).all {
                prop(Account::uuid).isEqualTo(accountUuid)
                prop(Account::identities).isNotNull()
                    .extracting(Identity::email).containsExactly("user@gmail.com")
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

        val results = parser.parseSettings(inputStream)

        assertThat(results.accounts)
            .isNotNull()
            .index(0)
            .prop(Account::incoming)
            .isNotNull()
            .prop(Server::authenticationType)
            .isEqualTo("CRAM_MD5")
    }
}
