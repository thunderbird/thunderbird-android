package app.k9mail.feature.preview

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class FeatureApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@FeatureApplication)
            modules(featureModule)
        }
    }
}
