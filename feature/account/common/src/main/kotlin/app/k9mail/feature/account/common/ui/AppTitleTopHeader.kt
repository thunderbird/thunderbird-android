package app.k9mail.feature.account.common.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.text.TextDisplayMediumAutoResize
import net.thunderbird.core.common.provider.UsingBrandTypography
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.thundermail.ui.RegisteredTrademarkInjector
import org.jetbrains.compose.resources.painterResource

@Composable
fun AppTitleTopHeader(
    title: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(
        start = MainTheme.spacings.half,
        end = MainTheme.spacings.quadruple,
    ),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.default, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(MainTheme.images.logo),
            modifier = Modifier.size(width = 52.dp, height = 50.dp),
            contentDescription = null,
        )

        UsingBrandTypography {
            TextDisplayMediumAutoResize(text = RegisteredTrademarkInjector.inject(title))
        }
    }
}
