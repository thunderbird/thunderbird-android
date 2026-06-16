package net.thunderbird.ui.catalog.ui.page.atom

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.thunderbird.ui.catalog.ui.mvi.observe
import net.thunderbird.ui.catalog.ui.page.CatalogPageContract.ViewModel
import net.thunderbird.ui.catalog.ui.page.CatalogPageViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CatalogAtomScreen(
    modifier: Modifier = Modifier,
    viewModel: ViewModel = koinViewModel<CatalogPageViewModel>(),
) {
    val (state, dispatch) = viewModel.observe { }

    CatalogAtomContent(
        pages = CatalogAtomPage.all(),
        initialPage = state.value.page as? CatalogAtomPage ?: CatalogAtomPage.TYPOGRAPHY,
        onEvent = { dispatch(it) },
        modifier = modifier,
    )
}
