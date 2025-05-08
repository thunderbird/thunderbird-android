package app.k9mail.legacy.message.controller

import net.thunderbird.core.android.account.LegacyAccount

interface MessagingControllerMailChecker {
    fun checkMail(
        account: LegacyAccount?,
        ignoreLastCheckedTime: Boolean,
        useManualWakeLock: Boolean,
        notify: Boolean,
        listener: MessagingListener?,
    )
}
