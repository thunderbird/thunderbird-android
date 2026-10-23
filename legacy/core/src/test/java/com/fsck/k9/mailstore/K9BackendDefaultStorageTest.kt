package com.fsck.k9.mailstore

import app.k9mail.legacy.mailstore.FolderSettings
import app.k9mail.legacy.mailstore.MessageStoreManager
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.FakeLegacyAccount
import com.fsck.k9.K9RobolectricTest
import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.mail.Message
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.message.domain.MessageLifecycleRepository
import net.thunderbird.feature.mail.message.domain.MessageQueryRepository
import net.thunderbird.feature.mail.message.mapper.MessageDataMapper
import org.junit.Before
import org.junit.Test
import org.koin.core.component.inject
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class K9BackendDefaultStorageTest : K9RobolectricTest() {
    val messageStoreManager: MessageStoreManager by inject()
    val saveMessageDataCreator: SaveMessageDataCreator by inject()

    val accountId = AccountIdFactory.create()
    private lateinit var backendStorage: BackendStorage

    @Before
    fun setUp() {
        val accountManager: LegacyAccountManager by inject()
        whenever(accountManager.findById(accountId)).thenReturn(FakeLegacyAccount.create(id = accountId))
        backendStorage = createBackendStorage()
    }

    @Test
    fun writeAndReadExtraString() {
        backendStorage.setExtraString("testString", "someValue")
        val value = backendStorage.getExtraString("testString")

        assertThat(value).isEqualTo("someValue")
    }

    @Test
    fun updateExtraString() {
        backendStorage.setExtraString("testString", "oldValue")
        backendStorage.setExtraString("testString", "newValue")

        val value = backendStorage.getExtraString("testString")
        assertThat(value).isEqualTo("newValue")
    }

    @Test
    fun writeAndReadExtraInteger() {
        backendStorage.setExtraNumber("testNumber", 42)
        val value = backendStorage.getExtraNumber("testNumber")

        assertThat(value).isEqualTo(42L)
    }

    @Test
    fun updateExtraInteger() {
        backendStorage.setExtraNumber("testNumber", 42)
        backendStorage.setExtraNumber("testNumber", 23)

        val value = backendStorage.getExtraNumber("testNumber")
        assertThat(value).isEqualTo(23L)
    }

    private fun createBackendStorage(): BackendStorage {
        val messageStore = messageStoreManager.getMessageStore(accountId)
        val folderSettingsProvider = createFolderSettingsProvider()
        return K9BackendStorage(
            messageStore = messageStore,
            folderSettingsProvider = folderSettingsProvider,
            saveMessageDataCreator = saveMessageDataCreator,
            listeners = emptyList(),
            messageLifecycleRepository = mock<MessageLifecycleRepository>(),
            messageQueryRepository = mock<MessageQueryRepository>(),
            messageDataMapper = mock<MessageDataMapper<Message>>(),
        )
    }
}

internal fun createFolderSettingsProvider(): FolderSettingsProvider {
    return mock {
        on { getFolderSettings(any()) } doReturn
            FolderSettings(
                visibleLimit = 25,
                isVisible = true,
                isSyncEnabled = false,
                isNotificationsEnabled = false,
                isPushEnabled = false,
                inTopGroup = false,
                integrate = false,
            )
    }
}
