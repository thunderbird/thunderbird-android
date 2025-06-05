package net.thunderbird.account.fake

import net.thunderbird.account.fake.FakeAccountAvatarData.ACCOUNT_AVATAR
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.profile.AccountAvatar
import net.thunderbird.feature.account.profile.AccountProfile

object FakeAccountProfileData {

    const val PROFILE_NAME = "AccountProfileName"
    const val PROFILE_COLOR = 0xFF0000

    fun createAccountProfile(
        id: AccountId = FakeAccountData.ACCOUNT_ID,
        name: String = PROFILE_NAME,
        color: Int = PROFILE_COLOR,
        avatar: AccountAvatar = ACCOUNT_AVATAR,
    ): AccountProfile {
        return AccountProfile(
            id = id,
            name = name,
            color = color,
            avatar = avatar,
        )
    }
}
