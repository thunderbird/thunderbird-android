package app.k9mail.feature.navigation.drawer.domain.usecase

import app.k9mail.legacy.message.controller.MessagingListener
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class SyncAllAccountsTest {

    @Test
    fun `should sync mail`() = runTest {
        val listenerExecutor: (MessagingListener?) -> Unit = { listener ->
            listener?.checkMailFinished(null, null)
        }
        val testSubject = SyncAllAccounts(
            messagingController = FakeMessagingControllerMailChecker(
                listenerExecutor = listenerExecutor,
            ),
        )

        val result = testSubject().first()

        assertk.assertThat(result.isSuccess).isEqualTo(true)
    }
}
