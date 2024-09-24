package app.k9mail.feature.navigation.drawer.domain.usecase

import app.k9mail.legacy.account.Account
import app.k9mail.legacy.message.controller.MessagingControllerMailChecker
import app.k9mail.legacy.message.controller.MessagingListener

internal class FakeMessagingControllerMailChecker(
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
