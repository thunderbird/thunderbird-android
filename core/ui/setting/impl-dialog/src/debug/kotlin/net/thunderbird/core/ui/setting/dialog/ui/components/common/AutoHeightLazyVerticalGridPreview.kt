package net.thunderbird.core.ui.setting.dialog.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import kotlinx.collections.immutable.persistentListOf

@Composable
@Preview(showBackground = true)
internal fun AutoHeightLazyVerticalWithOnePreview() {
    PreviewWithTheme {
        AutoHeightLazyVerticalGrid(
            items = persistentListOf(1),
            itemSize = 64.dp,
            itemContent = { ItemContent(it) },
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AutoHeightLazyVerticalWithOneShortPreview() {
    PreviewWithTheme {
        AutoHeightLazyVerticalGrid(
            items = persistentListOf(1, 2, 3, 4),
            itemSize = 64.dp,
            itemContent = { ItemContent(it) },
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AutoHeightLazyVerticalGridOverflowByOnePreview() {
    PreviewWithTheme {
        AutoHeightLazyVerticalGrid(
            items = persistentListOf(1, 2, 3, 4, 5, 6),
            itemSize = 64.dp,
            itemContent = { ItemContent(it) },
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AutoHeightLazyVerticalGridOverflowByTwoPreview() {
    PreviewWithTheme {
        AutoHeightLazyVerticalGrid(
            items = persistentListOf(1, 2, 3, 4, 5, 6, 7),
            itemSize = 64.dp,
            itemContent = { ItemContent(it) },
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AutoHeightLazyVerticalGridOverflowByOneShortPreview() {
    PreviewWithTheme {
        AutoHeightLazyVerticalGrid(
            items = persistentListOf(1, 2, 3, 4, 5, 6, 7, 8, 9),
            itemSize = 64.dp,
            itemContent = { ItemContent(it) },
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AutoHeightLazyVerticalGridOverflowPreview() {
    PreviewWithTheme {
        AutoHeightLazyVerticalGrid(
            items = persistentListOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
            itemSize = 64.dp,
            itemContent = { ItemContent(it) },
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AutoHeightLazyVerticalGridOverflowLargeSpacingPreview() {
    PreviewWithTheme {
        AutoHeightLazyVerticalGrid(
            items = persistentListOf(1, 2, 3, 4, 5, 6, 7, 8, 9),
            itemSize = 64.dp,
            horizontalSpacing = 64.dp,
            verticalSpacing = 64.dp,
            itemContent = { ItemContent(it) },
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AutoHeightLazyVerticalGridOverflowByLargeSpacingAndOneShortPreview() {
    PreviewWithTheme {
        AutoHeightLazyVerticalGrid(
            items = persistentListOf(1, 2, 3, 4, 5, 6, 7, 8),
            itemSize = 64.dp,
            horizontalSpacing = 64.dp,
            verticalSpacing = 64.dp,
            itemContent = { ItemContent(it) },
        )
    }
}

@Composable
private fun ItemContent(it: Int) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .background(color = Color.Red, shape = CircleShape),

        contentAlignment = Alignment.Center,
    ) {
        TextBodyLarge(
            text = it.toString(),
            color = Color.White,
        )
    }
}
