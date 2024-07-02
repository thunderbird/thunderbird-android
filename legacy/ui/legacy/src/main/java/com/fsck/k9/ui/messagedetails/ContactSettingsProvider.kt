package com.fsck.k9.ui.messagedetails

import com.fsck.k9.K9

class ContactSettingsProvider {
    val isShowContactPicture: Boolean
        get() = K9.isShowContactPicture
}
