package com.fsck.k9.ui.managefolders

internal fun shouldShowPushPreference(isLocalOnly: Boolean, isPushCapable: Boolean): Boolean =
    !isLocalOnly && isPushCapable
