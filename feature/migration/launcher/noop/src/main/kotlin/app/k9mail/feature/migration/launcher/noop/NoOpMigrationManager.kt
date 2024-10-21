package app.k9mail.feature.migration.launcher.noop

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import app.k9mail.feature.migration.launcher.api.MigrationManager

internal class NoOpMigrationManager : MigrationManager {
    override fun isFeatureIncluded() = false

    override fun getQrCodeActivityResultContract(): ActivityResultContract<Unit, Uri?> {
        return ThrowingActivityResultContract()
    }
}

private class ThrowingActivityResultContract : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: Context, input: Unit): Intent {
        error("Feature not enabled")
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        error("Feature not enabled")
    }
}
