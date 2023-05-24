package app.k9mail.ui.catalog.ui

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

val catalogUiModule: Module = module {
    viewModel { CatalogViewModel() }
}
