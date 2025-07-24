package app.k9mail.core.ui.compose.designsystem.organism

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.PreviewLightDarkLandscape
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemesLightDark
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextHeadlineSmall
import app.k9mail.core.ui.compose.theme2.MainTheme

@PreviewLightDarkLandscape
@Composable
private fun BasicDialogPreview() {
    PreviewWithThemesLightDark(
        useRow = true,
        useScrim = true,
        scrimPadding = PaddingValues(32.dp),
        arrangement = Arrangement.spacedBy(24.dp),
    ) {
        BasicDialogContent(
            headline = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    TextHeadlineSmall(text = "Reset settings?")
                }
            },
            supportingText = {
                TextBodyMedium(
                    text = "This will reset your app preferences back to their default settings. " +
                        "The following accounts will also be signed out:",
                    color = MainTheme.colors.onSurfaceVariant,
                )
            },
            content = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = MainTheme.spacings.triple,
                            end = MainTheme.spacings.triple,
                        ),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(MainTheme.sizes.iconAvatar)
                                .background(color = MainTheme.colors.primary, shape = CircleShape),
                        )
                        Text(text = "Account 1")
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(MainTheme.sizes.iconAvatar)
                                .background(color = MainTheme.colors.primary, shape = CircleShape),
                        )
                        Text(text = "Account 2")
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(MainTheme.sizes.iconAvatar)
                                .background(color = MainTheme.colors.primary, shape = CircleShape),
                        )
                        Text(text = "Account 3")
                    }
                }
            },
            buttons = {
                TextButton(onClick = {}) {
                    Text(text = "Cancel")
                }
                TextButton(onClick = {}) {
                    Text(text = "Accept")
                }
            },
            showDividers = true,
            modifier = Modifier.width(300.dp),
        )
    }
}

@PreviewLightDarkLandscape
@Composable
private fun PreviewOnlySupportingText() {
    PreviewWithThemesLightDark(
        useRow = true,
        useScrim = true,
        scrimPadding = PaddingValues(32.dp),
        arrangement = Arrangement.spacedBy(24.dp),
    ) {
        BasicDialogContent(
            headline = {
                TextHeadlineSmall(text = "Email can not be archived")
            },
            supportingText = {
                TextBodyMedium(
                    text = "Configure archive folder now",
                    color = MainTheme.colors.onSurfaceVariant,
                )
            },
            content = null,
            buttons = {
                TextButton(onClick = {}) {
                    Text(text = "Skip for now")
                }
                TextButton(onClick = {}) {
                    Text(text = "Set archive folder")
                }
            },
            showDividers = false,
            modifier = Modifier.width(300.dp),
        )
    }
}
