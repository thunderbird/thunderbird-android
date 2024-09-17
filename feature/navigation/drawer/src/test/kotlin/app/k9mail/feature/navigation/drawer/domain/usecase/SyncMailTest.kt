package app.k9mail.feature.navigation.drawer.domain.usecase

import app.k9mail.legacy.account.Account
import app.k9mail.legacy.message.controller.MessagingControllerMailChecker
import app.k9mail.legacy.message.controller.MessagingListener
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SyncMailTest {

    @Test
    fun `should sync mail`() = runTest {
        val listenerExecutor: (MessagingListener?) -> Unit = { listener ->
            listener?.checkMailFinished(null, null)
        }
        val testSubject = SyncMail(
            messagingController = FakeMessagingControllerMailChecker(
                listenerExecutor = listenerExecutor,
            ),
        )

        val result = testSubject(null).first()

        assertThat(result.isSuccess).isEqualTo(true)
    }

    private class FakeMessagingControllerMailChecker(
        private val listenerExecutor: (MessagingListener?) -> Unit = {},
    ) : MessagingControllerMailChecker {
        override fun checkMail(
            account: Account?,
            ignoreLastCheckedTime: Boolean,
            useManualWakeLock: Boolean,
            notify: Boolean,
            listener: MessagingListener?,
        ) {
            listenerExecutor(listener)
        }
    }
}
