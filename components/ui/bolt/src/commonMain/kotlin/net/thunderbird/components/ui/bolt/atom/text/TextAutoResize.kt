package net.thunderbird.components.ui.bolt.atom.text

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text as Material3Text

@Composable
fun TextAutoResize(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    softWrap: Boolean = false,
    maxLines: Int = 1,
    minFontSizeModifier: Float = 0.75f,
) {
    val minLineHeightSizeModifier = if (minFontSizeModifier * 1.1f < 1.0f) {
        minFontSizeModifier * 1.1f
    } else {
        1.0f
    }
    Material3Text(
        text = text,
        modifier = modifier,
        style = style.merge(
            color = color,
            textAlign = textAlign ?: TextAlign.Unspecified,
            lineHeight = if (maxLines > 0) {
                style.lineHeight * minLineHeightSizeModifier
            } else {
                style.lineHeight
            },
        ),
        softWrap = softWrap,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        autoSize = TextAutoSize.StepBased(
            minFontSize = style.fontSize * minFontSizeModifier,
            maxFontSize = style.fontSize,
            stepSize = 0.1.sp,
        ),
    )
}

@Composable
fun TextAutoResize(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    softWrap: Boolean = false,
    maxLines: Int = 1,
    minFontSizeModifier: Float = 0.75f,
) {
    val minLineHeightSizeModifier = if (minFontSizeModifier * 1.1f < 1.0f) {
        minFontSizeModifier * 1.1f
    } else {
        1.0f
    }
    Material3Text(
        text = text,
        modifier = modifier,
        style = style.merge(
            color = color,
            textAlign = textAlign ?: TextAlign.Unspecified,
            lineHeight = if (maxLines > 0) {
                style.lineHeight * minLineHeightSizeModifier
            } else {
                style.lineHeight
            },
        ),
        softWrap = softWrap,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        autoSize = TextAutoSize.StepBased(
            minFontSize = style.fontSize * minFontSizeModifier,
            maxFontSize = style.fontSize,
            stepSize = 0.1.sp,
        ),
    )
}
