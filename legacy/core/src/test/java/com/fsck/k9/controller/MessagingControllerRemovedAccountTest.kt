package com.fsck.k9.controller

import assertk.assertThat
import assertk.assertions.isFalse
import com.fsck.k9.K9RobolectricTest
import com.fsck.k9.backend.BackendManager
import net.thunderbird.components.core.logging.testing.TestLogger
import net.thunderbird.core.android.account.fake.FakeLegacyAccountManager
import net.thunderbird.core.featureflag.FeatureFlagResult
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.notification.testing.fake.FakeNotificationManager
import net.thunderbird.legacy.core.StubLocalDeleteOperationDecider
import net.thunderbird.legacy.core.mailstore.folder.FakeLocalMessageUidPrefixProvider
import net.thunderbird.legacy.core.mailstore.folder.FakeOutboxFolderManager
import org.junit.After
import org.junit.Test
import org.mockito.kotlin.mock
import org.robolectric.RuntimeEnvironment

class MessagingControllerRemovedAccountTest : K9RobolectricTest() {
    private val removedAccountId = AccountIdFactory.create()
    private val accountManager = FakeLegacyAccountManager()

    private val testSubject = MessagingController(
        RuntimeEnvironment.getApplication(),
        mock(),
        mock(),
        mock(),
        BackendManager(backendFactories = emptyMap(), accountManager = accountManager),
        mock(),
        mock(),
        mock(),
        StubLocalDeleteOperationDecider(),
        FakeLocalMessageUidPrefixProvider(),
        emptyList(),
        { FeatureFlagResult.Disabled },
        TestLogger(),
        FakeNotificationManager(
            emitOnSend = { error("Unexpected notification") },
            emitOnDismissNotification = { error("Unexpected notification dismissal") },
            emitOnDismissId = { error("Unexpected notification dismissal") },
        ),
        FakeOutboxFolderManager(OUTBOX_FOLDER_ID),
        accountManager,
    )

    @After
    fun tearDown() {
        testSubject.stop()
    }

    @Test
    fun `capability checks should return false for a removed account`() {
        assertThat(testSubject.isMoveCapable(removedAccountId)).isFalse()
        assertThat(testSubject.isCopyCapable(removedAccountId)).isFalse()
        assertThat(testSubject.isPushCapable(removedAccountId)).isFalse()
        assertThat(testSubject.supportsExpunge(removedAccountId)).isFalse()
    }

    private companion object {
        const val OUTBOX_FOLDER_ID = 1L
    }
}
