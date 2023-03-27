package app.k9mail.core.ui.compose.designsystem.atom.icon

import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.R
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.PreviewWithThemes

@Composable
fun IconSuccess(
    modifier: Modifier = Modifier,
    tint: Color = MainTheme.colors.success,
) {
    Icon(
        modifier = modifier,
        imageVector = Icons.Filled.CheckCircle,
        contentDescription = stringResource(id = R.string.designsystem_atom_icon_success),
        tint = tint,
    )
}

@Preview
@Composable
internal fun IconSuccessPreview() {
    PreviewWithThemes {
        IconSuccess()
    }
}
