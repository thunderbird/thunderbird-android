package app.k9mail.feature.account.common.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.atom.text.TextHeadline2
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveWidthContainer
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.PreviewWithThemes
import app.k9mail.feature.account.common.R

@Composable
fun AppTitleTopHeader(
    modifier: Modifier = Modifier,
    title: String = stringResource(id = R.string.account_common_title),
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = MainTheme.spacings.half,
                    end = MainTheme.spacings.quadruple,
                )
                .then(modifier),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(id = MainTheme.images.logo),
                contentDescription = null,
            )

            TextHeadline2(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(title)
                    }
                },
            )
        }
    }
}

@Preview
@Composable
internal fun AppTitleTopHeaderPreview() {
    PreviewWithThemes {
        AppTitleTopHeader(title = "Title")
    }
}
