package app.k9mail.feature.account.edit.ui.server.settings.save

import assertk.assertThat
import assertk.assertions.isTrue
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import org.junit.Test

class SaveIncomingServerSettingsViewModelTest {

    @Test
    fun `should set is incoming to true`() {
        val testSubject = SaveIncomingServerSettingsViewModel(
            accountId = AccountIdFactory.create(),
            saveServerSettings = { _, _ -> },
        )

        assertThat(testSubject.isIncoming).isTrue()
    }
}
