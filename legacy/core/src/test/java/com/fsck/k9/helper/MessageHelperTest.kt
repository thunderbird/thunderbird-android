package com.fsck.k9.helper

import android.graphics.Color
import android.text.SpannableString
import app.k9mail.core.android.common.contact.Contact
import app.k9mail.core.android.common.contact.ContactRepository
import app.k9mail.core.common.mail.EmailAddress
import app.k9mail.core.common.mail.toEmailAddressOrThrow
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.fsck.k9.CoreResourceProvider
import com.fsck.k9.TestCoreResourceProvider
import com.fsck.k9.helper.MessageHelper.Companion.toFriendly
import com.fsck.k9.mail.Address
import net.thunderbird.core.android.testing.RobolectricTest
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

class MessageHelperTest : RobolectricTest() {

    private val contactRepository: ContactRepository = mock()
    private val resourceProvider: CoreResourceProvider = TestCoreResourceProvider()
    private val messageHelper: MessageHelper = MessageHelper(resourceProvider, contactRepository)

    @Test
    fun testToFriendlyShowsPersonalPartIfItExists() {
        val address = Address("test@testor.com", "Tim Testor")
        assertThat(toFriendly(address, contactRepository)).isEqualTo("Tim Testor")
    }

    @Test
    fun testToFriendlyShowsEmailPartIfNoPersonalPartExists() {
        val address = Address("test@testor.com")
        assertThat(toFriendly(address, contactRepository)).isEqualTo("test@testor.com")
    }

    @Test
    fun testToFriendlyArray() {
        val address1 = Address("test@testor.com", "Tim Testor")
        val address2 = Address("foo@bar.com", "Foo Bar")
        val addresses = arrayOf(address1, address2)
        assertThat(toFriendly(addresses, contactRepository).toString()).isEqualTo("Tim Testor,Foo Bar")
    }

    @Test
    fun testToFriendlyWithContactLookup() {
        val address = Address(EMAIL_ADDRESS.address)
        setupContactRepositoryWithFakeContact(EMAIL_ADDRESS)

        assertThat(toFriendly(address, contactRepository)).isEqualTo("Tim Testor")
    }

    @Test
    fun testToFriendlyWithChangeContactColor() {
        val address = Address(EMAIL_ADDRESS.address)
        setupContactRepositoryWithFakeContact(EMAIL_ADDRESS)

        val friendly = toFriendly(
            address = address,
            contactRepository = contactRepository,
            showCorrespondentNames = true,
            changeContactNameColor = true,
            contactNameColor = Color.RED,
        )
        assertThat(friendly).isInstanceOf<SpannableString>()
        assertThat(friendly.toString()).isEqualTo("Tim Testor")
    }

    @Test
    fun testToFriendlyWithoutCorrespondentNames() {
        val address = Address(EMAIL_ADDRESS.address, "Tim Testor")
        setupContactRepositoryWithFakeContact(EMAIL_ADDRESS)

        val friendly = toFriendly(
            address = address,
            contactRepository = contactRepository,
            showCorrespondentNames = false,
            changeContactNameColor = false,
            contactNameColor = 0,
        )
        assertThat(friendly).isEqualTo("test@testor.com")
    }

    @Test
    fun toFriendly_spoofPreventionOverridesPersonal() {
        val address = Address("test@testor.com", "potus@whitehouse.gov")
        val friendly = toFriendly(address, contactRepository)
        assertThat(friendly).isEqualTo("test@testor.com")
    }

    @Test
    fun toFriendly_atPrecededByOpeningParenthesisShouldNotTriggerSpoofPrevention() {
        val address = Address("gitlab@gitlab.example", "username (@username)")
        val friendly = toFriendly(address, contactRepository)
        assertThat(friendly).isEqualTo("username (@username)")
    }

    @Test
    fun toFriendly_nameStartingWithAtShouldNotTriggerSpoofPrevention() {
        val address = Address("address@domain.example", "@username")
        val friendly = toFriendly(address, contactRepository)
        assertThat(friendly).isEqualTo("@username")
    }

    @Test
    fun toFriendly_spoofPreventionDoesntOverrideContact() {
        val address = Address(EMAIL_ADDRESS.address, "Tim Testor")
        setupContactRepositoryWithSpoofContact(EMAIL_ADDRESS)

        val friendly = toFriendly(
            address = address,
            contactRepository = contactRepository,
            showCorrespondentNames = true,
            changeContactNameColor = false,
            contactNameColor = 0,
        )
        assertThat(friendly).isEqualTo("Tim@Testor")
    }

    @Test
    fun testGetSenderDisplayNameWithShowContactNameShouldReturnCorrectOutput() {
        val address1 = Address("test@testor.com", "Tim Testor")
        val address2 = Address("foo@bar.com", "Foo Bar")
        val addresses = arrayOf(address1, address2)
        setupContactRepositoryWithFakeContact(EMAIL_ADDRESS)
        val displayName = messageHelper.getRecipientDisplayNames(addresses)
        assertThat(displayName.toString()).isEqualTo("To: Tim Testor,Foo Bar")
    }

    @Test
    fun testGetSenderDisplayNameWithoutShowContactNameShouldReturnCorrectOutput() {
        val address1 = Address("test@testor.com")
        val address2 = Address("foo@bar.com")
        val addresses = arrayOf(address1, address2)

        val displayName = messageHelper.getRecipientDisplayNames(addresses)
        assertThat(displayName.toString()).isEqualTo("To: test@testor.com,foo@bar.com")
    }

    @Test
    fun testGetSenderDisplayNameWithoutInputReturnCorrectOutput() {
        val displayName = messageHelper.getRecipientDisplayNames(null)
        assertThat(displayName.toString()).isEqualTo(resourceProvider.contactUnknownRecipient())
    }

    private fun setupContactRepositoryWithFakeContact(emailAddress: EmailAddress) {
        contactRepository.stub {
            on { getContactFor(emailAddress) } doReturn
                Contact(
                    id = 1L,
                    name = "Tim Testor",
                    emailAddress = emailAddress,
                    uri = mock(),
                    photoUri = null,
                )
        }
    }

    private fun setupContactRepositoryWithSpoofContact(emailAddress: EmailAddress) {
        contactRepository.stub {
            on { getContactFor(emailAddress) } doReturn
                Contact(
                    id = 1L,
                    name = "Tim@Testor",
                    emailAddress = emailAddress,
                    uri = mock(),
                    photoUri = null,
                )
        }
    }

    private companion object {
        val EMAIL_ADDRESS = "test@testor.com".toEmailAddressOrThrow()
    }
}
