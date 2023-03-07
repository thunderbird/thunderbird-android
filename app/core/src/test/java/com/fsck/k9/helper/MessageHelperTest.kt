package com.fsck.k9.helper

import android.content.Context
import android.graphics.Color
import android.text.SpannableString
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.fsck.k9.RobolectricTest
import com.fsck.k9.helper.MessageHelper.Companion.toFriendly
import com.fsck.k9.mail.Address
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment

class MessageHelperTest : RobolectricTest() {

    private lateinit var contacts: Contacts
    private lateinit var contactsWithFakeContact: Contacts
    private lateinit var contactsWithFakeSpoofContact: Contacts

    @Before
    fun setUp() {
        val context: Context = RuntimeEnvironment.getApplication()
        contacts = Contacts(context)
        contactsWithFakeContact = object : Contacts(context) {
            override fun getNameForAddress(address: String?): String? {
                return if ("test@testor.com" == address) {
                    "Tim Testor"
                } else {
                    null
                }
            }
        }
        contactsWithFakeSpoofContact = object : Contacts(context) {
            override fun getNameForAddress(address: String?): String? {
                return if ("test@testor.com" == address) {
                    "Tim@Testor"
                } else {
                    null
                }
            }
        }
    }

    @Test
    fun testToFriendlyShowsPersonalPartIfItExists() {
        val address = Address("test@testor.com", "Tim Testor")
        assertThat(toFriendly(address, contacts)).isEqualTo("Tim Testor")
    }

    @Test
    fun testToFriendlyShowsEmailPartIfNoPersonalPartExists() {
        val address = Address("test@testor.com")
        assertThat(toFriendly(address, contacts)).isEqualTo("test@testor.com")
    }

    @Test
    fun testToFriendlyArray() {
        val address1 = Address("test@testor.com", "Tim Testor")
        val address2 = Address("foo@bar.com", "Foo Bar")
        val addresses = arrayOf(address1, address2)
        assertThat(toFriendly(addresses, contacts).toString()).isEqualTo("Tim Testor,Foo Bar")
    }

    @Test
    fun testToFriendlyWithContactLookup() {
        val address = Address("test@testor.com")
        assertThat(toFriendly(address, contactsWithFakeContact)).isEqualTo("Tim Testor")
    }

    @Test
    fun testToFriendlyWithChangeContactColor() {
        val address = Address("test@testor.com")
        val friendly = toFriendly(
            address = address,
            contacts = contactsWithFakeContact,
            showCorrespondentNames = true,
            changeContactNameColor = true,
            contactNameColor = Color.RED,
        )
        assertThat(friendly).isInstanceOf(SpannableString::class.java)
        assertThat(friendly.toString()).isEqualTo("Tim Testor")
    }

    @Test
    fun testToFriendlyWithoutCorrespondentNames() {
        val address = Address("test@testor.com", "Tim Testor")
        val friendly = toFriendly(
            address = address,
            contacts = contactsWithFakeContact,
            showCorrespondentNames = false,
            changeContactNameColor = false,
            contactNameColor = 0,
        )
        assertThat(friendly).isEqualTo("test@testor.com")
    }

    @Test
    fun toFriendly_spoofPreventionOverridesPersonal() {
        val address = Address("test@testor.com", "potus@whitehouse.gov")
        val friendly = toFriendly(address, contacts)
        assertThat(friendly).isEqualTo("test@testor.com")
    }

    @Test
    fun toFriendly_atPrecededByOpeningParenthesisShouldNotTriggerSpoofPrevention() {
        val address = Address("gitlab@gitlab.example", "username (@username)")
        val friendly = toFriendly(address, contacts)
        assertThat(friendly).isEqualTo("username (@username)")
    }

    @Test
    fun toFriendly_nameStartingWithAtShouldNotTriggerSpoofPrevention() {
        val address = Address("address@domain.example", "@username")
        val friendly = toFriendly(address, contacts)
        assertThat(friendly).isEqualTo("@username")
    }

    @Test
    fun toFriendly_spoofPreventionDoesntOverrideContact() {
        val address = Address("test@testor.com", "Tim Testor")
        val friendly = toFriendly(
            address = address,
            contacts = contactsWithFakeSpoofContact,
            showCorrespondentNames = true,
            changeContactNameColor = false,
            contactNameColor = 0,
        )
        assertThat(friendly).isEqualTo("Tim@Testor")
    }
}
