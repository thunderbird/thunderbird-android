package app.k9mail.core.ui.compose.designsystem.atom.button

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.theme.MainTheme

@Composable
fun buttonContentPadding(): PaddingValues = PaddingValues(
    start = MainTheme.spacings.quadruple,
    top = MainTheme.spacings.default,
    end = MainTheme.spacings.quadruple,
    bottom = MainTheme.spacings.default,
)
