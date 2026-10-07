package net.thunderbird.feature.navigation.drawer.dropdown.domain.usecase

import app.k9mail.legacy.message.controller.MessagingListener
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.thunderbird.feature.account.AccountIdFactory

internal class SyncAccountTest {

    @Test
    fun `should sync mail with account`() = runTest {
        val listenerExecutor: (MessagingListener?) -> Unit = { listener ->
            listener?.checkMailFinished(null, null)
        }
        val accountId = AccountIdFactory.create()
        val messagingController = FakeMessagingControllerMailChecker(
            listenerExecutor = listenerExecutor,
        )
        val testSubject = SyncAccount(
            messagingController = messagingController,
        )

        val result = testSubject(accountId).first()

        assertThat(result.isSuccess).isEqualTo(true)
        assertThat(messagingController.recordedParameters).isEqualTo(
            listOf(
                CheckMailParameters(
                    accountId = accountId,
                    ignoreLastCheckedTime = true,
                    useManualWakeLock = true,
                    notify = true,
                ),
            ),
        )
    }
}
