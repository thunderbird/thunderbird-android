package net.thunderbird.account.fake

import net.thunderbird.feature.account.profile.AccountAvatar

object FakeAccountAvatarData {

    const val AVATAR_IMAGE_URI = "https://example.com/avatar.png"

    val ACCOUNT_AVATAR = AccountAvatar.Image(
        uri = AVATAR_IMAGE_URI,
    )
}
