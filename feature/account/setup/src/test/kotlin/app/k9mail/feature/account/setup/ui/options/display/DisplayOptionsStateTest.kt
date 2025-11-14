package app.k9mail.feature.account.setup.ui.options.display

import app.k9mail.feature.account.setup.ui.options.display.DisplayOptionsContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import net.thunderbird.core.validation.input.StringInputField
import org.junit.Test

class DisplayOptionsStateTest {

    @Test
    fun `should set default values`() {
        val state = State()

        assertThat(state).isEqualTo(
            State(
                accountName = StringInputField(),
                displayName = StringInputField(),
                emailSignature = StringInputField(),
            ),
        )
    }
}
