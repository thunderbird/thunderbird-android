package com.fsck.k9.notification

import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.preference.GeneralSettingsManager

internal fun shouldHideNotificationContent(
    account: LegacyAccountDto,
    generalSettingsManager: GeneralSettingsManager,
): Boolean {
    val privacySettings = generalSettingsManager.getConfig().privacy
    return privacySettings.isHideNotificationContent || account.notificationSettings.isContentHidden
}
