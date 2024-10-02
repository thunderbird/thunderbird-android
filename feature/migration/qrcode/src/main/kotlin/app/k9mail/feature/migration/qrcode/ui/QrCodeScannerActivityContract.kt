package app.k9mail.feature.migration.qrcode.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract

class QrCodeScannerActivityContract : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: Context, input: Unit): Intent {
        return Intent(context, QrCodeScannerActivity::class.java)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return intent?.data.takeIf { resultCode == Activity.RESULT_OK }
    }
}
