package net.thunderbird.feature.thundermail.internal.common.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import net.thunderbird.components.ui.bolt.PreviewWithThemeLightDark

@Preview
@Composable
private fun ThundermailOAuthRedirectScreenPreview(
    @PreviewParameter(ThundermailOAuthRedirectScreenPreviewColProvider::class) param:
    Pair<String, ThundermailContract.State>,
) {
    val (_, state) = param
    PreviewWithThemeLightDark {
        ThundermailOAuthRedirectScreen(
            state = state,
            onBack = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private class ThundermailOAuthRedirectScreenPreviewColProvider :
    CollectionPreviewParameterProvider<Pair<String, ThundermailContract.State>>(
        listOf(
            "Default" to ThundermailContract.State(),
            "Browser not available error" to ThundermailContract.State(
                error = ThundermailContract.Error.BrowserNotAvailable,
            ),
            "Canceled error" to ThundermailContract.State(
                error = ThundermailContract.Error.Canceled,
            ),
            "Unknown error" to ThundermailContract.State(
                error = ThundermailContract.Error.Unknown(Exception("Something went wrong")),
            ),
        ),
    ) {
    override fun getDisplayName(index: Int): String = values.elementAt(index).first
}
