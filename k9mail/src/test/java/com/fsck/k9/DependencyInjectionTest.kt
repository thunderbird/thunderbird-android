package com.fsck.k9

import org.junit.Test
import org.koin.Koin
import org.koin.log.PrintLogger
import org.koin.test.dryRun

class DependencyInjectionTest : K9RobolectricTest() {
    @Test
    fun testDependencyTree() {
        Koin.logger = PrintLogger()

        dryRun()
    }
}
