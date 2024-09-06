package app.k9mail.feature.launcher

/**
 * Contract defining the external functionality of the feature launcher to be provided by the host application.
 */
interface FeatureLauncherExternalContract {

    fun interface AccountSetupFinishedLauncher {
        fun launch(accountUuid: String?)
    }
}
