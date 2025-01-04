package app.k9mail.feature.account.setup.ui.options.display

import app.k9mail.feature.account.common.domain.entity.AccountDisplayOptions
import app.k9mail.feature.account.common.domain.input.StringInputField
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.junit.Test

class DisplayOptionsStateMapperKtTest {

    @Test
    fun `should map state to account options`() {
        val state = DisplayOptionsContract.State(
            accountName = StringInputField("accountName"),
            displayName = StringInputField("displayName"),
            emailSignature = StringInputField("emailSignature"),
            showInUnifiedInbox = true,
        )

        val result = state.toAccountDisplayOptions()

        assertThat(result).isEqualTo(
            AccountDisplayOptions(
                accountName = "accountName",
                displayName = "displayName",
                emailSignature = "emailSignature",
                showInUnifiedInbox = true,
            ),
        )
    }

    @Test
    fun `empty signature should map to null`() {
        val state = DisplayOptionsContract.State(emailSignature = StringInputField(""))

        val result = state.toAccountDisplayOptions()

        assertThat(result.emailSignature).isNull()
    }
}
