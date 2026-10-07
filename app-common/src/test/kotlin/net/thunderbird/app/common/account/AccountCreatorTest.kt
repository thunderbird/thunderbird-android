package net.thunderbird.app.common.account

import app.k9mail.feature.account.common.domain.entity.Account
import app.k9mail.feature.account.common.domain.entity.AccountOptions
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThan
import com.fsck.k9.CoreResourceProvider
import com.fsck.k9.account.DeletePolicyProvider
import com.fsck.k9.backend.BackendManager
import com.fsck.k9.backend.api.Backend
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mailstore.SpecialLocalFoldersCreator
import com.fsck.k9.preferences.UnifiedInboxConfigurator
import kotlin.test.Test
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.thunderbird.app.common.account.data.FakeAccountProfileRepository
import net.thunderbird.app.common.account.data.FakeLegacyAccountManager
import net.thunderbird.core.android.account.AccountDefaultsProvider.Companion.UNASSIGNED_ACCOUNT_NUMBER
import net.thunderbird.core.android.account.DeletePolicy
import net.thunderbird.core.featureflag.FeatureFlagResult
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.avatar.AvatarMonogramCreator
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AccountCreatorTest {
    private val accountManager = FakeLegacyAccountManager()
    private val accountDefaultsProvider = DefaultAccountDefaultsProvider(
        resourceProvider = mock<CoreResourceProvider> {
            on { defaultIdentityDescription() } doReturn "Default identity"
        },
        featureFlagProvider = { FeatureFlagResult.Disabled },
    )
    private val backend = mock<Backend>()
    private val backendManager = mock<BackendManager>()
    private val testSubject = AccountCreator(
        accountColorPicker = AccountColorPicker(
            repository = FakeAccountProfileRepository(),
            accountColors = persistentListOf(0x123456),
        ),
        localFoldersCreator = mock<SpecialLocalFoldersCreator>(),
        accountManager = accountManager,
        context = mock(),
        messagingController = mock<MessagingController>(),
        backendManager = backendManager,
        deletePolicyProvider = mock<DeletePolicyProvider>(),
        avatarMonogramCreator = { _, _ -> "AB" },
        unifiedInboxConfigurator = mock<UnifiedInboxConfigurator>(),
        accountDefaultsProvider = accountDefaultsProvider,
        featureFlagProvider = mock(),
        getFolderIdsForTypeUseCase = mock(),
        setPushForFolderUseCase = mock(),
        coroutineDispatcher = UnconfinedTestDispatcher(),
    )

    @Test
    fun `createLegacyAccount should map onboarding details and retain defaults`() = runTest {
        // Arrange
        val accountId = AccountIdFactory.create()
        val incomingServerSettings = createServerSettings("imap")
        val outgoingServerSettings = createServerSettings("smtp")
        val account = Account(
            id = accountId,
            emailAddress = "user@example.com",
            incomingServerSettings = incomingServerSettings,
            outgoingServerSettings = outgoingServerSettings,
            authorizationState = "authorization-state",
            specialFolderSettings = null,
            options = AccountOptions(
                accountName = "Personal",
                displayName = "User",
                emailSignature = "Signature",
                checkFrequencyInMinutes = 30,
                messageDisplayCount = 50,
                showNotification = false,
            ),
        )

        // Act
        val result = testSubject.createLegacyAccount(account)

        // Assert
        assertThat(result.id).isEqualTo(accountId)
        assertThat(result.name).isEqualTo("Personal")
        assertThat(result.email).isEqualTo("user@example.com")
        assertThat(result.profile.id).isEqualTo(accountId)
        assertThat(result.profile.name).isEqualTo("User")
        assertThat(result.profile.color).isEqualTo(0x123456)
        assertThat(result.profile.avatar.id).isEqualTo(accountId)
        assertThat(result.profile.avatar.avatarType).isEqualTo(AvatarTypeDto.MONOGRAM)
        assertThat(result.profile.avatar.avatarMonogram).isEqualTo("AB")
        assertThat(result.identities.single().description).isEqualTo("Default identity")
        assertThat(result.identities.single().name).isEqualTo("User")
        assertThat(result.identities.single().email).isEqualTo("user@example.com")
        assertThat(result.identities.single().signature).isEqualTo("Signature")
        assertThat(result.identities.single().signatureUse).isEqualTo(true)
        assertThat(result.incomingServerSettings.type).isEqualTo("imap")
        assertThat(result.outgoingServerSettings).isEqualTo(outgoingServerSettings)
        assertThat(result.oAuthState).isEqualTo("authorization-state")
        assertThat(result.automaticCheckIntervalMinutes).isEqualTo(30)
        assertThat(result.displayCount).isEqualTo(50)
        assertThat(result.isNotifyNewMail).isFalse()
        assertThat(result.accountNumber).isEqualTo(UNASSIGNED_ACCOUNT_NUMBER)
        assertThat(result.deletePolicy).isEqualTo(DeletePolicy.NEVER)
        assertThat(result.isFinishedSetup).isFalse()
    }

    @Test
    fun `initial folder refresh retains folder IDs assigned by the backend`() = runTest {
        // Arrange
        val accountId = AccountIdFactory.create()
        val initialAccount = testSubject.createLegacyAccount(
            Account(
                id = accountId,
                emailAddress = "user@example.com",
                incomingServerSettings = createServerSettings("imap"),
                outgoingServerSettings = createServerSettings("smtp"),
                authorizationState = null,
                specialFolderSettings = null,
                options = AccountOptions(
                    accountName = "Personal",
                    displayName = "User",
                    emailSignature = null,
                    checkFrequencyInMinutes = 30,
                    messageDisplayCount = 50,
                    showNotification = true,
                ),
            ),
        )
        accountManager.updateSync(initialAccount)
        whenever(backendManager.getBackend(accountId)).thenReturn(backend)
        whenever(backend.refreshFolderList()).doAnswer {
            val beforeRefresh = requireNotNull(accountManager.findById(accountId))
            accountManager.updateSync(beforeRefresh.copy(inboxFolderId = 42L, sentFolderId = 43L))
            "/"
        }

        // Act
        testSubject.refreshInitialFolderList(accountId)

        // Assert
        val savedAccount = requireNotNull(accountManager.findById(accountId))
        assertThat(savedAccount.inboxFolderId).isEqualTo(42L)
        assertThat(savedAccount.sentFolderId).isEqualTo(43L)
        assertThat(savedAccount.lastFolderListRefreshTime).isGreaterThan(0L)
    }

    private fun createServerSettings(type: String) = ServerSettings(
        type = type,
        host = "example.com",
        port = 993,
        connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
        authenticationType = AuthType.PLAIN,
        username = "user",
        password = "password",
        clientCertificateAlias = null,
    )
}
