package app.k9mail.feature.migration.qrcode.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import app.k9mail.core.ui.compose.common.activity.setActivityContent
import app.k9mail.core.ui.theme.api.FeatureThemeProvider
import com.fsck.k9.ui.base.K9Activity
import org.koin.android.ext.android.inject

class QrCodeScannerActivity : K9Activity() {
    private val themeProvider: FeatureThemeProvider by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setActivityContent {
            themeProvider.WithTheme(darkTheme = true) {
                QrCodeScannerScreen(
                    finishWithResult = ::finishWithResult,
                    finish = ::finish,
                )
            }
        }
    }

    private fun finishWithResult(result: Uri) {
        val resultIntent = Intent().apply {
            data = result
        }
        setResult(RESULT_OK, resultIntent)

        finish()
    }
}
