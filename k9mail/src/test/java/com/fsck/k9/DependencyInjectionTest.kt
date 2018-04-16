package com.fsck.k9

import android.arch.lifecycle.Lifecycle
import org.junit.Test
import org.koin.Koin
import org.koin.log.PrintLogger
import org.koin.test.dryRun
import org.mockito.Mockito.mock

class DependencyInjectionTest : K9RobolectricTest() {
    @Test
    fun testDependencyTree() {
        Koin.logger = PrintLogger()

        dryRun {
            mapOf(
                    "lifecycle" to mock(Lifecycle::class.java)
            )
        }
    }
}
