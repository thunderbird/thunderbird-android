package com.fsck.k9.activity.compose

import android.os.Bundle
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isSameInstanceAs
import assertk.assertions.isTrue
import com.fsck.k9.mail.Address
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.testing.RobolectricTest
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stubbing
import org.mockito.kotlin.verify

private const val REPLY_TO_ADDRESS = "reply-to@example.com"
private const val REPLY_TO_ADDRESS_2 = "reply-to2@example.com"
private const val REPLY_TO_ADDRESS_3 = "reply-to3@example.com"

class ReplyToPresenterTest : RobolectricTest() {
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
}
