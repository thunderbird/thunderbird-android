package app.k9mail.feature.account.edit.ui.server.settings.save

import assertk.assertThat
import assertk.assertions.isFalse
import net.thunderbird.feature.account.AccountIdFactory
import org.junit.Test

class SaveOutgoingServerSettingsViewModelTest {

    @Test
    fun `should set is incoming to true`() {
        val testSubject = SaveOutgoingServerSettingsViewModel(
            accountId = AccountIdFactory.create(),
            saveServerSettings = { _, _ -> },
        )

        assertThat(testSubject.isIncoming).isFalse()
    }
}
