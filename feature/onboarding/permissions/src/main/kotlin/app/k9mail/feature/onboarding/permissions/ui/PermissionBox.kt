package app.k9mail.feature.onboarding.permissions.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.common.image.ImageWithOverlayCoordinate
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonFilled
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleLarge
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.feature.onboarding.permissions.R
import app.k9mail.feature.onboarding.permissions.ui.PermissionsContract.UiPermissionState
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon

private val MAX_WIDTH = 500.dp

@Composable
internal fun PermissionBox(
    icon: ImageWithOverlayCoordinate,
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
    icon: ImageWithOverlayCoordinate,
    permissionState: UiPermissionState,
) {
    Box {
        val iconSize = MainTheme.sizes.iconLarge
        val overlayIconSize = iconSize / 2
        val overlayIconOffset = overlayIconSize / 2
        val scalingFactor = iconSize / icon.image.defaultHeight
        val overlayOffsetX = (icon.overlayOffsetX * scalingFactor) - overlayIconOffset
        val overlayOffsetY = (icon.overlayOffsetY * scalingFactor) - overlayIconOffset

        Icon(
            imageVector = icon.image,
            modifier = Modifier.size(iconSize),
        )

        when (permissionState) {
            UiPermissionState.Unknown -> Unit
            UiPermissionState.Granted -> {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    tint = MainTheme.colors.success,
                    modifier = Modifier
                        .size(overlayIconSize)
                        .offset(
                            x = overlayOffsetX,
                            y = overlayOffsetY,
                        ),
                )
            }

            UiPermissionState.Denied -> {
                Icon(
                    imageVector = Icons.Filled.Cancel,
                    tint = MainTheme.colors.warning,
                    modifier = Modifier
                        .size(overlayIconSize)
                        .offset(
                            x = overlayOffsetX,
                            y = overlayOffsetY,
                        ),
                )
            }
        }
    }
}
