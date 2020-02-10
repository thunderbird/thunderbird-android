package com.fsck.k9

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.fsck.k9.ui.endtoend.AutocryptKeyTransferActivity
import com.fsck.k9.ui.endtoend.AutocryptKeyTransferPresenter
import com.fsck.k9.ui.folders.FolderNameFormatter
import com.fsck.k9.ui.helper.SizeFormatter
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.KoinApplication
import org.koin.core.logger.PrintLogger
import org.koin.core.parameter.parametersOf
import org.koin.test.AutoCloseKoinTest
import org.koin.test.check.checkModules
import org.openintents.openpgp.OpenPgpApiManager
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = App::class)
class DependencyInjectionTest : AutoCloseKoinTest() {
    val lifecycleOwner = mock<LifecycleOwner> {
        on { lifecycle } doReturn mock<Lifecycle>()
    }
    val autocryptTransferView = mock<AutocryptKeyTransferActivity>()

    @Test
    fun testDependencyTree() {
        KoinApplication.logger = PrintLogger()

        getKoin().checkModules {
            create<OpenPgpApiManager> { parametersOf(lifecycleOwner) }
            create<AutocryptKeyTransferPresenter> { parametersOf(lifecycleOwner, autocryptTransferView) }
            create<FolderNameFormatter> { parametersOf(RuntimeEnvironment.application) }
            create<SizeFormatter> { parametersOf(RuntimeEnvironment.application) }
        }
    }
}
