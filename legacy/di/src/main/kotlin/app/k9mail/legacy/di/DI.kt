package app.k9mail.legacy.di

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.java.KoinJavaComponent.get as koinGet

object DI {
    private const val DEBUG = false

    @JvmStatic
    fun start(application: Application, modules: List<Module>, allowOverride: Boolean = false) {
        startKoin {
            allowOverride(allowOverride)

            if (BuildConfig.DEBUG && DEBUG) {
                androidLogger()
            }

            androidContext(application)
            modules(modules)
        }
    }

    @JvmStatic
    fun <T : Any> get(clazz: Class<T>): T {
        return koinGet(clazz)
    }

    inline fun <reified T : Any> get(): T {
        return koinGet(T::class.java)
    }
}
