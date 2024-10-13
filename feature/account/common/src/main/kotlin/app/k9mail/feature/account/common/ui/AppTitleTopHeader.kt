package app.k9mail.feature.account.common.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.text.TextDisplayMedium
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveWidthContainer
import app.k9mail.core.ui.compose.theme2.MainTheme

private const val TITLE_ICON_SIZE_DP = 56

@Composable
fun AppTitleTopHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    ResponsiveWidthContainer(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = MainTheme.spacings.quadruple,
                bottom = MainTheme.spacings.default,
            )
            .then(modifier),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = MainTheme.spacings.half,
                    end = MainTheme.spacings.quadruple,
                )
                .then(modifier),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(id = MainTheme.images.logo),
                modifier = Modifier
                    .padding(all = MainTheme.spacings.default)
                    .padding(end = MainTheme.spacings.default)
                    .size(TITLE_ICON_SIZE_DP.dp),
                contentDescription = null,
            )

            TextDisplayMedium(text = title)
        }
    }
}
