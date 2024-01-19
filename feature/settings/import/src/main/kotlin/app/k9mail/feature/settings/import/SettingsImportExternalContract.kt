package app.k9mail.feature.settings.import

interface SettingsImportExternalContract {
    /**
     * Activate account after server password(s) have been provided on settings import.
     */
    interface AccountActivator {
        fun enableAccount(accountUuid: String, incomingServerPassword: String?, outgoingServerPassword: String?)

        fun enableAccount(accountUuid: String)
    }
}
