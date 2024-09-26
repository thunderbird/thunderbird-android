package app.k9mail.feature.migration.qrcode.ui

import android.app.Application
import android.content.Context
import app.k9mail.feature.migration.qrcode.qrCodeModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

// TODO: This only exists for manual testing during development. Remove when integrating the feature into the app.
class QrCodeApplication : Application() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        startKoin {
            androidContext(this@QrCodeApplication)
            modules(qrCodeModule)
        }
    }
}
