package app.k9mail.feature.migration.launcher.k9

import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import app.k9mail.feature.migration.launcher.api.MigrationManager
import app.k9mail.feature.migration.qrcode.ui.QrCodeScannerActivityContract

class K9MigrationManager : MigrationManager {
    override fun isFeatureIncluded(): Boolean = false

    override fun getQrCodeActivityResultContract(): ActivityResultContract<Unit, Uri?> =
        QrCodeScannerActivityContract()
}
