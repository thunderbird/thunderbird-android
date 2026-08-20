package net.thunderbird.components.ui.bolt.atom.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import net.thunderbird.components.ui.bolt.theme.BoltTheme

@Composable
fun TextBodyLargeAutoResize(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    softWrap: Boolean = false,
    maxLines: Int = 1,
    minFontSizeModifier: Float = 0.75f,
) {
    TextAutoResize(
        text = text,
        modifier = modifier,
        style = BoltTheme.typography.bodyLarge,
        color = color,
        textAlign = textAlign,
        softWrap = softWrap,
        maxLines = maxLines,
        minFontSizeModifier = minFontSizeModifier,
    )
}

@Composable
fun TextBodyLargeAutoResize(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    softWrap: Boolean = false,
    maxLines: Int = 1,
    minFontSizeModifier: Float = 0.75f,
) {
    TextAutoResize(
        text = text,
        modifier = modifier,
        style = BoltTheme.typography.bodyLarge,
        color = color,
        textAlign = textAlign,
        softWrap = softWrap,
        maxLines = maxLines,
        minFontSizeModifier = minFontSizeModifier,
    )
}
