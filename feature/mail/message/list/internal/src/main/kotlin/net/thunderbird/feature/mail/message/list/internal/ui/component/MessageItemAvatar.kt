package net.thunderbird.feature.mail.message.list.internal.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.CircularProgressIndicator
import app.k9mail.core.ui.compose.designsystem.atom.image.RemoteImage
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleSmall
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.mail.message.list.ui.state.Avatar

@Composable
fun MessageItemAvatar(
    avatar: Avatar?,
    showMessageAvatar: Boolean,
    modifier: Modifier = Modifier,
    onAvatarClick: () -> Unit,
) {
    if (showMessageAvatar) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(MainTheme.sizes.iconAvatar)
                .padding(MainTheme.spacings.half)
                .background(color = MainTheme.colors.primaryContainer.copy(alpha = 0.15f), shape = CircleShape)
                .border(width = 1.dp, color = MainTheme.colors.primary, shape = CircleShape)
                .clickable(onClick = onAvatarClick),
        ) {
            when (avatar) {
                is Avatar.Icon -> Icon(
                    avatar.imageVector,
                    contentDescription = null,
                    modifier = Modifier.size(MainTheme.sizes.iconAvatar),
                )

                is Avatar.Image -> RemoteImage(
                    url = avatar.url,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    placeholder = {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(MainTheme.sizes.iconAvatar)) {
                            CircularProgressIndicator(modifier = Modifier.size(MainTheme.sizes.icon))
                        }
                    },
                )

                is Avatar.Monogram -> TextTitleSmall(text = avatar.value)
                null -> Unit
            }
        }
    }
}
