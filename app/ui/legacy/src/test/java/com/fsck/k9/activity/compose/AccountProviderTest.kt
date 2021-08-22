package com.fsck.k9.activity.compose

import com.fsck.k9.Account
import com.fsck.k9.Identity
import com.fsck.k9.K9
import com.fsck.k9.MissingAccoutException
import com.fsck.k9.Preferences
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.mail.internet.TextBody
import com.fsck.k9.mailstore.AttachmentResolver
import com.fsck.k9.mailstore.MessageViewInfo
import com.fsck.k9.message.IdentityHeaderBuilder
import com.fsck.k9.message.QuotedTextMode
import com.fsck.k9.message.SimpleMessageFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AccountProviderTest {
    private val ACCOUNT_UUID = "uuid"
    private val IDENTITY_A_DESCRIPTION = "identity a description"
    private val IDENTITY_A_NAME = "identity a name"
    private val IDENTITY_A_EMAIL = "a@test.org"
    private val IDENTITY_A_SIGNATURE = "\n--\nA"
    private val IDENTITY_A_REPLYTO = "a-reply@test.org"
    private val IDENTITY_B_DESCRIPTION = "identity b description"
    private val IDENTITY_B_NAME = "identity b name"
    private val IDENTITY_B_EMAIL = "b@test.org"
    private val IDENTITY_B_SIGNATURE = "\n--\nB"
    private val IDENTITY_B_REPLYTO = "b-reply@test.org"
    private val preferences = mock(Preferences::class.java)

    @Test
    fun constructor_noUuid_expectDefaultAccount() {
        val defaultAccount = createAccount()
        `when`(preferences.defaultAccount).thenReturn(defaultAccount)

        val toTest = AccountProvider(preferences, null)

        assertEquals(defaultAccount, toTest.account)
        assertFalse(toTest.hasIdentityChanged())
    }

    @Test
    fun constructor_existingUuid_expectAccount() {
        val account = createAccount()
        `when`(preferences.getAccount(ACCOUNT_UUID)).thenReturn(account)

        val toTest = AccountProvider(preferences, ACCOUNT_UUID)

        assertEquals(account, toTest.account)
        assertFalse(toTest.hasIdentityChanged())
    }

    @Test(expected = MissingAccoutException::class)
    fun constructor_noExistingUuidAndNoDefaultAccount_expectException() {
        AccountProvider(preferences, ACCOUNT_UUID)
    }

    @Test
    fun onAccountChosen_sameAccount_expectNoCallbackCall() {
        val defaultAccount = createAccount()
        `when`(preferences.getAccount(ACCOUNT_UUID)).thenReturn(defaultAccount)
        val toTest = AccountProvider(preferences, ACCOUNT_UUID)
        val callback = mock(OnAccountSelectedCallback::class.java)

        toTest.onAccountChosen(defaultAccount, callback)

        verifyNoMoreInteractions(callback)
    }

    @Test
    fun onAccountChosen_changedAccount_expectCallbackCall() {
        val defaultAccount = createAccount()
        `when`(preferences.getAccount(ACCOUNT_UUID)).thenReturn(defaultAccount)
        val toTest = AccountProvider(preferences, ACCOUNT_UUID)
        val newAccount = Account("new_uuid")
        val callback = mock(OnAccountSelectedCallback::class.java)

        toTest.onAccountChosen(newAccount, callback)

        verify(callback).accountChosen(defaultAccount, newAccount)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun setIdentity_expectIdentityChangedTrue() {
        val defaultAccount = createAccount()
        `when`(preferences.getAccount(ACCOUNT_UUID)).thenReturn(defaultAccount)
        val toTest = AccountProvider(preferences, ACCOUNT_UUID)

        toTest.identity = Identity()

        assertTrue(toTest.hasIdentityChanged())
    }

    @Test
    fun updateAccountAndIdentity_unknownIdentity() {
        val account = createAccount()
        `when`(preferences.getAccount(ACCOUNT_UUID)).thenReturn(account)
        val toTest = AccountProvider(preferences, ACCOUNT_UUID)
        val callback = mock(OnIdentityUpdateFinishedCallback::class.java)
        val message = MimeMessage()
        val newIdentityDescription = "new identity description"
        val newIdentityName = "new identity name"
        val email = "x@y.z"
        val newIdentity = Identity(newIdentityDescription, newIdentityName, email)
        message.addHeader(
            K9.IDENTITY_HEADER,
            IdentityHeaderBuilder()
                .setBody(TextBody("body"))
                .setIdentity(newIdentity)
                .setIdentityChanged(true)
                .setMessageFormat(SimpleMessageFormat.HTML)
                .setQuoteTextMode(QuotedTextMode.SHOW)
                .setQuoteStyle(Account.QuoteStyle.HEADER)
                .build()
        )
        val messageViewInfo = createMessageViewInfo(message)

        toTest.updateAccountAndIdentity(messageViewInfo, message, callback)

        assertEquals(newIdentityName, toTest.identity.name)
        assertEquals(email, toTest.identity.email)
        assertFalse(toTest.identity.signatureUse)
        assertNull(toTest.identity.replyTo)
    }

    @Test
    fun updateAccountAndIdentity_firstIdentity_noSignatureChange() {
        val account = createAccount()
        `when`(preferences.getAccount(ACCOUNT_UUID)).thenReturn(account)
        val toTest = AccountProvider(preferences, ACCOUNT_UUID)
        val callback = mock(OnIdentityUpdateFinishedCallback::class.java)
        val message = MimeMessage()
        val identity = Identity(IDENTITY_A_DESCRIPTION, IDENTITY_A_NAME, IDENTITY_A_EMAIL)
        message.addHeader(
            K9.IDENTITY_HEADER,
            IdentityHeaderBuilder()
                .setBody(TextBody("body"))
                .setIdentity(identity)
                .setIdentityChanged(false)
                .setMessageFormat(SimpleMessageFormat.HTML)
                .setQuoteTextMode(QuotedTextMode.SHOW)
                .setQuoteStyle(Account.QuoteStyle.HEADER)
                .build()
        )
        message.addHeader("Reply-to", IDENTITY_A_REPLYTO)
        message.replyTo[0] = Address(IDENTITY_A_REPLYTO)
        val messageViewInfo = createMessageViewInfo(message)

        toTest.updateAccountAndIdentity(messageViewInfo, message, callback)

        assertEquals(IDENTITY_A_NAME, toTest.identity.name)
        assertEquals(IDENTITY_A_EMAIL, toTest.identity.email)
        assertEquals(IDENTITY_A_SIGNATURE, toTest.identity.signature)
    }

    @Test
    fun updateAccountAndIdentity_firstIdentity_withSignatureChange() {
        val account = createAccount()
        `when`(preferences.getAccount(ACCOUNT_UUID)).thenReturn(account)
        val toTest = AccountProvider(preferences, ACCOUNT_UUID)
        val callback = mock(OnIdentityUpdateFinishedCallback::class.java)
        val message = MimeMessage()
        val newSignature = "\n--\nnew signature"
        val identity = Identity(
            IDENTITY_A_DESCRIPTION, IDENTITY_A_NAME, IDENTITY_A_EMAIL, newSignature, true,
            IDENTITY_A_REPLYTO
        )
        message.addHeader(
            K9.IDENTITY_HEADER,
            IdentityHeaderBuilder()
                .setBody(TextBody("body"))
                .setIdentity(identity)
                .setIdentityChanged(false)
                .setMessageFormat(SimpleMessageFormat.HTML)
                .setQuoteTextMode(QuotedTextMode.SHOW)
                .setQuoteStyle(Account.QuoteStyle.HEADER)
                .setSignatureChanged(true)
                .setSignature(newSignature)
                .build()
        )
        message.addHeader("Reply-to", IDENTITY_A_REPLYTO)
        message.replyTo[0] = Address(IDENTITY_A_REPLYTO)
        val messageViewInfo = createMessageViewInfo(message)

        toTest.updateAccountAndIdentity(messageViewInfo, message, callback)

        assertEquals(IDENTITY_A_NAME, toTest.identity.name)
        assertEquals(IDENTITY_A_EMAIL, toTest.identity.email)
        assertEquals(newSignature, toTest.identity.signature)
        assertTrue(toTest.identity.signatureUse)
    }

    @Test
    fun updateAccountAndIdentity_secondIdentity_noSignatureChange() {
        val account = createAccount()
        `when`(preferences.getAccount(ACCOUNT_UUID)).thenReturn(account)
        val toTest = AccountProvider(preferences, ACCOUNT_UUID)
        val callback = mock(OnIdentityUpdateFinishedCallback::class.java)
        val message = MimeMessage()
        val newIdentity = Identity(IDENTITY_B_DESCRIPTION, IDENTITY_B_NAME, IDENTITY_B_EMAIL)
        message.addHeader(
            K9.IDENTITY_HEADER,
            IdentityHeaderBuilder()
                .setBody(TextBody("body"))
                .setIdentity(newIdentity)
                .setIdentityChanged(true)
                .setMessageFormat(SimpleMessageFormat.HTML)
                .setQuoteTextMode(QuotedTextMode.SHOW)
                .setQuoteStyle(Account.QuoteStyle.HEADER)
                .build()
        )
        message.addHeader("Reply-to", IDENTITY_B_REPLYTO)
        message.replyTo[0] = Address(IDENTITY_B_REPLYTO)
        val messageViewInfo = createMessageViewInfo(message)

        toTest.updateAccountAndIdentity(messageViewInfo, message, callback)

        assertEquals(IDENTITY_B_NAME, toTest.identity.name)
        assertEquals(IDENTITY_B_EMAIL, toTest.identity.email)
    }

    @Test
    fun updateAccountAndIdentity_secondIdentity_withSignatureChange() {
        val account = createAccount()
        `when`(preferences.getAccount(ACCOUNT_UUID)).thenReturn(account)
        val toTest = AccountProvider(preferences, ACCOUNT_UUID)
        val callback = mock(OnIdentityUpdateFinishedCallback::class.java)
        val message = MimeMessage()
        val newSignature = "\n--\nnew signature"
        val newIdentity = Identity(
            IDENTITY_B_DESCRIPTION, IDENTITY_B_NAME, IDENTITY_B_EMAIL, newSignature, true,
            IDENTITY_B_REPLYTO
        )
        message.addHeader(
            K9.IDENTITY_HEADER,
            IdentityHeaderBuilder()
                .setBody(TextBody("body"))
                .setIdentity(newIdentity)
                .setIdentityChanged(true)
                .setMessageFormat(SimpleMessageFormat.HTML)
                .setQuoteTextMode(QuotedTextMode.SHOW)
                .setQuoteStyle(Account.QuoteStyle.HEADER)
                .setSignatureChanged(true)
                .setSignature(newSignature)
                .build()
        )
        message.addHeader("Reply-to", IDENTITY_B_REPLYTO)
        message.replyTo[0] = Address(IDENTITY_B_REPLYTO)
        val messageViewInfo = createMessageViewInfo(message)

        toTest.updateAccountAndIdentity(messageViewInfo, message, callback)

        assertEquals(IDENTITY_B_NAME, toTest.identity.name)
        assertEquals(IDENTITY_B_EMAIL, toTest.identity.email)
        assertEquals(newSignature, toTest.identity.signature)
        assertTrue(toTest.identity.signatureUse)
    }

    private fun createAccount(): Account {
        val account = Account(ACCOUNT_UUID)
        val identityList = ArrayList<Identity>()
        identityList.add(
            Identity(
                IDENTITY_A_DESCRIPTION, IDENTITY_A_NAME, IDENTITY_A_EMAIL, IDENTITY_A_SIGNATURE,
                true, IDENTITY_A_REPLYTO
            )
        )
        identityList.add(
            Identity(
                IDENTITY_B_DESCRIPTION, IDENTITY_B_NAME, IDENTITY_B_EMAIL, IDENTITY_B_SIGNATURE,
                true, IDENTITY_B_REPLYTO
            )
        )
        account.identities = identityList
        return account
    }

    private fun createMessageViewInfo(message: MimeMessage): MessageViewInfo {
        val messageViewInfo = MessageViewInfo(
            message,
            false,
            message, "subject",
            false,
            "text",
            ArrayList(),
            null,
            mock(AttachmentResolver::class.java),
            null,
            ArrayList()
        )
        return messageViewInfo
    }
}
