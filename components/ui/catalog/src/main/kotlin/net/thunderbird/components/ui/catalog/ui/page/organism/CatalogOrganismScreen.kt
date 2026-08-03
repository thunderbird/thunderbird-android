package net.thunderbird.components.ui.catalog.ui.page.organism

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.thunderbird.components.ui.catalog.ui.mvi.observe
import net.thunderbird.components.ui.catalog.ui.page.CatalogPageContract.ViewModel
import net.thunderbird.components.ui.catalog.ui.page.CatalogPageViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CatalogOrganismScreen(
    modifier: Modifier = Modifier,
    viewModel: ViewModel = koinViewModel<CatalogPageViewModel>(),
) {
    val (state, dispatch) = viewModel.observe { }

    CatalogOrganismContent(
        pages = CatalogOrganismPage.all(),
        initialPage = state.value.page as? CatalogOrganismPage ?: CatalogOrganismPage.APP_BAR,
        onEvent = { dispatch(it) },
        modifier = modifier,
    )
}
