package app.k9mail.feature.settings.import.ui

import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import app.k9mail.feature.migration.launcher.api.MigrationManager

class FakeMigrationManager(var featureIncluded: Boolean) : MigrationManager {
    override fun isFeatureIncluded(): Boolean {
        return featureIncluded
    }

    override fun getQrCodeActivityResultContract(): ActivityResultContract<Unit, Uri?> {
        error("Not implemented")
    }
}
