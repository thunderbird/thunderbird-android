package net.thunderbird.feature.navigation.drawer.siderail.domain.usecase

import app.k9mail.legacy.message.controller.MessagingListener
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.thunderbird.feature.navigation.drawer.siderail.ui.FakeData

internal class SyncAccountTest {

    @Test
    fun `should sync mail with account`() = runTest {
        val listenerExecutor: (MessagingListener?) -> Unit = { listener ->
            listener?.checkMailFinished(null, null)
        }
        val account = FakeData.ACCOUNT
        val accountManager = FakeLegacyAccountDtoManager(
            accounts = listOf(account),
        )
        val messagingController = FakeMessagingControllerMailChecker(
            listenerExecutor = listenerExecutor,
        )
        val testSubject = SyncAccount(
            accountManager = accountManager,
            messagingController = messagingController,
        )

        val result = testSubject(account.uuid).first()

        assertThat(result.isSuccess).isEqualTo(true)
        assertThat(accountManager.recordedParameters).isEqualTo(listOf(account.uuid))
        assertThat(messagingController.recordedParameters).isEqualTo(
            listOf(
                CheckMailParameters(
                    account = account,
                    ignoreLastCheckedTime = true,
                    useManualWakeLock = true,
                    notify = true,
                ),
            ),
        )
    }
}
