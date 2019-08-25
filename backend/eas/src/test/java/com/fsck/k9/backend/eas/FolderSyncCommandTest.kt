package com.fsck.k9.backend.eas

import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.backend.eas.dto.FolderChange
import com.fsck.k9.backend.eas.dto.FolderChanges
import com.fsck.k9.backend.eas.dto.FolderSync
import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.Folder
import com.fsck.k9.mail.MessagingException
import com.nhaarman.mockito_kotlin.*
import org.junit.Test

class FolderSyncCommandTest {
    private val client = mock<EasClient>()
    private val provisionManager = mock<EasProvisionManager>()
    private val backendStorage = mock<BackendStorage>()

    @Test
    fun folderSync_shouldSyncFolders() {
        whenever(backendStorage.getExtraString("EXTRA_FOLDER_SYNC_KEY")).thenReturn("key0")
        whenever(client.folderSync(FolderSync("key0"))).thenReturn(
                FolderSync("key1", 1,
                        FolderChanges(
                                folderAdd = listOf(
                                        FolderChange("FolderAdd1", 2, null, "id1"),
                                        FolderChange("FolderAdd2", 3, null, "id2"),
                                        FolderChange("FolderAdd3", 4, null, "id3"),
                                        FolderChange("NotMailFolder1", 8, null, "id4")),
                                folderDelete = listOf("id5", "id6"),
                                folderUpdate = listOf(
                                        FolderChange("FolderChange1", 5, null, "id7"),
                                        FolderChange("FolderChange2", 6, null, "id8"),
                                        FolderChange("NotMailFolder2", 7, null, "id9"))
                        )
                )
        )

        whenever(provisionManager.ensureProvisioned<(() -> Unit)>(any())).thenAnswer { (it.getArgument(0) as (() -> Unit))() }

        val cut = FolderSyncCommand(client, provisionManager, backendStorage)

        cut.sync()

        verify(backendStorage).getExtraString(any())
        verify(backendStorage).setExtraString("EXTRA_FOLDER_SYNC_KEY", "key1")
        verify(backendStorage).createFolders(listOf(
                FolderInfo("id1", "FolderAdd1", Folder.FolderType.INBOX),
                FolderInfo("id2", "FolderAdd2", Folder.FolderType.DRAFTS),
                FolderInfo("id3", "FolderAdd3", Folder.FolderType.TRASH)
        ))
        verify(backendStorage).deleteFolders(listOf("id5", "id6"))
        verify(backendStorage).changeFolder("id7", "FolderChange1", Folder.FolderType.SENT)
        verify(backendStorage).changeFolder("id8", "FolderChange2", Folder.FolderType.OUTBOX)
        verifyNoMoreInteractions(backendStorage)
    }

    @Test
    fun folderSync_nothingToSync_shouldSyncFolders() {
        whenever(backendStorage.getExtraString("EXTRA_FOLDER_SYNC_KEY")).thenReturn("key0")
        whenever(client.folderSync(FolderSync("key0"))).thenReturn(
                FolderSync("key1", 1, FolderChanges())
        )

        whenever(provisionManager.ensureProvisioned<(() -> Unit)>(any())).thenAnswer { (it.getArgument(0) as (() -> Unit))() }

        val cut = FolderSyncCommand(client, provisionManager, backendStorage)

        cut.sync()

        verify(backendStorage).getExtraString(any())
        verify(backendStorage).setExtraString("EXTRA_FOLDER_SYNC_KEY", "key1")
        verifyNoMoreInteractions(backendStorage)
    }

    @Test(expected = MessagingException::class)
    fun folderSync_serverError_shouldThrow() {
        whenever(backendStorage.getExtraString("EXTRA_FOLDER_SYNC_KEY")).thenReturn("key0")
        whenever(client.folderSync(FolderSync("key0"))).thenReturn(
                FolderSync("key1", 2)
        )

        whenever(provisionManager.ensureProvisioned<(() -> Unit)>(any())).thenAnswer { (it.getArgument(0) as (() -> Unit))() }

        val cut = FolderSyncCommand(client, provisionManager, backendStorage)

        cut.sync()
    }

    @Test(expected = AuthenticationFailedException::class)
    fun folderSync_authError_shouldThrow() {
        whenever(backendStorage.getExtraString("EXTRA_FOLDER_SYNC_KEY")).thenReturn("key0")
        whenever(client.folderSync(FolderSync("key0"))).thenAnswer { throw AuthenticationFailedException("auth fail") }

        whenever(provisionManager.ensureProvisioned<(() -> Unit)>(any())).thenAnswer { (it.getArgument(0) as (() -> Unit))() }

        val cut = FolderSyncCommand(client, provisionManager, backendStorage)

        cut.sync()
    }
}
