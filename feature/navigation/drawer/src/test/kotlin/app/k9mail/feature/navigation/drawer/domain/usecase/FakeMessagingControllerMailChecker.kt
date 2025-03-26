package app.k9mail.feature.navigation.drawer.domain.usecase

import app.k9mail.legacy.account.LegacyAccount
import app.k9mail.legacy.message.controller.MessagingControllerMailChecker
import app.k9mail.legacy.message.controller.MessagingListener

internal class FakeMessagingControllerMailChecker(
    val recordedParameters: MutableList<CheckMailParameters> = mutableListOf(),
    private val listenerExecutor: (MessagingListener?) -> Unit = {},
) : MessagingControllerMailChecker {
    override fun checkMail(
        account: LegacyAccount?,
        ignoreLastCheckedTime: Boolean,
        useManualWakeLock: Boolean,
        notify: Boolean,
        listener: MessagingListener?,
    ) {
        recordedParameters.add(CheckMailParameters(account, ignoreLastCheckedTime, useManualWakeLock, notify))

        listenerExecutor(listener)
    }
}

internal data class CheckMailParameters(
    val account: LegacyAccount?,
    val ignoreLastCheckedTime: Boolean,
    val useManualWakeLock: Boolean,
    val notify: Boolean,
)
