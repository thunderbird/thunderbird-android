package app.k9mail.feature.navigation.drawer.domain.usecase

import app.k9mail.legacy.message.controller.MessagingListener
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

internal class SyncAllAccountsTest {

    @Test
    fun `should sync mail`() = runTest {
        val listenerExecutor: (MessagingListener?) -> Unit = { listener ->
            listener?.checkMailFinished(null, null)
        }
        val messagingController = FakeMessagingControllerMailChecker(
            listenerExecutor = listenerExecutor,
        )
        val testSubject = SyncAllAccounts(
            messagingController = messagingController,
        )

        val result = testSubject().first()

        assertThat(result.isSuccess).isEqualTo(true)
        assertThat(messagingController.recordedParameters).isEqualTo(
            listOf(
                CheckMailParameters(
                    account = null,
                    ignoreLastCheckedTime = true,
                    useManualWakeLock = true,
                    notify = true,
                ),
            ),
        )
    }
}
