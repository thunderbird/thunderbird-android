package net.thunderbird.account.fake

import net.thunderbird.feature.account.avatar.Avatar

object FakeAccountAvatarData {

    const val AVATAR_IMAGE_URI = "https://example.com/avatar.png"

    val ACCOUNT_AVATAR = Avatar.Image(
        uri = AVATAR_IMAGE_URI,
    )
}
