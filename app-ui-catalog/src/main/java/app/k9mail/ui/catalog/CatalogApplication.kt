package app.k9mail.ui.catalog

import android.app.Application
import app.k9mail.ui.catalog.ui.catalogUiModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class CatalogApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@CatalogApplication)
            modules(catalogUiModule)
        }
    }
}
