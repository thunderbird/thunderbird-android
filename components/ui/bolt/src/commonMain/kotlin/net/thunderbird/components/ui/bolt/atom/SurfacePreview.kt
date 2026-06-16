package net.thunderbird.components.ui.bolt.atom

import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.theme.MainTheme

@Composable
@Preview(showBackground = true)
internal fun SurfacePreview() {
    PreviewWithThemes {
        Surface(
            modifier = Modifier
                .requiredHeight(MainTheme.sizes.larger)
                .requiredWidth(MainTheme.sizes.larger),
            content = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun SurfaceWithShapePreview() {
    PreviewWithThemes {
        Surface(
            modifier = Modifier
                .requiredHeight(MainTheme.sizes.larger)
                .requiredWidth(MainTheme.sizes.larger),
            shape = MainTheme.shapes.small,
            color = MainTheme.colors.primary,
            content = {},
        )
    }
}
