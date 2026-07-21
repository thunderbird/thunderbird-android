package net.thunderbird.app.common.account

import android.content.Context
import app.k9mail.feature.account.common.domain.entity.Account
import app.k9mail.feature.account.common.domain.entity.AccountOptions
import com.fsck.k9.Preferences
import com.fsck.k9.account.DeletePolicyProvider
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mailstore.SpecialLocalFoldersCreator
import com.fsck.k9.preferences.UnifiedInboxConfigurator
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.android.account.DeletePolicy
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.feature.account.avatar.AvatarMonogramCreator
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AccountCreatorTest {

    @Test
    fun `creating an automatically synchronized account starts an initial mail check`() = runTest {
        // Arrange
        Log.logger = TestLogger()
        val legacyAccount = LegacyAccountDto(
            uuid = ACCOUNT_UUID,
            isSensitiveDebugLoggingEnabled = { false },
        ).apply {
            identities += Identity()
        }
        val preferences = mock<Preferences> {
            on { newAccount(ACCOUNT_UUID) } doReturn legacyAccount
        }
        val accountColorPicker = mock<AccountColorPicker>()
        whenever(accountColorPicker.pickColor()).thenReturn(ACCOUNT_COLOR)
        val messagingController = mock<MessagingController>()
        val testSubject = AccountCreator(
            accountColorPicker = accountColorPicker,
            localFoldersCreator = mock<SpecialLocalFoldersCreator>(),
            preferences = preferences,
            context = mock<Context>(),
            messagingController = messagingController,
            deletePolicyProvider = mock<DeletePolicyProvider> {
                on { getDeletePolicy(Protocols.IMAP) } doReturn DeletePolicy.NEVER
            },
            avatarMonogramCreator = AvatarMonogramCreator { _, _ -> "UE" },
            unifiedInboxConfigurator = mock<UnifiedInboxConfigurator>(),
            coroutineDispatcher = StandardTestDispatcher(testScheduler),
            servicesEnabler = {},
        )

        // Act
        testSubject.createAccount(createAccount())

        // Assert
        verify(messagingController).refreshFolderListBlocking(legacyAccount)
        verify(messagingController).checkMail(legacyAccount, true, true, false, null)
    }

    private fun createAccount() = Account(
        uuid = ACCOUNT_UUID,
        emailAddress = "user@example.com",
        incomingServerSettings = INCOMING_SERVER_SETTINGS,
        outgoingServerSettings = OUTGOING_SERVER_SETTINGS,
        authorizationState = null,
        specialFolderSettings = null,
        options = AccountOptions(
            accountName = "Personal",
            displayName = "User Example",
            emailSignature = null,
            checkFrequencyInMinutes = AUTOMATIC_CHECK_INTERVAL_MINUTES,
            messageDisplayCount = 25,
            showNotification = true,
        ),
    )

    companion object {
        private const val ACCOUNT_UUID = "4c5344e8-8da2-46cc-8c26-97e2115cc1d9"
        private const val ACCOUNT_COLOR = 0x123456
        private const val AUTOMATIC_CHECK_INTERVAL_MINUTES = 15

        private val INCOMING_SERVER_SETTINGS = ServerSettings(
            type = Protocols.IMAP,
            host = "imap.example.com",
            port = 993,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user@example.com",
            password = "password",
            clientCertificateAlias = null,
        )

        private val OUTGOING_SERVER_SETTINGS = ServerSettings(
            type = Protocols.SMTP,
            host = "smtp.example.com",
            port = 465,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user@example.com",
            password = "password",
            clientCertificateAlias = null,
        )
    }
}
