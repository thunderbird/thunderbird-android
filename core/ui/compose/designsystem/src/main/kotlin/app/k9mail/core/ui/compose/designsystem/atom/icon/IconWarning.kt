package app.k9mail.core.ui.compose.designsystem.atom.icon

import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.R
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.PreviewWithThemes

@Composable
fun IconWarning(
    modifier: Modifier = Modifier,
    tint: Color = MainTheme.colors.warning,
) {
    Icon(
        modifier = modifier,
        imageVector = Icons.Filled.Warning,
        contentDescription = stringResource(id = R.string.designsystem_atom_icon_warning),
        tint = tint,
    )
}

@Preview
@Composable
internal fun IconWarningPreview() {
    PreviewWithThemes {
        IconWarning()
    }
}
