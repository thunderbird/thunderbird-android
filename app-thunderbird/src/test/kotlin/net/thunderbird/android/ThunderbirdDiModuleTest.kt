package net.thunderbird.android

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.util.DisplayMetrics
import kotlin.test.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, manifest = Config.NONE)
class ThunderbirdDiModuleTest {

    private val externalDependencies = module {
        single<Application> { mock() }
        single<Context> { get<Application>() }
        single<AssetManager> { mock() }
        single<Configuration> { mock() }
        single<DisplayMetrics> { mock() }
        single<NotificationManager> { mock() }
        single<Resources> { mock() }
    }

    @Test
    fun `should have a complete di graph`() {
        try {
            startKoin {
                modules(thunderbirdDiModule, externalDependencies)
            }
        } finally {
            stopKoin()
        }
    }
}
