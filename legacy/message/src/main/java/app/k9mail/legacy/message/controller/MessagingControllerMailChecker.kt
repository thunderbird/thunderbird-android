package app.k9mail.legacy.message.controller

import net.thunderbird.core.android.account.LegacyAccountDto

interface MessagingControllerMailChecker {
    fun checkMail(
        account: LegacyAccountDto?,
        ignoreLastCheckedTime: Boolean,
        useManualWakeLock: Boolean,
        notify: Boolean,
        listener: MessagingListener?,
    )
}
