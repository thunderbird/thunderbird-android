package net.thunderbird.components.ui.bolt.atom.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import net.thunderbird.components.ui.bolt.theme.BoltTheme

@Composable
fun TextDisplayMediumAutoResize(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    softWrap: Boolean = false,
    maxLines: Int = 1,
    minFontSizeModifier: Float = 0.35f,
) {
    TextAutoResize(
        text = text,
        modifier = modifier,
        style = BoltTheme.typography.displayMedium,
        color = color,
        textAlign = textAlign,
        softWrap = softWrap,
        maxLines = maxLines,
        minFontSizeModifier = minFontSizeModifier,
    )
}

@Composable
fun TextDisplayMediumAutoResize(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    softWrap: Boolean = false,
    maxLines: Int = 1,
    minFontSizeModifier: Float = 0.35f,
) {
    TextAutoResize(
        text = text,
        modifier = modifier,
        style = BoltTheme.typography.displayMedium,
        color = color,
        textAlign = textAlign,
        softWrap = softWrap,
        maxLines = maxLines,
        minFontSizeModifier = minFontSizeModifier,
    )
}
