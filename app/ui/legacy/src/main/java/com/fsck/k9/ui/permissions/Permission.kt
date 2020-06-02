package com.fsck.k9.ui.permissions

import android.Manifest
import androidx.annotation.StringRes
import com.fsck.k9.ui.R

private const val PERMISSIONS_REQUEST_READ_CONTACTS = 1

enum class Permission(
    val permission: String,
    val requestCode: Int,
    @param:StringRes val rationaleTitle: Int,
    @param:StringRes val rationaleMessage: Int
) {
    READ_CONTACTS(
        Manifest.permission.READ_CONTACTS,
        PERMISSIONS_REQUEST_READ_CONTACTS,
        R.string.permission_contacts_rationale_title,
        R.string.permission_contacts_rationale_message
    );
}
