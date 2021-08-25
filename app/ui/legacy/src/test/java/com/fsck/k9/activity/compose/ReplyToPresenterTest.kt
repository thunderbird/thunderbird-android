package com.fsck.k9.activity.compose

import android.os.Bundle
import com.fsck.k9.Identity
import com.fsck.k9.K9RobolectricTest
import com.fsck.k9.mail.Address
import com.fsck.k9.view.RecipientSelectView.Recipient
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.eq
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

private val REPLY_TO_ADDRESS = "reply-to@example.com"
private val REPLY_TO_ADDRESS_2 = "reply-to2@example.com"
private val REPLY_TO_ADDRESS_3 = "reply-to3@example.com"

class ReplyToPresenterTest : K9RobolectricTest() {
    private val replyToPresenter: ReplyToPresenter
    private val view: ReplyToView

    init {
        view = mock()
        replyToPresenter = ReplyToPresenter(view)
    }

    @Test
    fun testInstanceState_expectStoreVisibility() {
        val state = mock<Bundle>()
        `when`(view.isVisible()).thenReturn(true)
        `when`(state.getBoolean(any())).thenReturn(true)

        replyToPresenter.onSaveInstaceState(state)
        replyToPresenter.onRestoreInstanceState(state)

        val stringCaptor = argumentCaptor<String>()
        verify(state).putBoolean(stringCaptor.capture(), eq(true))
        verify(state).getBoolean(stringCaptor.firstValue)
        verify(view).isVisible()
        verify(view).setVisible(true)
        verifyNoMoreInteractions(view, state)
    }

    @Test
    fun testGetAddresses_expectAddressesFromView() {
        val array = arrayOf(mock<Address>())
        `when`(view.getAddresses()).thenReturn(array)

        val result = replyToPresenter.getAddresses()

        assertThat(result).isSameInstanceAs(array)
    }

    @Test
    fun testHasUncompletedRecipients_onlyCompleteAddresses_expectTrue() {
        `when`(view.hasUncompletedText()).thenReturn(false)

        assertThat(replyToPresenter.hasUncompletedRecipients()).isFalse()

        verify(view).hasUncompletedText()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun testHasUncompletedRecipients_notCompleteAddresses_expectFalse() {
        `when`(view.hasUncompletedText()).thenReturn(true)

        assertThat(replyToPresenter.hasUncompletedRecipients()).isTrue()

        verify(view).hasUncompletedText()
        verify(view).showError()
        verify(view).setVisible(true)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun testSetIdentity_identityWithOneReplyTo_expectSetReplyTo() {
        replyToPresenter.setIdentity(Identity("a", "b", "x@y.z", null, false, REPLY_TO_ADDRESS))

        val recipientsCaptor = argumentCaptor<Recipient>()
        verify(view).addRecipients(recipientsCaptor.capture())
        assertThat(recipientsCaptor.allValues.size).isEqualTo(1)
        assertThat(recipientsCaptor.firstValue.address.address).isEqualTo(REPLY_TO_ADDRESS)
    }

    @Test
    fun testSetIdentity_identityWithMultipleReplyTo_expectSetReplyTo() {
        replyToPresenter.setIdentity(Identity("a", "b", "x@y.z", null, false, REPLY_TO_ADDRESS + ", " + REPLY_TO_ADDRESS_2))

        val recipientsCaptor = argumentCaptor<Recipient>()
        verify(view, atLeastOnce()).addRecipients(recipientsCaptor.capture())
        assertThat(recipientsCaptor.allValues.size).isEqualTo(2)
        assertThat(recipientsCaptor.firstValue.address.address).isEqualTo(REPLY_TO_ADDRESS)
        assertThat(recipientsCaptor.secondValue.address.address).isEqualTo(REPLY_TO_ADDRESS_2)
    }

    @Test
    fun testOnSwitchIdentity_newIdentityWithoutReplyTo_expectRemoveReplyToOfOldIdentity() {
        `when`(view.getRecipients()).thenReturn(listOf(createRecipient(REPLY_TO_ADDRESS)))

        replyToPresenter.onSwitchIdentity(Identity("a", "b", "x@y.z", null, false, REPLY_TO_ADDRESS), Identity())

        verify(view).getRecipients()
        val captor = argumentCaptor<List<Recipient>>()
        verify(view).removeRecipients(captor.capture())
        assertThat(captor.allValues.size).isEqualTo(1)
        assertThat(captor.firstValue.size).isEqualTo(1)
        assertThat(captor.firstValue.first().address.address).isEqualTo(REPLY_TO_ADDRESS)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun testOnSwitchIdentity_identityWithSubsetOfOldIdentity_expectRemoveThenAdd() {
        `when`(view.getRecipients()).thenReturn(listOf(createRecipient(REPLY_TO_ADDRESS), createRecipient(REPLY_TO_ADDRESS_2)))

        replyToPresenter.onSwitchIdentity(
            Identity("a", "b", "x@y.z", null, false, REPLY_TO_ADDRESS + ", " + REPLY_TO_ADDRESS_2),
            Identity("c", "d", "x@y.z", null, false, REPLY_TO_ADDRESS + ", " + REPLY_TO_ADDRESS_3)
        )

        verify(view).getRecipients()
        val captor = argumentCaptor<List<Recipient>>()
        verify(view).removeRecipients(captor.capture())
        assertThat(captor.allValues.size).isEqualTo(1)
        assertThat(captor.firstValue.size).isEqualTo(2)
        assertThat(captor.firstValue.get(0).address.address).isEqualTo(REPLY_TO_ADDRESS)
        assertThat(captor.firstValue.get(1).address.address).isEqualTo(REPLY_TO_ADDRESS_2)
        val recipientsCaptor = argumentCaptor<Recipient>()
        verify(view, atLeastOnce()).addRecipients(recipientsCaptor.capture())
        assertThat(recipientsCaptor.allValues.size).isEqualTo(2)
        assertThat(recipientsCaptor.firstValue.address.address).isEqualTo(REPLY_TO_ADDRESS)
        assertThat(recipientsCaptor.secondValue.address.address).isEqualTo(REPLY_TO_ADDRESS_3)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun testOnNonRecipientFieldFocused_notVisible_expectNoChange() {
        `when`(view.isVisible()).thenReturn(false)

        replyToPresenter.onNonRecipientFieldFocused()

        verify(view).isVisible()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun testOnNonRecipientFieldFocused_noContentFieldVisible_expectHide() {
        `when`(view.isVisible()).thenReturn(true)
        `when`(view.getAddresses()).thenReturn(arrayOf())

        replyToPresenter.onNonRecipientFieldFocused()

        verify(view).isVisible()
        verify(view).getAddresses()
        verify(view).setVisible(false)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun testOnNonRecipientFieldFocused_withContentFieldVisible_expectNoChange() {
        `when`(view.isVisible()).thenReturn(true)
        `when`(view.getAddresses()).thenReturn(arrayOf(mock()))

        replyToPresenter.onNonRecipientFieldFocused()

        verify(view).isVisible()
        verify(view).getAddresses()
        verifyNoMoreInteractions(view)
    }

    private fun createRecipient(mailAddress: String): Recipient {
        return Recipient(Address(mailAddress))
    }
}
