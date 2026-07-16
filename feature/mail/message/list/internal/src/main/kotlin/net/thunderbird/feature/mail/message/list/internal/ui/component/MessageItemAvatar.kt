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
import net.thunderbird.components.ui.bolt.atom.CircularProgressIndicator
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.image.RemoteImage
import net.thunderbird.components.ui.bolt.atom.text.TextTitleSmall
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.feature.mail.message.list.ui.state.Avatar

@Composable
fun MessageItemAvatar(
    avatar: Avatar?,
    showMessageAvatar: Boolean,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (showMessageAvatar) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(BoltTheme.sizes.iconAvatar)
                .padding(BoltTheme.spacings.half)
                .background(color = BoltTheme.colors.primaryContainer.copy(alpha = 0.15f), shape = CircleShape)
                .border(width = 1.dp, color = BoltTheme.colors.primary, shape = CircleShape)
                .clickable(onClick = onAvatarClick),
        ) {
            when (avatar) {
                is Avatar.Icon -> Icon(
                    avatar.imageVector,
                    contentDescription = null,
                    modifier = Modifier.size(BoltTheme.sizes.iconAvatar),
                )

                is Avatar.Image -> RemoteImage(
                    url = avatar.url,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    placeholder = {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(BoltTheme.sizes.iconAvatar)) {
                            CircularProgressIndicator(modifier = Modifier.size(BoltTheme.sizes.icon))
                        }
                    },
                )

                is Avatar.Monogram -> TextTitleSmall(text = avatar.value)

                null -> Unit
            }
        }
    }
}
