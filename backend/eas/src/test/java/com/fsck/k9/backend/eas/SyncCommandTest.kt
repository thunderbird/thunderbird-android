package com.fsck.k9.backend.eas

import com.fsck.k9.backend.api.BackendFolder
import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.SyncConfig
import com.fsck.k9.backend.api.SyncListener
import com.fsck.k9.backend.eas.dto.*
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.K9LibRobolectricTestRunner
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.TestMessageConstructionUtils.*
import com.fsck.k9.mail.internet.BinaryTempFileBody
import com.fsck.k9.mail.internet.MimeMessageHelper
import com.nhaarman.mockito_kotlin.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import java.io.IOException

@RunWith(K9LibRobolectricTestRunner::class)
class SyncCommandTest {
    private val client = mock<EasClient>()
    private val provisionManager = mock<EasProvisionManager>()
    private val backendStorage = mock<BackendStorage>()

    @Before
    fun setUp() {
        BinaryTempFileBody.setTempDirectory(RuntimeEnvironment.application.cacheDir)
    }

    @Test
    fun messageFetch_shouldFetchMessage() {
        val backendFolderMock = mock<BackendFolder>()
        whenever(backendFolderMock.getFolderExtraString("EXTRA_SYNC_KEY")).thenReturn("key0")
        whenever(backendStorage.getFolder("col0")).thenReturn(backendFolderMock)

        val expectedMessage = EasMessage().apply {
            MimeMessageHelper.setBody(this, bodypart("text/plain", "text").body);
            setFolderServerId("col0")
            subject = "Subject"
            uid = "id0";
            messageId = "id0";
            setFlag(Flag.SEEN, true)
        }

        whenever(client.sync(Sync(
                SyncCollections(
                        SyncCollection(
                                clazz = "Email",
                                syncKey = "key0",
                                collectionId = "col0",
                                deleteAsMoves = 0,
                                getChanges = 0,
                                options = SyncOptions(
                                        mimeSupport = 2,
                                        bodyPreference = SyncBodyPreference(4)
                                ),
                                commands = SyncCommands(fetch = listOf(SyncItem("id0")))
                        )
                )
        ))).thenReturn(Sync(
                SyncCollections(
                        SyncCollection(
                                clazz = "Email",
                                syncKey = "key1",
                                collectionId = "col0",
                                responses = SyncResponses(fetch = listOf(
                                        SyncItem(serverId = "id0", data = SyncData(
                                                emailFrom = "k9@icloud.com",
                                                emailTo = "chris@gmx.de",
                                                emailRead = 1,
                                                emailSubject = "Subject",
                                                body = Body(
                                                        data = EasMessageElement().apply { from(expectedMessage) }
                                                )
                                        ))
                                )),
                                status = 1
                        )
                ),
                status = 1
        ))

        whenever(provisionManager.ensureProvisioned<(() -> SyncCollection)>(any())).thenAnswer { (it.getArgument(0) as (() -> SyncCollection))() }

        val cut = SyncCommand(client, provisionManager, backendStorage)

        val actualMessage = cut.fetch("col0", "id0")

        assertEquals(expectedMessage, actualMessage)
        verify(backendFolderMock).setFolderExtraString("EXTRA_SYNC_KEY", "key1")
    }

    @Test(expected = MessagingException::class)
    fun messageFetch_statusNotOk_shouldThrow() {
        val backendFolderMock = mock<BackendFolder>()
        whenever(backendFolderMock.getFolderExtraString("EXTRA_SYNC_KEY")).thenReturn("key0")
        whenever(backendStorage.getFolder("col0")).thenReturn(backendFolderMock)

        whenever(client.sync(Sync(
                SyncCollections(
                        SyncCollection(
                                clazz = "Email",
                                syncKey = "key0",
                                collectionId = "col0",
                                deleteAsMoves = 0,
                                getChanges = 0,
                                options = SyncOptions(
                                        mimeSupport = 2,
                                        bodyPreference = SyncBodyPreference(4)
                                ),
                                commands = SyncCommands(fetch = listOf(SyncItem("id0")))
                        )
                )
        ))).thenReturn(Sync(
                SyncCollections(
                        SyncCollection(
                                "Email",
                                "key1",
                                "col0",
                                status = 2
                        )
                ),
                status = 2
        ))

        whenever(provisionManager.ensureProvisioned<(() -> SyncCollection)>(any())).thenAnswer { (it.getArgument(0) as (() -> SyncCollection))() }

        val cut = SyncCommand(client, provisionManager, backendStorage)

        cut.fetch("col0", "id0")
    }

    @Test(expected = MessagingException::class)
    fun messageFetch_responseEmpty_shouldThrow() {
        val backendFolderMock = mock<BackendFolder>()
        whenever(backendFolderMock.getFolderExtraString("EXTRA_SYNC_KEY")).thenReturn("key0")
        whenever(backendStorage.getFolder("col0")).thenReturn(backendFolderMock)

        whenever(client.sync(Sync(
                SyncCollections(
                        SyncCollection(
                                clazz = "Email",
                                syncKey = "key0",
                                collectionId = "col0",
                                deleteAsMoves = 0,
                                getChanges = 0,
                                options = SyncOptions(
                                        mimeSupport = 2,
                                        bodyPreference = SyncBodyPreference(4)
                                ),
                                commands = SyncCommands(fetch = listOf(SyncItem("id0")))
                        )
                )
        ))).thenReturn(Sync(
                SyncCollections(
                        SyncCollection(
                                "Email",
                                "key1",
                                "col0",
                                status = 1,
                                responses = SyncResponses()
                        )
                ),
                status = 1
        ))

        whenever(provisionManager.ensureProvisioned<(() -> SyncCollection)>(any())).thenAnswer { (it.getArgument(0) as (() -> SyncCollection))() }

        val cut = SyncCommand(client, provisionManager, backendStorage)

        cut.fetch("col0", "id0")
    }

    @Test(expected = IOException::class)
    fun messageFetch_clientError_shouldThrow() {
        val backendFolderMock = mock<BackendFolder>()
        whenever(backendFolderMock.getFolderExtraString("EXTRA_SYNC_KEY")).thenReturn("key0")
        whenever(backendStorage.getFolder("col0")).thenReturn(backendFolderMock)

        whenever(client.sync(any())).thenAnswer { throw IOException() }

        whenever(provisionManager.ensureProvisioned<(() -> SyncCollection)>(any())).thenAnswer { (it.getArgument(0) as (() -> SyncCollection))() }

        val cut = SyncCommand(client, provisionManager, backendStorage)

        cut.fetch("col0", "id0")
    }

    @Test
    fun setFlag_FlaggedTrue_shouldSendChangeCommand() {
        val backendFolderMock = mock<BackendFolder>()
        whenever(backendFolderMock.getFolderExtraString("EXTRA_SYNC_KEY")).thenReturn("key0")
        whenever(backendStorage.getFolder("col0")).thenReturn(backendFolderMock)

        whenever(client.sync(Sync(
                SyncCollections(
                        SyncCollection(
                                clazz = "Email",
                                syncKey = "key0",
                                collectionId = "col0",
                                deleteAsMoves = 0,
                                getChanges = 0,
                                commands = SyncCommands(change = listOf(
                                        SyncItem(
                                                serverId = "id0",
                                                data = SyncData(emailFlag = EmailFlag(2))
                                        ),
                                        SyncItem(
                                                serverId = "id1",
                                                data = SyncData(emailFlag = EmailFlag(2))
                                        )
                                ))
                        )
                )
        ))).thenReturn(Sync(
                SyncCollections(
                        SyncCollection(
                                clazz = "Email",
                                syncKey = "key1",
                                collectionId = "col0",
                                status = 1
                        )
                ),
                status = 1
        ))

        whenever(provisionManager.ensureProvisioned<(() -> SyncCollection)>(any())).thenAnswer { (it.getArgument(0) as (() -> SyncCollection))() }

        val cut = SyncCommand(client, provisionManager, backendStorage)

        cut.setFlag("col0", listOf("id0", "id1"), Flag.FLAGGED, true)

        verify(backendFolderMock).setFolderExtraString("EXTRA_SYNC_KEY", "key1")
    }

    @Test
    fun setFlag_FlaggedFalse_shouldSendChangeCommand() {
        val backendFolderMock = mock<BackendFolder>()
        whenever(backendFolderMock.getFolderExtraString("EXTRA_SYNC_KEY")).thenReturn("key0")
        whenever(backendStorage.getFolder("col0")).thenReturn(backendFolderMock)

        whenever(client.sync(Sync(
                SyncCollections(
                        SyncCollection(
                                clazz = "Email",
                                syncKey = "key0",
                                collectionId = "col0",
                                deleteAsMoves = 0,
                                getChanges = 0,
                                commands = SyncCommands(change = listOf(
                                        SyncItem(
                                                serverId = "id0",
                                                data = SyncData(emailFlag = EmailFlag(0))
                                        ),
                                        SyncItem(
                                                serverId = "id1",
                                                data = SyncData(emailFlag = EmailFlag(0))
                                        )
                                ))
                        )
                )
        ))).thenReturn(Sync(
                SyncCollections(
                        SyncCollection(
                                clazz = "Email",
                                syncKey = "key1",
                                collectionId = "col0",
                                status = 1
                        )
                ),
                status = 1
        ))

        whenever(provisionManager.ensureProvisioned<(() -> SyncCollection)>(any())).thenAnswer { (it.getArgument(0) as (() -> SyncCollection))() }

        val cut = SyncCommand(client, provisionManager, backendStorage)

        cut.setFlag("col0", listOf("id0", "id1"), Flag.FLAGGED, false)

        verify(backendFolderMock).setFolderExtraString("EXTRA_SYNC_KEY", "key1")
    }

    @Test
    fun setFlag_SeenTrue_shouldSendChangeCommand() {
        val backendFolderMock = mock<BackendFolder>()
        whenever(backendFolderMock.getFolderExtraString("EXTRA_SYNC_KEY")).thenReturn("key0")
        whenever(backendStorage.getFolder("col0")).thenReturn(backendFolderMock)

        whenever(client.sync(Sync(
                SyncCollections(
                        SyncCollection(
                                clazz = "Email",
                                syncKey = "key0",
                                collectionId = "col0",
                                deleteAsMoves = 0,
                                getChanges = 0,
                                commands = SyncCommands(change = listOf(
                                        SyncItem(
                                                serverId = "id0",
                                                data = SyncData(emailRead = 1)
                                        ),
                                        SyncItem(
                                                serverId = "id1",
                                                data = SyncData(emailRead = 1)
                                        )
                                ))
                        )
                )
        ))).thenReturn(Sync(
                SyncCollections(
                        SyncCollection(
                                clazz = "Email",
                                syncKey = "key1",
                                collectionId = "col0",
                                status = 1
                        )
                ),
                status = 1
        ))

        whenever(provisionManager.ensureProvisioned<(() -> SyncCollection)>(any())).thenAnswer { (it.getArgument(0) as (() -> SyncCollection))() }

        val cut = SyncCommand(client, provisionManager, backendStorage)

        cut.setFlag("col0", listOf("id0", "id1"), Flag.SEEN, true)

        verify(backendFolderMock).setFolderExtraString("EXTRA_SYNC_KEY", "key1")
    }

    @Test
    fun setFlag_SeenFalse_shouldSendChangeCommand() {
        val backendFolderMock = mock<BackendFolder>()
        whenever(backendFolderMock.getFolderExtraString("EXTRA_SYNC_KEY")).thenReturn("key0")
        whenever(backendStorage.getFolder("col0")).thenReturn(backendFolderMock)

        whenever(client.sync(Sync(
                SyncCollections(
                        SyncCollection(
                                clazz = "Email",
                                syncKey = "key0",
                                collectionId = "col0",
                                deleteAsMoves = 0,
                                getChanges = 0,
                                commands = SyncCommands(change = listOf(
                                        SyncItem(
                                                serverId = "id0",
                                                data = SyncData(emailRead = 0)
                                        ),
                                        SyncItem(
                                                serverId = "id1",
                                                data = SyncData(emailRead = 0)
                                        )
                                ))
                        )
                )
        ))).thenReturn(Sync(
                SyncCollections(
                        SyncCollection(
                                clazz = "Email",
                                syncKey = "key1",
                                collectionId = "col0",
                                status = 1
                        )
                ),
                status = 1
        ))

        whenever(provisionManager.ensureProvisioned<(() -> SyncCollection)>(any())).thenAnswer { (it.getArgument(0) as (() -> SyncCollection))() }

        val cut = SyncCommand(client, provisionManager, backendStorage)

        cut.setFlag("col0", listOf("id0", "id1"), Flag.SEEN, false)

        verify(backendFolderMock).setFolderExtraString("EXTRA_SYNC_KEY", "key1")
    }

    @Test(expected = MessagingException::class)
    fun setFlag_statusNotOk_shouldThrow() {
        val backendFolderMock = mock<BackendFolder>()
        whenever(backendFolderMock.getFolderExtraString("EXTRA_SYNC_KEY")).thenReturn("key0")
        whenever(backendStorage.getFolder("col0")).thenReturn(backendFolderMock)

        whenever(client.sync(Sync(
                SyncCollections(
                        SyncCollection(
                                clazz = "Email",
                                syncKey = "key0",
                                collectionId = "col0",
                                deleteAsMoves = 0,
                                getChanges = 0,
                                commands = SyncCommands(change = listOf(
                                        SyncItem(
                                                serverId = "id0",
                                                data = SyncData(emailFlag = EmailFlag(2))
                                        )
                                ))
                        )
                )
        ))).thenReturn(Sync(
                SyncCollections(
                        SyncCollection(
                                "Email",
                                "key1",
                                "col0",
                                status = 2
                        )
                ),
                status = 2
        ))

        whenever(provisionManager.ensureProvisioned<(() -> SyncCollection)>(any())).thenAnswer { (it.getArgument(0) as (() -> SyncCollection))() }

        val cut = SyncCommand(client, provisionManager, backendStorage)

        cut.setFlag("col0", listOf("id0"), Flag.FLAGGED, true)
    }


    @Test(expected = IOException::class)
    fun setFlag_clientError_shouldThrow() {
        val backendFolderMock = mock<BackendFolder>()
        whenever(backendFolderMock.getFolderExtraString("EXTRA_SYNC_KEY")).thenReturn("key0")
        whenever(backendStorage.getFolder("col0")).thenReturn(backendFolderMock)

        whenever(client.sync(any())).thenAnswer { throw IOException() }

        whenever(provisionManager.ensureProvisioned<(() -> SyncCollection)>(any())).thenAnswer { (it.getArgument(0) as (() -> SyncCollection))() }

        val cut = SyncCommand(client, provisionManager, backendStorage)

        cut.setFlag("col0", listOf("id0"), Flag.FLAGGED, true)
    }

    @Test
    fun setFlag_other_shouldReturnEarly() {
        val cut = SyncCommand(client, provisionManager, backendStorage)

        cut.setFlag("col0", listOf("id0"), Flag.ANSWERED, true)

        verifyZeroInteractions(backendStorage, client, provisionManager)
    }

    @Test
    fun sync_shouldSync() {
        val backendFolderMock = mock<BackendFolder>()
        whenever(backendFolderMock.getFolderExtraString("EXTRA_SYNC_KEY")).thenReturn(null)
        whenever(backendStorage.getFolder("col0")).thenReturn(backendFolderMock)

        val syncConfig = SyncConfig(
                SyncConfig.ExpungePolicy.IMMEDIATELY,
                maximumPolledMessageAge = 28,
                earliestPollDate = null,
                syncRemoteDeletions = true,
                maximumAutoDownloadMessageSize = 32000,
                defaultVisibleLimit = 25,
                syncFlags = emptySet())

        val syncListener = mock<SyncListener>()

        val expectedMessage0 = EasMessage().apply {
            MimeMessageHelper.setBody(this, bodypart("text/plain", "text").body);
            setFolderServerId("col0")
            subject = "Subject"
            uid = "id0";
            messageId = "id0";
            setFlag(Flag.SEEN, true)
        }

        val expectedMessage3 = EasMessage().apply {
            MimeMessageHelper.setBody(this, bodypart("text/plain", "text").body);
            setFolderServerId("col0")
            subject = "Subject"
            uid = "id3";
            messageId = "id3";
            setFlag(Flag.FLAGGED, true)
        }

        whenever(client.sync(Sync(
                SyncCollections(
                        SyncCollection(
                                clazz = "Email",
                                syncKey = "0",
                                collectionId = "col0"
                        )
                )
        ))).thenReturn(Sync(
                SyncCollections(
                        SyncCollection(
                                "Email",
                                "key0",
                                "col0",
                                status = 1
                        )
                ),
                status = 1
        ))

        whenever(client.sync(Sync(
                SyncCollections(
                        SyncCollection(
                                clazz = "Email",
                                syncKey = "key0",
                                collectionId = "col0",
                                deleteAsMoves = 1,
                                getChanges = 1,
                                windowSize = 30,
                                options = SyncOptions(
                                        filterType = 5,
                                        bodyPreference = SyncBodyPreference(4, 32000),
                                        mimeSupport = 2
                                )
                        )
                )
        ))).thenReturn(Sync(
                SyncCollections(
                        SyncCollection(
                                "Email",
                                "key1",
                                "col0",
                                status = 1,
                                moreAvailable = true,
                                commands = SyncCommands(
                                        add = listOf(
                                                SyncItem(serverId = "id0", data = SyncData(
                                                        emailFrom = "k9@icloud.com",
                                                        emailTo = "chris@gmx.de",
                                                        emailRead = 1,
                                                        emailSubject = "Subject",
                                                        body = Body(
                                                                data = EasMessageElement().apply { from(expectedMessage0) },
                                                                truncated = 1
                                                        )
                                                ))
                                        ),
                                        change = listOf(
                                                SyncItem(serverId = "id1", data = SyncData(emailRead = 0))
                                        ),
                                        delete = listOf(SyncItem(serverId = "id2"))
                                ))
                ),
                status = 1
        ))

        whenever(client.sync(Sync(
                SyncCollections(
                        SyncCollection(
                                clazz = "Email",
                                syncKey = "key1",
                                collectionId = "col0",
                                deleteAsMoves = 1,
                                getChanges = 1,
                                windowSize = 30,
                                options = SyncOptions(
                                        filterType = 5,
                                        bodyPreference = SyncBodyPreference(4, 32000),
                                        mimeSupport = 2
                                )
                        )
                )
        ))).thenReturn(Sync(
                SyncCollections(
                        SyncCollection(
                                "Email",
                                "key2",
                                "col0",
                                status = 1,
                                moreAvailable = false,
                                commands = SyncCommands(
                                        add = listOf(
                                                SyncItem(serverId = "id3", data = SyncData(
                                                        emailFrom = "k9@icloud.com",
                                                        emailTo = "chris@gmx.de",
                                                        emailSubject = "Subject",
                                                        emailFlag = EmailFlag(2),
                                                        body = Body(
                                                                data = EasMessageElement().apply { from(expectedMessage3) },
                                                                truncated = 0
                                                        )
                                                ))
                                        ),
                                        change = listOf(
                                                SyncItem(serverId = "id4", data = SyncData(emailFlag = EmailFlag(2))),
                                                SyncItem(serverId = "id5", data = SyncData(emailRead = 1, emailFlag = EmailFlag(0)))
                                        )
                                )
                        )
                ),
                status = 1
        ))

        whenever(provisionManager.ensureProvisioned<(() -> Unit)>(any())).thenAnswer { (it.getArgument(0) as (() -> Unit))() }

        val cut = SyncCommand(client, provisionManager, backendStorage)

        cut.sync("col0", syncConfig, syncListener)

        verify(syncListener).syncNewMessage("col0", "id0", false)
        verify(syncListener).syncFlagChanged("col0", "id1")
        verify(syncListener).syncRemovedMessage("col0", "id2")
        verify(syncListener).syncNewMessage("col0", "id3", false)
        verify(syncListener).syncFlagChanged("col0", "id4")
        verify(syncListener).syncFlagChanged("col0", "id5")
        verify(syncListener).syncFinished("col0", -1, 2)

        verify(backendFolderMock).savePartialMessage(expectedMessage0)
        verify(backendFolderMock).setMessageFlag("id1", Flag.SEEN, false)
        verify(backendFolderMock).destroyMessages(listOf("id2"))
        verify(backendFolderMock).saveCompleteMessage(expectedMessage3)
        verify(backendFolderMock).setMessageFlag("id4", Flag.FLAGGED, true)
        verify(backendFolderMock).setMessageFlag("id5", Flag.FLAGGED, false)
        verify(backendFolderMock).setMessageFlag("id5", Flag.SEEN, true)

        verify(backendFolderMock).getFolderExtraString("EXTRA_SYNC_KEY")
        verify(backendFolderMock).setFolderExtraString("EXTRA_SYNC_KEY", "key1")
        verify(backendFolderMock).setFolderExtraString("EXTRA_SYNC_KEY", "key2")
        verifyNoMoreInteractions(syncListener, backendFolderMock)
    }

    @Test
    fun sync_dontSyncDeletions_shouldSync() {
        val backendFolderMock = mock<BackendFolder>()
        whenever(backendFolderMock.getFolderExtraString("EXTRA_SYNC_KEY")).thenReturn("key0")
        whenever(backendStorage.getFolder("col0")).thenReturn(backendFolderMock)

        val syncConfig = SyncConfig(
                SyncConfig.ExpungePolicy.IMMEDIATELY,
                maximumPolledMessageAge = -1,
                earliestPollDate = null,
                syncRemoteDeletions = false,
                maximumAutoDownloadMessageSize = 32000,
                defaultVisibleLimit = 25,
                syncFlags = emptySet())

        val syncListener = mock<SyncListener>()

        whenever(client.sync(Sync(
                SyncCollections(
                        SyncCollection(
                                clazz = "Email",
                                syncKey = "key0",
                                collectionId = "col0",
                                deleteAsMoves = 1,
                                getChanges = 1,
                                windowSize = 30,
                                options = SyncOptions(
                                        filterType = 0,
                                        bodyPreference = SyncBodyPreference(4, 32000),
                                        mimeSupport = 2
                                )
                        )
                )
        ))).thenReturn(Sync(
                SyncCollections(
                        SyncCollection(
                                "Email",
                                "key1",
                                "col0",
                                status = 1,
                                commands = SyncCommands(
                                        delete = listOf(SyncItem(serverId = "id2"))
                                ))
                ),
                status = 1
        ))

        whenever(provisionManager.ensureProvisioned<(() -> Unit)>(any())).thenAnswer { (it.getArgument(0) as (() -> Unit))() }

        val cut = SyncCommand(client, provisionManager, backendStorage)

        cut.sync("col0", syncConfig, syncListener)

        verify(backendFolderMock).getFolderExtraString("EXTRA_SYNC_KEY")
        verify(backendFolderMock).setFolderExtraString("EXTRA_SYNC_KEY", "key1")
        verify(syncListener).syncFinished("col0", -1, 0)
        verifyNoMoreInteractions(syncListener, backendFolderMock)
    }

    @Test(expected = MessagingException::class)
    fun sync_statusNotOk_shouldThrow() {
        val backendFolderMock = mock<BackendFolder>()
        whenever(backendFolderMock.getFolderExtraString("EXTRA_SYNC_KEY")).thenReturn("key0")
        whenever(backendStorage.getFolder("col0")).thenReturn(backendFolderMock)

        val syncConfig = SyncConfig(
                SyncConfig.ExpungePolicy.IMMEDIATELY,
                maximumPolledMessageAge = -1,
                earliestPollDate = null,
                syncRemoteDeletions = false,
                maximumAutoDownloadMessageSize = 32000,
                defaultVisibleLimit = 25,
                syncFlags = emptySet())

        val syncListener = mock<SyncListener>()

        whenever(client.sync(Sync(
                SyncCollections(
                        SyncCollection(
                                clazz = "Email",
                                syncKey = "key0",
                                collectionId = "col0",
                                deleteAsMoves = 1,
                                getChanges = 1,
                                windowSize = 30,
                                options = SyncOptions(
                                        filterType = 0,
                                        bodyPreference = SyncBodyPreference(4, 32000),
                                        mimeSupport = 2
                                )
                        )
                )
        ))).thenReturn(Sync(
                SyncCollections(
                        SyncCollection(
                                "Email",
                                "key1",
                                "col0",
                                status = 2
                        )
                ),
                status = 2
        ))

        whenever(provisionManager.ensureProvisioned<(() -> Unit)>(any())).thenAnswer { (it.getArgument(0) as (() -> Unit))() }

        val cut = SyncCommand(client, provisionManager, backendStorage)

        cut.sync("col0", syncConfig, syncListener)

        verify(backendFolderMock).getFolderExtraString("EXTRA_SYNC_KEY")
        verifyNoMoreInteractions(syncListener, backendFolderMock)
    }
}
