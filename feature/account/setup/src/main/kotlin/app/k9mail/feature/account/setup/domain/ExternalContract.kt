package app.k9mail.feature.account.setup.domain

interface ExternalContract {
    fun interface AccountOwnerNameProvider {
        fun getOwnerName(): String?
    }

    fun interface AccountSetupFinishedLauncher {
        fun launch(accountUuid: String)
    }
}
