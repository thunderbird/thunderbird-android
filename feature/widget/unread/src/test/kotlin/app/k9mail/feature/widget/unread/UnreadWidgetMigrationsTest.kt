package app.k9mail.feature.widget.unread

import android.content.Context
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.folder.api.Folder
import net.thunderbird.feature.mail.folder.api.FolderServerId
import net.thunderbird.feature.mail.folder.api.data.FolderError
import net.thunderbird.feature.mail.folder.api.data.repository.FolderQueryRepository
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class UnreadWidgetMigrationsTest {
    private val accountId = AccountIdFactory.create()
    private val account = mock<LegacyAccount> {
        on { id } doReturn accountId
    }
    private val accountManager = mock<LegacyAccountManager> {
        on { findById(accountId) } doReturn account
    }
    private val folderQueryRepository = object : FolderQueryRepository {
        override suspend fun findById(accountId: AccountId, folderId: Long): Outcome<Folder?, FolderError> =
            Outcome.success(null)

        override suspend fun findFolderServerIdById(
            accountId: AccountId,
            folderId: Long,
        ): Outcome<FolderServerId?, FolderError> = Outcome.success(null)

        override suspend fun findIdByServerId(
            accountId: AccountId,
            folderServerId: FolderServerId,
        ): Outcome<Long?, FolderError> = Outcome.success(42L)

        override suspend fun isPresent(accountId: AccountId, folderId: Long): Boolean = false
    }
    private val testSubject = UnreadWidgetMigrations(accountManager, folderQueryRepository)

    @Test
    fun `upgradePreferences should skip malformed account IDs and migrate remaining widgets`() = runTest {
        // Arrange
        val preferences = RuntimeEnvironment.getApplication().getSharedPreferences(
            "unread_widget_migrations_test",
            Context.MODE_PRIVATE,
        )
        preferences.edit().clear()
            .putString("unread_widget.1", "invalid")
            .putString("unread_widget.1.folder_name", "INBOX")
            .putString("unread_widget.2", accountId.toString())
            .putString("unread_widget.2.folder_name", "INBOX")
            .commit()

        try {
            // Act
            testSubject.upgradePreferences(preferences, version = 1)

            // Assert
            assertThat(preferences.getString("unread_widget.2.folder_id", null)).isEqualTo("42")
            assertThat(preferences.getString("unread_widget.1.folder_name", null)).isEqualTo("INBOX")
        } finally {
            preferences.edit().clear().commit()
        }
    }
}
