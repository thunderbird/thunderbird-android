package com.fsck.k9

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import com.fsck.k9.ui.endtoend.AutocryptKeyTransferActivity
import com.fsck.k9.ui.endtoend.AutocryptKeyTransferViewModel
import com.fsck.k9.ui.endtoend.AutocryptSetupMessageLiveEvent
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import org.junit.Test
import org.koin.Koin
import org.koin.log.PrintLogger
import org.koin.test.dryRun

class DependencyInjectionTest : K9RobolectricTest() {
    val lifecycleOwner = mock<LifecycleOwner> {
        on { lifecycle } doReturn mock<Lifecycle>()
    }

    @Test
    fun testDependencyTree() {
        Koin.logger = PrintLogger()

        dryRun {
            mapOf(
                    "lifecycleOwner" to lifecycleOwner,
                    "autocryptTransferView" to mock<AutocryptKeyTransferActivity>()
            )
        }
    }
}
