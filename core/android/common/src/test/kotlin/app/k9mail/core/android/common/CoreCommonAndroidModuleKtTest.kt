package app.k9mail.core.android.common

import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.koinApplication
import org.koin.test.check.checkModules
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
internal class CoreCommonAndroidModuleKtTest {

    @Test
    fun `should have a valid di module`() {
        koinApplication {
            modules(coreCommonAndroidModule)
            androidContext(RuntimeEnvironment.getApplication())
            checkModules()
        }
    }
}
