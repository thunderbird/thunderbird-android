package app.k9mail.feature.account.common.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.thunderbird.components.ui.bolt.atom.text.TextDisplayMediumAutoResize
import net.thunderbird.components.ui.bolt.template.ResponsiveWidthContainer
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import org.jetbrains.compose.resources.painterResource

private const val TITLE_ICON_SIZE_DP = 56

/**
 * Onboarding header containing the application logo image and title text arranged horizontally.
 *
 * @param title The title text to display next to the logo
 * @param modifier Optional modifier to apply to the container
 * @param content Optional composable content to display below the title row. The component automatically
 *  adjusts its width based on screen size and centers the content horizontally.
 */
@Composable
fun AppTitleTopHeader(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    ResponsiveWidthContainer(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = BoltTheme.spacings.quadruple,
                bottom = BoltTheme.spacings.default,
            )
            .then(modifier),
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding)
                .padding(horizontal = BoltTheme.spacings.double),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(modifier = Modifier.wrapContentWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(BoltTheme.images.logo),
                        modifier = Modifier
                            .padding(all = BoltTheme.spacings.default)
                            .padding(end = BoltTheme.spacings.default)
                            .size(TITLE_ICON_SIZE_DP.dp),
                        contentDescription = null,
                    )

                    TextDisplayMediumAutoResize(text = title)
                }
                content()
            }
        }
    }
}
