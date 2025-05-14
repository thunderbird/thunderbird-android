package net.thunderbird.feature.account.storage.mapper

import net.thunderbird.core.architecture.data.DataMapper
import net.thunderbird.feature.account.profile.AccountProfile
import net.thunderbird.feature.account.storage.profile.ProfileDto

interface AccountProfileDataMapper : DataMapper<AccountProfile, ProfileDto>
