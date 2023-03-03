package app.k9mail.core.android.common

import org.junit.Test
import org.koin.dsl.koinApplication
import org.koin.test.check.checkModules

internal class CoreCommonAndroidModuleTest {

    @Test
    fun `should have a valid di module`() {
        koinApplication {
            modules(coreCommonAndroidModule)
            checkModules()
        }
    }
}
