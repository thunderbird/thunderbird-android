package net.thunderbird.feature.account.avatar.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import net.thunderbird.feature.account.avatar.Avatar

/**
 * Returns a compatible [Avatar]. If the provided [avatar] is not a [Avatar.Monogram],
 * a new [Avatar.Monogram] is created using the provided [name].
 *
 * @param avatar The original account avatar.
 * @param name The name to use for generating a monogram if needed.
 */
@Composable
fun rememberCompatAvatar(
    avatar: Avatar?,
    name: String,
): Avatar = remember(avatar, name) {
    when (avatar) {
        is Avatar.Monogram -> avatar
        is Avatar.Icon -> avatar
        is Avatar.Image -> avatar
        else -> Avatar.Monogram(
            value = extractNameInitials(name),
        )
    }
}

private fun extractNameInitials(displayName: String): String {
    return displayName.take(2)
}
