package app.k9mail.core.common

import org.junit.Test
import org.koin.dsl.koinApplication
import org.koin.test.check.checkModules

internal class CoreCommonModuleKtTest {

    @Test
    fun `should have a valid di module`() {
        koinApplication {
            modules(coreCommonModule)
            checkModules()
        }
    }
}
