package app.k9mail.feature.settings.import

import net.thunderbird.feature.account.AccountId

interface SettingsImportExternalContract {
    /**
     * Activate account after server password(s) have been provided on settings import.
     */
    interface AccountActivator {
        fun enableAccount(accountId: AccountId, incomingServerPassword: String?, outgoingServerPassword: String?)

        fun enableAccount(accountId: AccountId)
    }
}
