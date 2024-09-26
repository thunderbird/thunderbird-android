package app.k9mail.feature.migration.qrcode.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import app.k9mail.core.ui.compose.common.activity.setActivityContent
import app.k9mail.core.ui.compose.theme2.k9mail.K9MailTheme2

// TODO: This only exists for manual testing during development. Remove when integrating the feature into the app.
class QrCodeScannerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setActivityContent {
            K9MailTheme2(darkTheme = true) {
                QrCodeScannerScreen()
            }
        }
    }
}
