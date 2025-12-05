package com.fsck.k9.ui.messagedetails

import net.thunderbird.core.preference.display.visualSettings.message.list.MessageListPreferencesManager

class ContactSettingsProvider(private val messageListPreferencesManager: MessageListPreferencesManager) {
    val isShowContactPicture: Boolean
        get() = messageListPreferencesManager.getConfig().isShowContactPicture
}
