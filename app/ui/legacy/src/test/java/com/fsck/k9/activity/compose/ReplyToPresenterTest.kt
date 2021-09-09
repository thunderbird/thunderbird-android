package com.fsck.k9.activity.compose

import android.os.Bundle
import com.fsck.k9.Identity
import com.fsck.k9.K9RobolectricTest
import com.fsck.k9.mail.Address
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stubbing
import org.mockito.kotlin.verify

private const val REPLY_TO_ADDRESS = "reply-to@example.com"
private const val REPLY_TO_ADDRESS_2 = "reply-to2@example.com"
private const val REPLY_TO_ADDRESS_3 = "reply-to3@example.com"

class ReplyToPresenterTest : K9RobolectricTest() {
    private val view = mock<ReplyToView>()
    private val replyToPresenter = ReplyToPresenter(view)

    @Test
    fun testInstanceState_expectStoreVisibility() {
        val initialView = mock<ReplyToView> {
            on { isVisible } doReturn true
        }
        val initialPresenter = ReplyToPresenter(initialView)
        val state = Bundle()
        initialPresenter.onSaveInstanceState(state)

        replyToPresenter.onRestoreInstanceState(state)

        verify(view).isVisible = true
    }

    @Test
    fun testGetAddresses_expectAddressesFromView() {
        val addresses = Address.parse(REPLY_TO_ADDRESS)
        stubbing(view) {
            on { getAddresses() } doReturn addresses
        }

        val result = replyToPresenter.getAddresses()

        assertThat(result).isSameInstanceAs(addresses)
    }

    @Test
    fun testHasUncompletedRecipients_onlyCompleteAddresses_expectTrue() {
        stubbing(view) {
            on { hasUncompletedText() } doReturn false
        }

        val result = replyToPresenter.isNotReadyForSending()

        assertThat(result).isFalse()
    }

    @Test
    fun testHasUncompletedRecipients_notCompleteAddresses_expectFalse() {
        stubbing(view) {
            on { hasUncompletedText() } doReturn true
        }

        val result = replyToPresenter.isNotReadyForSending()

        assertThat(result).isTrue()
        verify(view).showError()
        verify(view).isVisible = true
    }

    @Test
    fun testSetIdentity_identityWithOneReplyTo_expectSetReplyTo() {
        val identity = Identity("a", "b", "x@y.z", null, false, REPLY_TO_ADDRESS)

        replyToPresenter.setIdentity(identity)

        verify(view).silentlyAddAddresses(Address.parse(REPLY_TO_ADDRESS))
    }

    @Test
    fun testSetIdentity_identityWithMultipleReplyTo_expectSetReplyTo() {
        val replyTo = "$REPLY_TO_ADDRESS, $REPLY_TO_ADDRESS_2"
        val identity = Identity("a", "b", "x@y.z", null, false, replyTo)

        replyToPresenter.setIdentity(identity)

        verify(view).silentlyAddAddresses(Address.parse(replyTo))
    }

    @Test
    fun testOnSwitchIdentity_newIdentityWithoutReplyTo_expectRemoveReplyToOfOldIdentity() {
        val replyToOne = "$REPLY_TO_ADDRESS, $REPLY_TO_ADDRESS_2"
        val identityOne = Identity("a", "b", "x@y.z", null, false, replyToOne)
        val identityTwo = Identity()

        replyToPresenter.setIdentity(identityOne)
        replyToPresenter.setIdentity(identityTwo)

        verify(view).silentlyRemoveAddresses(Address.parse(replyToOne))
    }

    @Test
    fun testOnSwitchIdentity_identityWithSubsetOfOldIdentity_expectRemoveThenAdd() {
        val replyToOne = "$REPLY_TO_ADDRESS, $REPLY_TO_ADDRESS_2"
        val identityOne = Identity("a", "b", "x@y.z", null, false, replyToOne)
        val replyToTwo = "$REPLY_TO_ADDRESS, $REPLY_TO_ADDRESS_3"
        val identityTwo = Identity("c", "d", "x@y.z", null, false, replyToTwo)

        replyToPresenter.setIdentity(identityOne)
        replyToPresenter.setIdentity(identityTwo)

        verify(view).silentlyAddAddresses(Address.parse(replyToOne))
        verify(view).silentlyRemoveAddresses(Address.parse(replyToOne))
        verify(view).silentlyAddAddresses(Address.parse(replyToTwo))
    }

    @Test
    fun testOnNonRecipientFieldFocused_notVisible_expectNoChange() {
        stubbing(view) {
            on { isVisible } doReturn false
        }

        replyToPresenter.onNonRecipientFieldFocused()

        verify(view, never()).isVisible = false
    }

    @Test
    fun testOnNonRecipientFieldFocused_noContentFieldVisible_expectHide() {
        stubbing(view) {
            on { isVisible } doReturn true
            on { getAddresses() } doReturn emptyArray()
        }

        replyToPresenter.onNonRecipientFieldFocused()

        verify(view).isVisible = false
    }

    @Test
    fun testOnNonRecipientFieldFocused_withContentFieldVisible_expectNoChange() {
        stubbing(view) {
            on { isVisible } doReturn true
            on { getAddresses() } doReturn Address.parse(REPLY_TO_ADDRESS)
        }

        replyToPresenter.onNonRecipientFieldFocused()

        verify(view, never()).isVisible = false
    }
}
