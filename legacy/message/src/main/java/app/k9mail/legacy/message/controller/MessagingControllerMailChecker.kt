package app.k9mail.legacy.message.controller

import app.k9mail.legacy.account.Account

interface MessagingControllerMailChecker {
    fun checkMail(
        account: Account?,
        ignoreLastCheckedTime: Boolean,
        useManualWakeLock: Boolean,
        notify: Boolean,
        listener: MessagingListener?,
    )
}
