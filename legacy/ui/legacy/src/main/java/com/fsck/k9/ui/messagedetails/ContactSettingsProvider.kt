package com.fsck.k9.ui.messagedetails

import net.thunderbird.core.preference.GeneralSettingsManager

class ContactSettingsProvider(private val generalSettingsManager: GeneralSettingsManager) {
    val isShowContactPicture: Boolean
        get() = generalSettingsManager.getSettings().isShowContactPicture
}
