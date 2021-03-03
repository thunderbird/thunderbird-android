package com.fsck.k9.activity.setup

import android.os.Parcelable
import com.fsck.k9.mail.AuthType
import kotlinx.parcelize.Parcelize

@Parcelize
data class InitialAccountSettings(
    val authenticationType: AuthType,
    val email: String,
    val password: String?,
    val clientCertificateAlias: String?
) : Parcelable
