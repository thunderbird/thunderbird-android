package net.thunderbird.feature.navigation.drawer.dropdown.domain.usecase

import app.k9mail.legacy.message.controller.MessagingControllerMailChecker
import app.k9mail.legacy.message.controller.MessagingListener
import net.thunderbird.feature.account.AccountId

internal class FakeMessagingControllerMailChecker(
    val recordedParameters: MutableList<CheckMailParameters> = mutableListOf(),
    private val listenerExecutor: (MessagingListener?) -> Unit = {},
) : MessagingControllerMailChecker {
    override fun checkMail(
        accountId: AccountId?,
        ignoreLastCheckedTime: Boolean,
        useManualWakeLock: Boolean,
        notify: Boolean,
        listener: MessagingListener?,
    ) {
        recordedParameters.add(CheckMailParameters(accountId, ignoreLastCheckedTime, useManualWakeLock, notify))

        listenerExecutor(listener)
    }
}

internal data class CheckMailParameters(
    val accountId: AccountId?,
    val ignoreLastCheckedTime: Boolean,
    val useManualWakeLock: Boolean,
    val notify: Boolean,
)
