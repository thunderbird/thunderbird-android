package app.k9mail.core.ui.compose.designsystem.atom

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import app.k9mail.core.ui.compose.theme2.MainTheme
import kotlinx.collections.immutable.persistentListOf

val choice = persistentListOf(
    Pair("1", "Native Android"),
    Pair("2", "Native iOS"),
    Pair("3", "KMM"),
    Pair("4", "Flutter"),
)

@Composable
@Preview(showBackground = true)
internal fun RadioGroupSelectedPreview() {
    PreviewWithThemes {
        RadioGroup(
            onClick = {},
            options = choice,
            optionTitle = { it.second },
            selectedOption = choice[0],
            modifier = Modifier.padding(MainTheme.spacings.default),
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun RadioGroupUnSelectedPreview() {
    PreviewWithThemes {
        RadioGroup(
            onClick = {},
            options = choice,
            optionTitle = { it.second },
            modifier = Modifier.padding(MainTheme.spacings.default),
        )
    }
}
