package app.k9mail.core.ui.compose.designsystem.atom.text

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import net.thunderbird.core.ui.compose.theme2.MainTheme

@Composable
fun TextDisplayMediumAutoResize(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
) {
    val style: TextStyle = MainTheme.typography.displayMedium
    BasicText(
        text = text,
        modifier = modifier,
        style = style.merge(
            color = color,
            textAlign = textAlign ?: TextAlign.Unspecified,
        ),
        softWrap = false,
        autoSize = TextAutoSize.StepBased(
            minFontSize = style.fontSize * 0.95f,
            maxFontSize = MainTheme.typography.displayMedium.fontSize,
            stepSize = 0.5.sp,
        ),
    )
}
