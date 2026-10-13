package com.fsck.k9.controller.push

import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.feature.account.AccountIdFactory
import org.junit.Assert.assertFalse
import org.junit.Test

class PushControllerTest {
    @Test
    fun `native API accounts are excluded without initializing their backend`() {
        val account = LegacyAccountDto(AccountIdFactory.create()).apply {
            incomingServerSettings = ServerSettings(
                type = Protocols.ZOHO_MAIL_API,
                host = "mail.invalid",
                port = 443,
                connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
                authenticationType = AuthType.XOAUTH2,
                username = "user@example.test",
                password = null,
                clientCertificateAlias = null,
            )
        }
        var backendWasRequested = false

        val result = isAccountPushCapable(account) {
            backendWasRequested = true
            true
        }

        assertFalse(result)
        assertFalse(backendWasRequested)
    }
}
