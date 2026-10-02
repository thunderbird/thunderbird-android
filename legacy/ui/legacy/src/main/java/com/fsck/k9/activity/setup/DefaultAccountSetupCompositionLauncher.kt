package com.fsck.k9.activity.setup

import androidx.activity.ComponentActivity
import app.k9mail.feature.launcher.FeatureLauncherExternalContract

internal class DefaultAccountSetupCompositionLauncher :
    FeatureLauncherExternalContract.AccountSetupCompositionLauncher {
    override fun launch(activity: ComponentActivity, accountUuid: String?) {
        AccountSetupComposition.actionEditCompositionSettings(
            context = activity,
            accountUuid = accountUuid,
        )
    }
}
