package app.k9mail.feature.navigation.drawer

import app.k9mail.feature.navigation.drawer.ui.DrawerViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

val navigationDrawerModule: Module = module {

    viewModel { DrawerViewModel() }
}
