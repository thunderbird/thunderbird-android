package app.k9mail.core.ui.compose.designsystem.atom.image

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil3.CoilImage

@Composable
fun RemoteImage(
    url: String,
    modifier: Modifier = Modifier,
    placeholder: @Composable (() -> Unit)? = null,
    alignment: Alignment = Alignment.Center,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    previewPlaceholder: Painter? = null,
) {
    CoilImage(
        imageModel = { url },
        imageOptions = ImageOptions(
            alignment = alignment,
            contentDescription = contentDescription,
            contentScale = contentScale,
        ),
        failure = {
            placeholder?.invoke()
        },
        loading = {
            placeholder?.invoke()
        },
        modifier = modifier,
        previewPlaceholder = previewPlaceholder,
    )
}
