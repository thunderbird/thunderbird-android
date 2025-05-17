package net.thunderbird.feature.navigation.drawer.dropdown.domain.usecase

import app.k9mail.legacy.message.controller.MessagingControllerMailChecker
import app.k9mail.legacy.message.controller.MessagingListener
import net.thunderbird.core.android.account.LegacyAccount

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
