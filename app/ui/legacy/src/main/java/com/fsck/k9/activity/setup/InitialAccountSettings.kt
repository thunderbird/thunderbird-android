package com.fsck.k9.activity.setup

import android.os.Parcelable
import com.fsck.k9.mail.AuthType
import kotlinx.parcelize.Parcelize

@Deprecated(
    message = "Remove once all usages have been removed for the new account edit feature",
)
@Parcelize
data class InitialAccountSettings(
    val authenticationType: AuthType,
    val email: String,
    val password: String?,
    val clientCertificateAlias: String?,
) : Parcelable
