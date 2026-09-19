package app.k9mail.feature.launcher

import androidx.activity.ComponentActivity

/**
 * Contract defining the external functionality of the feature launcher to be provided by the host application.
 */
interface FeatureLauncherExternalContract {
    fun interface MessageListLauncher {
        fun launch(accountUuid: String?)
    }

    fun interface AccountSetupCompositionLauncher {
        fun launch(activity: ComponentActivity, accountUuid: String?)
    }

    fun interface AccountManageIdentitiesLauncher {
        fun launch(activity: ComponentActivity, accountUuid: String?)
    }
}
