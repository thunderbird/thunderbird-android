package com.fsck.k9.activity

import androidx.activity.ComponentActivity
import app.k9mail.feature.launcher.FeatureLauncherExternalContract

internal class DefaultAccountManageIdentitiesLauncher :
    FeatureLauncherExternalContract.AccountManageIdentitiesLauncher {
    override fun launch(activity: ComponentActivity, accountUuid: String?) {
        ManageIdentities.start(activity, accountUuid)
    }
}
