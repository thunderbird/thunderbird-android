package app.k9mail.feature.account.common.domain.entity

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.AuthType
import org.junit.Test

class AuthenticationTypeTest {

    @Test
    fun `should map all AuthenticationType to AuthTypes`() {
        val types = AuthenticationType.entries

        for (type in types) {
            val authType = type.toAuthType()

            assertThat(authType).isEqualTo(
                when (type) {
                    AuthenticationType.PasswordCleartext -> AuthType.PLAIN
                    AuthenticationType.PasswordEncrypted -> AuthType.CRAM_MD5
                    AuthenticationType.OAuth2 -> AuthType.XOAUTH2
                    AuthenticationType.ClientCertificate -> AuthType.EXTERNAL
                    AuthenticationType.None -> AuthType.NONE
                },
            )
        }
    }

    @Test
    fun `should map all AuthTypes to AuthenticationTypes`() {
        val types = AuthType.entries

        for (type in types) {
            val authenticationType = type.toAuthenticationType()

            assertThat(authenticationType).isEqualTo(
                when (type) {
                    AuthType.PLAIN -> AuthenticationType.PasswordCleartext
                    AuthType.CRAM_MD5 -> AuthenticationType.PasswordEncrypted
                    AuthType.EXTERNAL -> AuthenticationType.ClientCertificate
                    AuthType.XOAUTH2 -> AuthenticationType.OAuth2
                    AuthType.NONE -> AuthenticationType.None
                },
            )
        }
    }
}
