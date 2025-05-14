package net.thunderbird.feature.account.storage.mapper

import net.thunderbird.core.architecture.data.DataMapper
import net.thunderbird.feature.account.profile.AccountAvatar
import net.thunderbird.feature.account.storage.profile.AvatarDto

interface AccountAvatarDataMapper : DataMapper<AccountAvatar, AvatarDto>
