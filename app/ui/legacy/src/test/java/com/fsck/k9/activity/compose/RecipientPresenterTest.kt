package com.fsck.k9.activity.compose

import androidx.test.core.app.ApplicationProvider
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.fsck.k9.Account
import com.fsck.k9.K9RobolectricTest
import com.fsck.k9.activity.compose.RecipientMvpView.CryptoSpecialModeDisplayType
import com.fsck.k9.activity.compose.RecipientMvpView.CryptoStatusDisplayType
import com.fsck.k9.activity.compose.RecipientPresenter.CryptoMode
import com.fsck.k9.autocrypt.AutocryptDraftStateHeaderParser
import com.fsck.k9.helper.ReplyToParser
import com.fsck.k9.helper.ReplyToParser.ReplyToAddresses
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.Message.RecipientType
import com.fsck.k9.message.AutocryptStatusInteractor
import com.fsck.k9.message.AutocryptStatusInteractor.RecipientAutocryptStatus
import com.fsck.k9.message.AutocryptStatusInteractor.RecipientAutocryptStatusType
import com.fsck.k9.message.ComposePgpEnableByDefaultDecider
import com.fsck.k9.message.ComposePgpInlineDecider
import com.fsck.k9.view.RecipientSelectView.Recipient
import kotlin.test.assertNotNull
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.koin.test.inject
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stubbing
import org.openintents.openpgp.OpenPgpApiManager
import org.openintents.openpgp.OpenPgpApiManager.OpenPgpApiManagerCallback
import org.openintents.openpgp.OpenPgpApiManager.OpenPgpProviderState
import org.openintents.openpgp.util.OpenPgpApi
import org.robolectric.Robolectric
import org.robolectric.annotation.LooperMode

private val TO_ADDRESS = Address("to@domain.example")
private val CC_ADDRESS = Address("cc@domain.example")
private const val CRYPTO_PROVIDER = "crypto_provider"
private const val CRYPTO_KEY_ID = 123L

@LooperMode(LooperMode.Mode.LEGACY)
class RecipientPresenterTest : K9RobolectricTest() {
    private val openPgpApiManager = mock<OpenPgpApiManager> {
        on { openPgpProviderState } doReturn OpenPgpProviderState.UNCONFIGURED
        on { setOpenPgpProvider(any(), any()) } doAnswer { invocation ->
            openPgpApiManagerCallback = invocation.getArgument(1)
        }
    }
    private val recipientMvpView = mock<RecipientMvpView>()
    private val account = mock<Account>()
    private val composePgpInlineDecider = mock<ComposePgpInlineDecider>()
    private val composePgpEnableByDefaultDecider = mock<ComposePgpEnableByDefaultDecider>()
    private val autocryptStatusInteractor = mock<AutocryptStatusInteractor>()
    private val replyToParser = mock<ReplyToParser>()
    private val autocryptDraftStateHeaderParser: AutocryptDraftStateHeaderParser by inject()
    private lateinit var recipientPresenter: RecipientPresenter

    private val noRecipientsAutocryptResult = RecipientAutocryptStatus(RecipientAutocryptStatusType.NO_RECIPIENTS, null)
    private var openPgpApiManagerCallback: OpenPgpApiManagerCallback? = null

    @Before
    fun setUp() {
        Robolectric.getBackgroundThreadScheduler().pause()

        recipientPresenter = RecipientPresenter(
            ApplicationProvider.getApplicationContext(),
            mock(),
            openPgpApiManager,
            recipientMvpView,
            account,
            composePgpInlineDecider,
            composePgpEnableByDefaultDecider,
            autocryptStatusInteractor,
            replyToParser,
            autocryptDraftStateHeaderParser,
        )
    }

    @Test
    @Ignore("It looks like the support version of AsyncTaskLoader handles background tasks differently")
    fun testInitFromReplyToMessage() {
        val message = mock<Message>()
        stubbing(replyToParser) {
            on { getRecipientsToReplyTo(message, account) } doReturn ReplyToAddresses(arrayOf(TO_ADDRESS))
        }

        recipientPresenter.initFromReplyToMessage(message, false)
        runBackgroundTask()

        verify(recipientMvpView).addRecipients(eq(RecipientType.TO), eq(Recipient(TO_ADDRESS)))
    }

    @Test
    @Ignore("It looks like the support version of AsyncTaskLoader handles background tasks differently")
    fun testInitFromReplyToAllMessage() {
        val message = mock<Message>()
        val replyToAddresses = ReplyToAddresses(listOf(TO_ADDRESS), listOf(CC_ADDRESS))
        stubbing(replyToParser) {
            on { getRecipientsToReplyAllTo(message, account) } doReturn replyToAddresses
        }

        recipientPresenter.initFromReplyToMessage(message, true)
        runBackgroundTask()
        runBackgroundTask()

        verify(recipientMvpView).addRecipients(eq(RecipientType.TO), eq(Recipient(TO_ADDRESS)))
        verify(recipientMvpView).addRecipients(eq(RecipientType.CC), eq(Recipient(CC_ADDRESS)))
    }

    @Test
    fun initFromReplyToMessage_shouldCallComposePgpInlineDecider() {
        val message = mock<Message>()
        stubbing(replyToParser) {
            on { getRecipientsToReplyTo(message, account) } doReturn ReplyToAddresses(arrayOf(TO_ADDRESS))
        }

        recipientPresenter.initFromReplyToMessage(message, false)

        verify(composePgpInlineDecider).shouldReplyInline(message)
    }

    @Test
    fun getCurrentCryptoStatus_withoutCryptoProvider() {
        stubbing(openPgpApiManager) {
            on { openPgpProviderState } doReturn OpenPgpProviderState.UNCONFIGURED
        }

        recipientPresenter.asyncUpdateCryptoStatus()

        assertNotNull(recipientPresenter.currentCachedCryptoStatus) { status ->
            assertThat(status.displayType).isEqualTo(CryptoStatusDisplayType.UNCONFIGURED)
            assertThat(status.specialModeDisplayType).isEqualTo(CryptoSpecialModeDisplayType.NONE)
            assertThat(status.attachErrorStateOrNull).isNull()
            assertThat(status.isProviderStateOk()).isFalse()
            assertThat(status.isOpenPgpConfigured).isFalse()
        }
    }

    @Test
    fun getCurrentCryptoStatus_withCryptoProvider() {
        setupCryptoProvider(noRecipientsAutocryptResult)

        assertNotNull(recipientPresenter.currentCachedCryptoStatus) { status ->
            assertThat(status.displayType).isEqualTo(CryptoStatusDisplayType.UNAVAILABLE)
            assertThat(status.isProviderStateOk()).isTrue()
            assertThat(status.isOpenPgpConfigured).isTrue()
        }
    }

    @Test
    fun getCurrentCryptoStatus_withOpportunistic() {
        val recipientAutocryptStatus = RecipientAutocryptStatus(
            RecipientAutocryptStatusType.AVAILABLE_UNCONFIRMED,
            null,
        )

        setupCryptoProvider(recipientAutocryptStatus)

        assertNotNull(recipientPresenter.currentCachedCryptoStatus) { status ->
            assertThat(status.displayType).isEqualTo(CryptoStatusDisplayType.AVAILABLE)
            assertThat(status.isProviderStateOk()).isTrue()
            assertThat(status.isOpenPgpConfigured).isTrue()
        }
    }

    @Test
    fun getCurrentCryptoStatus_withOpportunistic__confirmed() {
        val recipientAutocryptStatus = RecipientAutocryptStatus(
            RecipientAutocryptStatusType.AVAILABLE_CONFIRMED,
            null,
        )

        setupCryptoProvider(recipientAutocryptStatus)

        assertNotNull(recipientPresenter.currentCachedCryptoStatus) { status ->
            assertThat(status.displayType).isEqualTo(CryptoStatusDisplayType.AVAILABLE)
            assertThat(status.isProviderStateOk()).isTrue()
            assertThat(status.isOpenPgpConfigured).isTrue()
        }
    }

    @Test
    fun getCurrentCryptoStatus_withOpportunistic__missingKeys() {
        val recipientAutocryptStatus = RecipientAutocryptStatus(
            RecipientAutocryptStatusType.UNAVAILABLE,
            null,
        )

        setupCryptoProvider(recipientAutocryptStatus)

        assertNotNull(recipientPresenter.currentCachedCryptoStatus) { status ->
            assertThat(status.displayType).isEqualTo(CryptoStatusDisplayType.UNAVAILABLE)
            assertThat(status.isProviderStateOk()).isTrue()
            assertThat(status.isOpenPgpConfigured).isTrue()
        }
    }

    @Test
    fun getCurrentCryptoStatus_withOpportunistic__privateMissingKeys() {
        val recipientAutocryptStatus = RecipientAutocryptStatus(
            RecipientAutocryptStatusType.UNAVAILABLE,
            null,
        )

        setupCryptoProvider(recipientAutocryptStatus)
        recipientPresenter.onCryptoModeChanged(CryptoMode.CHOICE_ENABLED)
        runBackgroundTask()

        assertNotNull(recipientPresenter.currentCachedCryptoStatus) { status ->
            assertThat(status.displayType).isEqualTo(CryptoStatusDisplayType.ENABLED_ERROR)
            assertThat(status.isProviderStateOk()).isTrue()
            assertThat(status.isOpenPgpConfigured).isTrue()
        }
    }

    @Test
    fun getCurrentCryptoStatus_withModeDisabled() {
        val recipientAutocryptStatus = RecipientAutocryptStatus(
            RecipientAutocryptStatusType.AVAILABLE_UNCONFIRMED,
            null,
        )

        setupCryptoProvider(recipientAutocryptStatus)
        recipientPresenter.onCryptoModeChanged(CryptoMode.CHOICE_DISABLED)
        runBackgroundTask()

        assertNotNull(recipientPresenter.currentCachedCryptoStatus) { status ->
            assertThat(status.displayType).isEqualTo(CryptoStatusDisplayType.AVAILABLE)
            assertThat(status.isProviderStateOk()).isTrue()
            assertThat(status.isOpenPgpConfigured).isTrue()
        }
    }

    @Test
    fun getCurrentCryptoStatus_withModePrivate() {
        val recipientAutocryptStatus = RecipientAutocryptStatus(
            RecipientAutocryptStatusType.AVAILABLE_UNCONFIRMED,
            null,
        )

        setupCryptoProvider(recipientAutocryptStatus)
        recipientPresenter.onCryptoModeChanged(CryptoMode.CHOICE_ENABLED)
        runBackgroundTask()

        assertNotNull(recipientPresenter.currentCachedCryptoStatus) { status ->
            assertThat(status.displayType).isEqualTo(CryptoStatusDisplayType.ENABLED)
            assertThat(status.isProviderStateOk()).isTrue()
            assertThat(status.isOpenPgpConfigured).isTrue()
        }
    }

    @Test
    fun getCurrentCryptoStatus_withModeSignOnly() {
        setupCryptoProvider(noRecipientsAutocryptResult)

        recipientPresenter.onMenuSetSignOnly(true)
        runBackgroundTask()

        assertNotNull(recipientPresenter.currentCachedCryptoStatus) { status ->
            assertThat(status.displayType).isEqualTo(CryptoStatusDisplayType.SIGN_ONLY)
            assertThat(status.isProviderStateOk()).isTrue()
            assertThat(status.isOpenPgpConfigured).isTrue()
            assertThat(status.isSignOnly).isTrue()
        }
    }

    @Test
    fun getCurrentCryptoStatus_withModeInline() {
        setupCryptoProvider(noRecipientsAutocryptResult)

        recipientPresenter.onMenuSetPgpInline(true)
        runBackgroundTask()

        assertNotNull(recipientPresenter.currentCachedCryptoStatus) { status ->
            assertThat(status.displayType).isEqualTo(CryptoStatusDisplayType.UNAVAILABLE)
            assertThat(status.isProviderStateOk()).isTrue()
            assertThat(status.isPgpInlineModeEnabled).isTrue()
        }
    }

    private fun runBackgroundTask() {
        assertThat(Robolectric.getBackgroundThreadScheduler().runOneTask()).isTrue()
    }

    private fun setupCryptoProvider(autocryptStatusResult: RecipientAutocryptStatus) {
        stubbing(account) {
            on { openPgpProvider } doReturn CRYPTO_PROVIDER
            on { isOpenPgpProviderConfigured } doReturn true
            on { openPgpKey } doReturn CRYPTO_KEY_ID
        }

        recipientPresenter.onSwitchAccount(account)

        val openPgpApiMock = mock<OpenPgpApi>()

        stubbing(autocryptStatusInteractor) {
            on { retrieveCryptoProviderRecipientStatus(eq(openPgpApiMock), any()) } doReturn autocryptStatusResult
        }

        stubbing(openPgpApiManager) {
            on { openPgpApi } doReturn openPgpApiMock
            on { openPgpProviderState } doReturn OpenPgpProviderState.OK
        }

        openPgpApiManagerCallback!!.onOpenPgpProviderStatusChanged()
        runBackgroundTask()
    }
}
