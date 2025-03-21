package net.thunderbird.ui.catalog.di

import net.thunderbird.ui.catalog.ui.CatalogViewModel
import net.thunderbird.ui.catalog.ui.page.CatalogPageViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

val catalogUiModule: Module = module {
    viewModel { CatalogViewModel() }
    viewModel { CatalogPageViewModel() }
}
