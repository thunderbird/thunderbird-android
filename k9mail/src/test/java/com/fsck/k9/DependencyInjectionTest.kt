package com.fsck.k9

import android.arch.lifecycle.Lifecycle
import com.nhaarman.mockito_kotlin.mock
import org.junit.Test
import org.koin.Koin
import org.koin.log.PrintLogger
import org.koin.test.dryRun

class DependencyInjectionTest : K9RobolectricTest() {
    @Test
    fun testDependencyTree() {
        Koin.logger = PrintLogger()

        dryRun {
            mapOf(
                    "lifecycle" to mock<Lifecycle>()
            )
        }
    }
}
