package net.thunderbird.feature.account.settings.impl.ui.general.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import app.k9mail.core.ui.compose.designsystem.atom.card.CardElevated
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextHeadlineSmall
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.account.avatar.Avatar
import net.thunderbird.feature.account.avatar.ui.Avatar
import net.thunderbird.feature.account.avatar.ui.AvatarSize

@Composable
internal fun GeneralSettingsProfileView(
    name: String,
    email: String?,
    color: Color,
    avatar: Avatar,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(MainTheme.spacings.double),
        contentAlignment = Alignment.TopCenter,
    ) {
        ProfileCard(
            name = name,
            email = email,
            modifier = Modifier
                .padding(top = MainTheme.spacings.quadruple)
                .fillMaxWidth(),
        )
        Avatar(
            avatar = avatar,
            color = color,
            size = AvatarSize.LARGE,
        )
    }
}

@Composable
private fun ProfileCard(
    name: String,
    email: String?,
    modifier: Modifier = Modifier,
) {
    CardElevated(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MainTheme.spacings.oneHalf,
                    vertical = MainTheme.spacings.triple,
                ),
        ) {
            Spacer(modifier = Modifier.height(MainTheme.spacings.triple))
            TextHeadlineSmall(
                text = name,
                color = MainTheme.colors.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
            email?.let {
                Spacer(modifier = Modifier.height(MainTheme.spacings.default))
                TextBodyLarge(
                    text = it,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            }
        }
    }
}
