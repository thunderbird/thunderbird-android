package app.k9mail.feature.launcher

interface FeatureLauncherExternalContract {

    fun interface AccountSetupFinishedLauncher {
        fun launch(accountUuid: String)
    }

    fun interface ImportSettingsLauncher {
        fun launch()
    }
}
