package com.fsck.k9.helper

import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import app.k9mail.core.android.common.contact.ContactRepository
import app.k9mail.core.common.mail.toEmailAddressOrNull
import com.fsck.k9.CoreResourceProvider
import com.fsck.k9.K9.contactNameColor
import com.fsck.k9.K9.isChangeContactNameColor
import com.fsck.k9.K9.isShowContactName
import com.fsck.k9.K9.isShowCorrespondentNames
import com.fsck.k9.mail.Address
import java.util.regex.Pattern

class MessageHelper(
    private val resourceProvider: CoreResourceProvider,
    private val contactRepository: ContactRepository,
) {

    fun getSenderDisplayName(address: Address?): CharSequence {
        if (address == null) {
            return resourceProvider.contactUnknownSender()
        }
        val repository = if (isShowContactName) contactRepository else null
        return toFriendly(address, repository)
    }

    fun getRecipientDisplayNames(addresses: Array<Address>?): CharSequence {
        if (addresses == null || addresses.isEmpty()) {
            return resourceProvider.contactUnknownRecipient()
        }
        val repository = if (isShowContactName) contactRepository else null
        val recipients = toFriendly(addresses, repository)
        return SpannableStringBuilder(resourceProvider.contactDisplayNamePrefix()).append(' ').append(recipients)
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
        fun toFriendly(address: Address, contactRepository: ContactRepository?): CharSequence {
            return toFriendly(
                address,
                contactRepository,
                isShowCorrespondentNames,
                isChangeContactNameColor,
                contactNameColor,
            )
        }

        fun toFriendly(addresses: Array<Address>?, contactRepository: ContactRepository?): CharSequence? {
            var repository = contactRepository
            if (addresses == null) {
                return null
            }
            if (addresses.size >= TOO_MANY_ADDRESSES) {
                // Don't look up contacts if the number of addresses is very high.
                repository = null
            }
            val stringBuilder = SpannableStringBuilder()
            for (i in addresses.indices) {
                stringBuilder.append(toFriendly(addresses[i], repository))
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
            contactRepository: ContactRepository?,
            showCorrespondentNames: Boolean,
            changeContactNameColor: Boolean,
            contactNameColor: Int,
        ): CharSequence {
            if (!showCorrespondentNames) {
                return address.address
            } else if (contactRepository != null) {
                val name = contactRepository.getContactName(address)
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

        private fun ContactRepository.getContactName(address: Address): String? {
            return address.address.toEmailAddressOrNull()?.let { emailAddress ->
                getContactFor(emailAddress)?.name
            }
        }

        private fun isSpoofAddress(displayName: String): Boolean {
            return displayName.contains("@") && SPOOF_ADDRESS_PATTERN.matcher(displayName).find()
        }
    }
}
