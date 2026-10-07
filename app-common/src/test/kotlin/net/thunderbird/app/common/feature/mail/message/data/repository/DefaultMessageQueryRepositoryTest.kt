package net.thunderbird.app.common.feature.mail.message.data.repository

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.mailstore.LocalStore
import com.fsck.k9.mailstore.LocalStoreProvider
import com.fsck.k9.mailstore.LockableDatabase
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.folder.FolderIdFactory
import net.thunderbird.feature.mail.folder.LegacyFolderIdFactory
import net.thunderbird.feature.mail.message.LegacyMessageIdFactory
import net.thunderbird.feature.mail.message.MessageServerId
import net.thunderbird.feature.mail.message.domain.GetMessageIdCriteria
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultMessageQueryRepositoryTest {

    private val accountId = AccountIdFactory.create()
    private val folderId = FolderIdFactory.of("00000000-0000-4000-8000-000000000010")
    private val legacyFolderId = LegacyFolderIdFactory.toLegacyId(folderId)
    private val serverId = "server-msg-id-123"
    private val criteria = GetMessageIdCriteria(
        folderId = folderId,
        messageServerId = MessageServerId(serverId),
    )

    private val cursor = mock<Cursor>()
    private val sqliteDatabase = mock<SQLiteDatabase> {
        on {
            query(
                eq("messages"),
                eq(arrayOf("id")),
                eq("folder_id = ? AND uid = ?"),
                eq(arrayOf(legacyFolderId.toString(), serverId)),
                eq(null),
                eq(null),
                eq(null),
            )
        } doReturn cursor
    }

    private val lockableDatabase = mock<LockableDatabase> {
        on { execute(eq(false), any<LockableDatabase.DbCallback<Any?>>()) } doAnswer { invocation ->
            val callback = invocation.getArgument<LockableDatabase.DbCallback<Any?>>(1)
            callback.doDbWork(sqliteDatabase)
        }
    }

    private val localStore = mock<LocalStore> {
        on { database } doReturn lockableDatabase
    }

    private val localStoreProvider = mock<LocalStoreProvider> {
        on { getInstance(accountId) } doReturn localStore
    }

    private val logger = mock<Logger>()

    private val testSubject = DefaultMessageQueryRepository(
        logger = logger,
        localStoreProvider = localStoreProvider,
        ioDispatcher = UnconfinedTestDispatcher(),
    )

    @Test
    fun `findIdByCriteria returns message id when matching message is found`() = runTest {
        val expectedLegacyMessageId = 42L
        whenever(cursor.moveToFirst()).thenReturn(true)
        whenever(cursor.getLong(0)).thenReturn(expectedLegacyMessageId)

        val result = testSubject.findIdByCriteria(accountId, criteria)

        assertThat(result).isEqualTo(Outcome.success(LegacyMessageIdFactory.of(expectedLegacyMessageId)))
        verify(localStoreProvider).getInstance(accountId)
    }

    @Test
    fun `findIdByCriteria returns null when no matching message is found`() = runTest {
        whenever(cursor.moveToFirst()).thenReturn(false)

        val result = testSubject.findIdByCriteria(accountId, criteria)

        assertThat(result).isEqualTo(Outcome.success(null))
        verify(localStoreProvider).getInstance(accountId)
    }
}
