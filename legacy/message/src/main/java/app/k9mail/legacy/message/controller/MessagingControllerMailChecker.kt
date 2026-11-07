package app.k9mail.legacy.message.controller

import net.thunderbird.feature.account.AccountId

interface MessagingControllerMailChecker {
    fun checkMail(
        accountId: AccountId?,
        ignoreLastCheckedTime: Boolean,
        useManualWakeLock: Boolean,
        notify: Boolean,
        listener: MessagingListener?,
    )
}
