package net.thunderbird.components.ui.catalog

import net.thunderbird.components.ui.catalog.ui.CatalogViewModel
import net.thunderbird.components.ui.catalog.ui.page.CatalogPageViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val componentsUiCatalogDiModule: Module = module {
    viewModel { CatalogViewModel() }
    viewModel { CatalogPageViewModel() }
}
