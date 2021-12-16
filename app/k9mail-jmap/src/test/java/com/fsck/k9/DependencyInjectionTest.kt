package com.fsck.k9

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.fsck.k9.ui.changelog.ChangeLogMode
import com.fsck.k9.ui.changelog.ChangelogViewModel
import com.fsck.k9.ui.endtoend.AutocryptKeyTransferActivity
import com.fsck.k9.ui.endtoend.AutocryptKeyTransferPresenter
import com.fsck.k9.ui.folders.FolderNameFormatter
import com.fsck.k9.ui.helper.SizeFormatter
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.logger.PrintLogger
import org.koin.core.parameter.parametersOf
import org.koin.java.KoinJavaComponent
import org.koin.test.AutoCloseKoinTest
import org.koin.test.check.checkModules
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
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

    @KoinInternalApi
    @Test
    fun testDependencyTree() {
        KoinJavaComponent.getKoin().setupLogger(PrintLogger())

        getKoin().checkModules {
            withParameter<OpenPgpApiManager> { lifecycleOwner }
            create<AutocryptKeyTransferPresenter> { parametersOf(lifecycleOwner, autocryptTransferView) }
            withParameter<FolderNameFormatter> { RuntimeEnvironment.application }
            withParameter<SizeFormatter> { RuntimeEnvironment.application }
            withParameter<ChangelogViewModel> { ChangeLogMode.CHANGE_LOG }
        }
    }
}
