package com.fsck.k9.backend.eas

import com.fsck.k9.backend.api.BackendFolder
import com.fsck.k9.backend.api.BackendStorage
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
import java.io.ByteArrayOutputStream
import java.io.IOException

@RunWith(K9LibRobolectricTestRunner::class)
class MessageFetchCommandTest {
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

        val expectedMessage = EasMessage(EasFolder("col0")).apply {
            MimeMessageHelper.setBody(this, bodypart("text/plain", "text").body);
            subject = "Subject"
            uid = "id0";
            messageId = "id0";
            setFlag(Flag.SEEN, true)
        }

        whenever(client.sync(Sync(
                SyncCollections(
                        SyncCollection(
                                "Email",
                                "key0",
                                "col0",
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
                                responses = SyncResponses(fetch = listOf(
                                        SyncItem("id0", SyncData(
                                                emailFrom = "k9@icloud.com",
                                                emailTo = "chris@gmx.de",
                                                emailRead = 1,
                                                emailSubject = "Subject",
                                                body = Body(
                                                        data = ByteArrayOutputStream().apply {
                                                            expectedMessage.writeTo(this)
                                                        }.toString()
                                                )
                                        ))
                                )),
                                status = 1
                        )
                ),
                status = 1
        ))

        whenever(provisionManager.ensureProvisioned<(() -> Message)>(any())).thenAnswer { (it.getArgument(0) as (() -> Message))() }

        val cut = MessageFetchCommand(client, provisionManager, backendStorage)

        val actualMessage = cut.fetch("col0", "id0")

        assertEquals(expectedMessage, actualMessage)
        verify(backendFolderMock).setFolderExtraString("EXTRA_SYNC_KEY", "key1")
    }

    @Test(expected = MessagingException::class)
    fun messageFetch_serverError_shouldThrow() {
        val backendFolderMock = mock<BackendFolder>()
        whenever(backendFolderMock.getFolderExtraString("EXTRA_SYNC_KEY")).thenReturn("key0")
        whenever(backendStorage.getFolder("col0")).thenReturn(backendFolderMock)

        whenever(client.sync(Sync(
                SyncCollections(
                        SyncCollection(
                                "Email",
                                "key0",
                                "col0",
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
                status = 1
        ))

        whenever(provisionManager.ensureProvisioned<(() -> Message)>(any())).thenAnswer { (it.getArgument(0) as (() -> Message))() }

        val cut = MessageFetchCommand(client, provisionManager, backendStorage)

        cut.fetch("col0", "id0")
    }

    @Test(expected = IOException::class)
    fun messageFetch_clientError_shouldThrow() {
        val backendFolderMock = mock<BackendFolder>()
        whenever(backendFolderMock.getFolderExtraString("EXTRA_SYNC_KEY")).thenReturn("key0")
        whenever(backendStorage.getFolder("col0")).thenReturn(backendFolderMock)

        whenever(client.sync(any())).thenAnswer { throw IOException() }

        whenever(provisionManager.ensureProvisioned<(() -> Message)>(any())).thenAnswer { (it.getArgument(0) as (() -> Message))() }

        val cut = MessageFetchCommand(client, provisionManager, backendStorage)

        cut.fetch("col0", "id0")
    }
}
