package net.thunderbird.account.fake

import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.avatar.Avatar

object FakeAccountAvatarData {

    const val AVATAR_IMAGE_URI = "https://example.com/avatar.png"

    val ACCOUNT_AVATAR = Avatar.Image(
        id = AccountIdFactory.create(),
        uri = AVATAR_IMAGE_URI,
    )
}
