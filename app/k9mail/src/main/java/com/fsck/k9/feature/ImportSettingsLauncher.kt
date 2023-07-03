package com.fsck.k9.feature

import android.content.Context
import android.content.Intent
import app.k9mail.feature.launcher.FeatureLauncherExternalContract
import com.fsck.k9.activity.FragmentLauncherActivity

class ImportSettingsLauncher(
    private val context: Context,
) : FeatureLauncherExternalContract.ImportSettingsLauncher {
    override fun launch() {
        val intent = Intent(context, FragmentLauncherActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(FragmentLauncherActivity.EXTRA_FRAGMENT, FragmentLauncherActivity.FRAGMENT_IMPORT_SETTINGS)
        }
        context.startActivity(intent)
    }
}
