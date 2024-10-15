package app.k9mail.feature.account.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.common.provider.BrandNameProvider
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes

@Composable
@Preview(showBackground = true)
internal fun AppTitleTopHeaderPreview() {
    PreviewWithThemes {
        AppTitleTopHeader(
            brandNameProvider = object : BrandNameProvider {
                override val brandName = "BrandName"
            },
        )
    }
}
