package com.fsck.k9

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import com.fsck.k9.notification.NotificationActionCreator
import com.fsck.k9.ui.endtoend.AutocryptKeyTransferActivity
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import org.junit.Test
import org.koin.Koin
import org.koin.dsl.module.applicationContext
import org.koin.log.PrintLogger
import org.koin.standalone.StandAloneContext
import org.koin.test.dryRun

class DependencyInjectionTest : K9RobolectricTest() {
    val lifecycleOwner = mock<LifecycleOwner> {
        on { lifecycle } doReturn mock<Lifecycle>()
    }

    @Test
    fun testDependencyTree() {
        Koin.logger = PrintLogger()

        // NOTE: Users of the core module will have to provide these dependencies.
        StandAloneContext.loadKoinModules(applicationContext {
            bean { mock<NotificationActionCreator>() }
        })

        dryRun {
            mapOf(
                    "lifecycleOwner" to lifecycleOwner,
                    "autocryptTransferView" to mock<AutocryptKeyTransferActivity>()
            )
        }
    }
}
