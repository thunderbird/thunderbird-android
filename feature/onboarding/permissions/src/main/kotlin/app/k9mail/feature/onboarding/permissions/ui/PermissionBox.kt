package app.k9mail.feature.onboarding.permissions.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonFilled
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleLarge
import app.k9mail.feature.onboarding.permissions.R
import app.k9mail.feature.onboarding.permissions.ui.PermissionsContract.UiPermissionState
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.core.ui.compose.theme2.MainTheme

private val MAX_WIDTH = 500.dp

@Composable
internal fun PermissionBox(
    icon: ImageVector,
    permissionState: UiPermissionState,
    title: String,
    description: String,
    onAllowClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(MAX_WIDTH)
            .padding(horizontal = MainTheme.spacings.double),
    ) {
        Row {
            Box(
                modifier = Modifier.padding(
                    end = MainTheme.spacings.double,
                    top = MainTheme.spacings.default,
                    bottom = MainTheme.spacings.default,
                ),
            ) {
                IconWithPermissionStateOverlay(icon, permissionState)
            }
            Column {
                TextTitleLarge(text = title)
                TextBodyMedium(text = description)
            }
        }

        val buttonAlpha by animateFloatAsState(
            targetValue = if (permissionState == UiPermissionState.Granted) 0f else 1f,
            label = "AllowButtonAlpha",
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End,
        ) {
            Spacer(modifier = Modifier.height(MainTheme.spacings.default))

            ButtonFilled(
                text = stringResource(R.string.onboarding_permissions_allow_button),
                onClick = onAllowClick,
                modifier = Modifier.alpha(buttonAlpha),
            )
        }
    }
}

@Composable
private fun IconWithPermissionStateOverlay(
    icon: ImageVector,
    permissionState: UiPermissionState,
) {
    Box(
        contentAlignment = Alignment.BottomEnd,
    ) {
        Icon(
            imageVector = icon,
            modifier = Modifier.size(MainTheme.sizes.iconLarge),
        )

        when (permissionState) {
            UiPermissionState.Unknown -> Unit
            UiPermissionState.Granted -> {
                OverlayIcon(
                    imageVector = Icons.Filled.CheckCircle,
                    tint = MainTheme.colors.success,
                )
            }

            UiPermissionState.Denied -> {
                OverlayIcon(
                    imageVector = Icons.Filled.Cancel,
                    tint = MainTheme.colors.warning,
                )
            }
        }
    }
}

@Composable
private fun OverlayIcon(
    imageVector: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MainTheme.colors.surface,
        shape = CircleShape,
        modifier = modifier,
    ) {
        Icon(
            imageVector = imageVector,
            tint = tint,
            modifier = Modifier.size(MainTheme.sizes.iconSmall),
        )
    }
}
