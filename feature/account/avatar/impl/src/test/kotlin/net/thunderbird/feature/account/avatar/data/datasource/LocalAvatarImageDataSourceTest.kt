package net.thunderbird.feature.account.avatar.data.datasource

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.eygraber.uri.toKmpUri
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.file.DirectoryProvider
import net.thunderbird.core.testing.TestClock
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.avatar.data.AvatarDataContract
import org.junit.Rule
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalTime::class)
class LocalAvatarImageDataSourceTest {

    @JvmField
    @Rule
    val folder = TemporaryFolder()

    private lateinit var directoryProvider: DirectoryProvider
    private lateinit var fileManager: CapturingFileManager
    private lateinit var clock: TestClock
    private lateinit var testSubject: LocalAvatarImageDataSource

    @BeforeTest
    fun setUp() {
        val appDir = folder.newFolder("app")
        directoryProvider = FakeDirectoryProvider(appDir.absolutePath.toKmpUri())
        fileManager = CapturingFileManager()
        clock = TestClock(Instant.fromEpochMilliseconds(1_000))
        testSubject = LocalAvatarImageDataSource(fileManager, directoryProvider, clock)
    }

    @Test
    fun `update should copy image to expected path and return versioned destination uri`() = runTest {
        // Arrange
        val accountId = AccountIdFactory.create()
        val source = "file:///external/picked/image.jpg".toKmpUri()
        val expectedDir = directoryProvider.getFilesDir().buildUpon()
            .appendPath(AvatarDataContract.DataSource.LocalAvatarImage.DIRECTORY_NAME)
            .build()
        val expectedDest = expectedDir.buildUpon().appendPath("$accountId.jpg").build()
        val expectedVersioned = expectedDest.buildUpon().appendQueryParameter("v", "1000").build()

        // Act
        val returned = testSubject.update(accountId, source)

        // Assert
        assertThat(returned).isEqualTo(expectedVersioned)
        assertThat(fileManager.lastCreatedDir).isEqualTo(expectedDir)
        assertThat(fileManager.lastCopySource).isEqualTo(source)
        assertThat(fileManager.lastCopyDestination).isEqualTo(expectedDest)
        assertThat(fileManager.lastDeleted).isNull()
    }

    @Test
    fun `successive updates should return different URIs`() = runTest {
        // Arrange
        val accountId = AccountIdFactory.create()
        val source = "file:///external/picked/image.jpg".toKmpUri()

        // Act
        val uri1 = testSubject.update(accountId, source)
        clock.advanceTimeBy(1.milliseconds)
        val uri2 = testSubject.update(accountId, source)

        // Assert
        assertThat(uri1).isNotEqualTo(uri2)
        // Base paths should be the same
        assertThat(uri1.buildUpon().clearQuery().build()).isEqualTo(uri2.buildUpon().clearQuery().build())
    }

    @Test
    fun `delete should remove expected avatar path`() = runTest {
        // Arrange
        val accountId = AccountIdFactory.create()
        val expectedDir = directoryProvider.getFilesDir().buildUpon()
            .appendPath(AvatarDataContract.DataSource.LocalAvatarImage.DIRECTORY_NAME)
            .build()
        val expectedDest = expectedDir.buildUpon().appendPath("$accountId.jpg").build()

        // Act
        testSubject.delete(accountId)

        // Assert
        assertThat(fileManager.lastDeleted).isEqualTo(expectedDest)
        // No copy on delete
        assertThat(fileManager.lastCopySource).isNull()
        assertThat(fileManager.lastCopyDestination).isNull()
        // Directory creation occurs when computing path even in delete(), due to getAvatarDirUri()
        assertThat(fileManager.lastCreatedDir).isNotNull()
        assertThat(fileManager.lastCreatedDir).isEqualTo(expectedDir)
    }
}
