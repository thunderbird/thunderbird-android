package com.fsck.k9.mailstore

import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.feature.mail.folder.api.SpecialFolderUpdater

interface LegacyAccountDtoSpecialFolderUpdaterFactory : SpecialFolderUpdater.Factory<LegacyAccountDto>
