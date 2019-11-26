package com.fsck.k9

import android.os.Parcel
import android.os.Parcelable

data class Identity(
        val description: String? = null,
        val name: String? = null,
        val email: String? = null,
        val signature: String? = null,
        val signatureUse: Boolean = false,
        val replyTo: String? = null
) : Parcelable {
    override fun toString(): String {
        return "Account.Identity(description=" + description + ", name=" + name + ", email=" + email + ", replyTo=" + replyTo + ", signature=" +
                signature
    }

    // TODO remove when callers are converted to Kotlin
    fun withName(name: String?) = copy(name = name)
    fun withSignature(signature: String?) = copy(signature = signature)
    fun withSignatureUse(signatureUse: Boolean) = copy(signatureUse = signatureUse)
    fun withEmail(email: String?) = copy(email = email)

    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readByte() != 0.toByte(),
            parcel.readString())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(description)
        parcel.writeString(name)
        parcel.writeString(email)
        parcel.writeString(signature)
        parcel.writeByte(if (signatureUse) 1 else 0)
        parcel.writeString(replyTo)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Identity> {
        override fun createFromParcel(parcel: Parcel): Identity {
            return Identity(parcel)
        }

        override fun newArray(size: Int): Array<Identity?> {
            return arrayOfNulls(size)
        }
    }
}
