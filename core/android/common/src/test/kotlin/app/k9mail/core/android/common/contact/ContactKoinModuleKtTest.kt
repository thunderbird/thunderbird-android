package app.k9mail.core.android.common.contact

import app.k9mail.core.android.common.coreCommonAndroidModule
import app.k9mail.core.android.common.test.externalModule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.koinApplication
import org.koin.test.check.checkModules
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
internal class ContactKoinModuleKtTest {

    @Test
    fun `should have a valid di module`() {
        koinApplication {
            modules(
                externalModule,
                coreCommonAndroidModule,
                contactModule,
            )
            androidContext(RuntimeEnvironment.getApplication())
            checkModules()
        }
    }
}
