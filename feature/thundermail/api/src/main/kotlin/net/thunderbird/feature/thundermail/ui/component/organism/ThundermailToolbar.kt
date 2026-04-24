package net.thunderbird.feature.thundermail.ui.component.organism

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleLarge
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.thundermail.ui.screen.ThundermailConstants

@Composable
fun ThundermailToolbar(
    header: @Composable () -> Unit,
    subHeaderText: String,
    modifier: Modifier = Modifier,
    maxWidth: Dp = ThundermailConstants.MaxContainerWidth,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = MainTheme.spacings.double,
        vertical = MainTheme.spacings.default,
    ),
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = modifier.fillMaxWidth(),
    ) {
        Spacer(modifier = Modifier.heightIn(min = MainTheme.sizes.large, max = MainTheme.sizes.huger))
        header()
        Spacer(modifier = Modifier.height(MainTheme.spacings.triple))
        TextTitleLarge(
            text = subHeaderText,
            color = MainTheme.colors.primary,
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth()
                .padding(contentPadding),
        )
    }
}
