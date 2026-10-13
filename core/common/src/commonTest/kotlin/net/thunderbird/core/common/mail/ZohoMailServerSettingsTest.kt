package net.thunderbird.core.common.mail

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class ZohoMailServerSettingsTest {
    @Test
    fun `OAuth scopes should cover required Zoho Mail operations with least privilege`() {
        assertThat(ZohoMailServerSettings.OAUTH_SCOPES).isEqualTo(
            listOf(
                "ZohoMail.accounts.READ",
                "ZohoMail.folders.READ",
                "ZohoMail.messages.READ",
                "ZohoMail.messages.CREATE",
                "ZohoMail.messages.UPDATE",
                "ZohoMail.messages.DELETE",
                "ZohoMail.folders.UPDATE",
            ),
        )
        assertThat(ZohoMailServerSettings.SCOPE_SEPARATOR).isEqualTo(",")
    }
}
