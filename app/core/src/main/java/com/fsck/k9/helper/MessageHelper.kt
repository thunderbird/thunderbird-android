package com.fsck.k9.helper

import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import com.fsck.k9.CoreResourceProvider
import com.fsck.k9.K9.contactNameColor
import com.fsck.k9.K9.isChangeContactNameColor
import com.fsck.k9.K9.isShowContactName
import com.fsck.k9.K9.isShowCorrespondentNames
import com.fsck.k9.mail.Address
import java.util.regex.Pattern

class MessageHelper(
    private val resourceProvider: CoreResourceProvider,
    private val contacts: Contacts,
) {

    fun getSenderDisplayName(address: Address?): CharSequence {
        if (address == null) {
            return resourceProvider.contactUnknownSender()
        }
        val contactHelper = if (isShowContactName) contacts else null
        return toFriendly(address, contactHelper)
    }

    fun getRecipientDisplayNames(addresses: Array<Address>?): CharSequence {
        if (addresses == null || addresses.isEmpty()) {
            return resourceProvider.contactUnknownRecipient()
        }
        val contactHelper = if (isShowContactName) contacts else null
        val recipients = toFriendly(addresses, contactHelper)
        return SpannableStringBuilder(resourceProvider.contactDisplayNamePrefix()).append(recipients)
    }

    companion object {
        /**
         * If the number of addresses exceeds this value the addresses aren't
         * resolved to the names of Android contacts.
         *
         * TODO: This number was chosen arbitrarily and should be determined by performance tests.
         *
         * @see .toFriendly
         */
        private const val TOO_MANY_ADDRESSES = 50
        private val SPOOF_ADDRESS_PATTERN = Pattern.compile("[^(]@")

        /**
         * Returns the name of the contact this email address belongs to if
         * the [contacts][Contacts] parameter is not `null` and a
         * contact is found. Otherwise the personal portion of the [Address]
         * is returned. If that isn't available either, the email address is
         * returned.
         *
         * @param address An [com.fsck.k9.mail.Address]
         * @param contacts A [Contacts] instance or `null`.
         * @return A "friendly" name for this [Address].
         */
        fun toFriendly(address: Address, contacts: Contacts?): CharSequence {
            return toFriendly(
                address,
                contacts,
                isShowCorrespondentNames,
                isChangeContactNameColor,
                contactNameColor,
            )
        }

        fun toFriendly(addresses: Array<Address>?, contacts: Contacts?): CharSequence? {
            var contacts = contacts
            if (addresses == null) {
                return null
            }
            if (addresses.size >= TOO_MANY_ADDRESSES) {
                // Don't look up contacts if the number of addresses is very high.
                contacts = null
            }
            val stringBuilder = SpannableStringBuilder()
            for (i in addresses.indices) {
                stringBuilder.append(toFriendly(addresses[i], contacts))
                if (i < addresses.size - 1) {
                    stringBuilder.append(',')
                }
            }
            return stringBuilder
        }

        /* package, for testing */
        @JvmStatic
        fun toFriendly(
            address: Address,
            contacts: Contacts?,
            showCorrespondentNames: Boolean,
            changeContactNameColor: Boolean,
            contactNameColor: Int,
        ): CharSequence {
            if (!showCorrespondentNames) {
                return address.address
            } else if (contacts != null) {
                val name = contacts.getNameForAddress(address.address)
                if (name != null) {
                    return if (changeContactNameColor) {
                        val coloredName = SpannableString(name)
                        coloredName.setSpan(
                            ForegroundColorSpan(contactNameColor),
                            0,
                            coloredName.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                        )
                        coloredName
                    } else {
                        name
                    }
                }
            }
            return if (!TextUtils.isEmpty(address.personal) && !isSpoofAddress(address.personal)) {
                address.personal
            } else {
                address.address
            }
        }

        private fun isSpoofAddress(displayName: String): Boolean {
            return displayName.contains("@") && SPOOF_ADDRESS_PATTERN.matcher(displayName).find()
        }
    }
}
