package net.thunderbird.components.ui.catalog

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class CatalogApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            allowOverride(false)
            androidContext(this@CatalogApplication)
            modules(componentsUiCatalogDiModule)
        }
    }
}
