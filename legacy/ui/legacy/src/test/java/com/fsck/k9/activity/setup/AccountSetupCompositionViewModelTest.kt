package com.fsck.k9.activity.setup

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import assertk.assertions.prop
import com.fsck.k9.EmailAddressValidator
import com.fsck.k9.FakeLegacyAccount
import com.fsck.k9.activity.account.identity.LegacyIdentitySignatureWebViewConfigurator
import com.fsck.k9.activity.setup.AccountSetupCompositionContract.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.fake.FakeLegacyAccountManager
import net.thunderbird.core.common.resources.StringsResourceManager
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class AccountSetupCompositionViewModelTest {
    private val account = FakeLegacyAccount.create(
        identities = listOf(
            Identity(name = "Old name", email = "old@example.com", signature = "Old signature"),
            Identity(name = "Alias", email = "alias@example.com"),
        ),
    )
    private val accountManager = FakeLegacyAccountManager(listOf(account))

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saving should store sender name, email and signature in the first identity`() {
        // Arrange
        val testSubject = createTestSubject()
        testSubject.event(Event.SenderNameChange("New name"))
        testSubject.event(Event.SenderEmailChange("new@example.com"))
        testSubject.event(Event.UseSignatureChange(true))
        testSubject.event(Event.SignatureChange("New signature"))
        testSubject.event(Event.OnFormatSignatureAsHtmlCheck(true))

        // Act
        testSubject.event(Event.SavePressed)

        // Assert
        val savedAccount = accountManager.findById(account.id)
        assertThat(savedAccount).isNotNull().prop(LegacyAccount::identities).isEqualTo(
            listOf(
                Identity(
                    name = "New name",
                    email = "new@example.com",
                    signature = "New signature",
                    signatureUse = true,
                    signatureIsHtml = true,
                ),
                Identity(name = "Alias", email = "alias@example.com"),
            ),
        )
    }

    @Test
    fun `saving should keep account changes made elsewhere after the screen was opened`() {
        // Arrange
        val testSubject = createTestSubject()
        accountManager.updateSync(account.id) { it.copy(oAuthState = "refreshed") }
        testSubject.event(Event.BccEmailChange("bcc@example.com"))
        testSubject.event(Event.SignatureLocationChange(1 to "Before quoted text"))

        // Act
        testSubject.event(Event.SavePressed)

        // Assert
        assertThat(accountManager.findById(account.id)).isNotNull().all {
            prop(LegacyAccount::oAuthState).isEqualTo("refreshed")
            prop(LegacyAccount::alwaysBcc).isEqualTo("bcc@example.com")
            prop(LegacyAccount::isSignatureBeforeQuotedText).isTrue()
        }
    }

    private fun createTestSubject() = AccountSetupCompositionViewModel(
        legacyAccountManager = accountManager,
        resources = FakeStringsResourceManager(),
        emailAddressValidator = EmailAddressValidator(),
        legacyIdentitySignatureWebViewConfigurator = mock<LegacyIdentitySignatureWebViewConfigurator>(),
        accountUuid = account.id.toString(),
    )

    private class FakeStringsResourceManager : StringsResourceManager {
        override fun stringResource(resourceId: Int): String = ""
        override fun stringResource(resourceId: Int, vararg formatArgs: Any?): String = ""
    }
}
