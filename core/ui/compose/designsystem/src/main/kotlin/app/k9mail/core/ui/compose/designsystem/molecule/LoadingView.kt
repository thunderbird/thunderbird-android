package app.k9mail.core.ui.compose.designsystem.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.atom.CircularProgressIndicator
import app.k9mail.core.ui.compose.designsystem.atom.text.TextSubtitle1
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.PreviewWithThemes

@Composable
fun LoadingView(
    modifier: Modifier = Modifier,
    message: String? = null,
    contentAlignment: Alignment = Alignment.Center,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier),
        contentAlignment = contentAlignment,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MainTheme.spacings.default)
                .then(modifier),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (message != null) {
                TextSubtitle1(text = message)
            }
            Row(
                modifier = Modifier.height(MainTheme.sizes.larger),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
internal fun LoadingViewPreview() {
    PreviewWithThemes {
        LoadingView()
    }
}

@Preview(showBackground = true)
@Composable
internal fun LoadingViewWithMessagePreview() {
    PreviewWithThemes {
        LoadingView(
            message = "Loading ...",
        )
    }
}
