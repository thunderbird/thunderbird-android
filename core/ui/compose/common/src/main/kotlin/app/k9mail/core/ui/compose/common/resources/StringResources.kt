package app.k9mail.core.ui.compose.common.resources

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString

private const val PLACE_HOLDER = "{placeHolder}"

/**
 * Loads a string resource with a single string parameter that will be replaced with the given [AnnotatedString].
 */
@Composable
@ReadOnlyComposable
fun annotatedStringResource(@StringRes id: Int, argument: AnnotatedString): AnnotatedString {
    val stringWithPlaceHolder = stringResource(id, PLACE_HOLDER)
    return buildAnnotatedString {
        // In Android Studio previews loading string resources with formatting is not working
        if (LocalInspectionMode.current) {
            append(stringWithPlaceHolder)
            return@buildAnnotatedString
        }

        val placeHolderStartIndex = stringWithPlaceHolder.indexOf(PLACE_HOLDER)
        require(placeHolderStartIndex != -1)

        append(text = stringWithPlaceHolder, start = 0, end = placeHolderStartIndex)
        append(argument)
        append(
            text = stringWithPlaceHolder,
            start = placeHolderStartIndex + PLACE_HOLDER.length,
            end = stringWithPlaceHolder.length,
        )
    }
}
