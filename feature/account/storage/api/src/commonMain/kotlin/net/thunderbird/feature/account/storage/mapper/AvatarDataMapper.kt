package net.thunderbird.feature.account.storage.mapper

import net.thunderbird.core.architecture.data.DataMapper
import net.thunderbird.feature.account.avatar.Avatar
import net.thunderbird.feature.account.storage.profile.AvatarDto

interface AvatarDataMapper : DataMapper<Avatar, AvatarDto>
