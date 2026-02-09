package net.thunderbird.feature.account.avatar.data.datasource

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import com.eygraber.uri.Uri
import com.eygraber.uri.toKmpUri
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.file.DirectoryProvider
import net.thunderbird.core.file.FileManager
import net.thunderbird.core.file.FileOperationError
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.avatar.data.AvatarDataContract
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class LocalAvatarImageDataSourceTest {

    @JvmField
    @Rule
    val folder = TemporaryFolder()

    private lateinit var directoryProvider: DirectoryProvider
    private lateinit var fileManager: SpyFileManager
    private lateinit var testSubject: LocalAvatarImageDataSource

    @BeforeTest
    fun setUp() {
        val appDir = folder.newFolder("app")
        directoryProvider = FakeDirectoryProvider(appDir.absolutePath.toKmpUri())
        fileManager = SpyFileManager()
        testSubject = LocalAvatarImageDataSource(fileManager, directoryProvider)
    }

    @Test
    fun `update with JPG should copy image to JPG path and clean up old files`() = runTest {
        // Arrange
        val accountId = AccountIdFactory.create()
        val source = "file:///external/picked/image.jpg".toKmpUri()
        val expectedDir = getAvatarDir()
        val expectedDest = expectedDir.buildUpon().appendPath("$accountId.jpg").build()

        // Act
        val returned = testSubject.update(accountId, source)

        // Assert
        assertThat(returned).isEqualTo(expectedDest)
        assertThat(fileManager.lastCopySource).isEqualTo(source)
        assertThat(fileManager.lastCopyDestination).isEqualTo(expectedDest)

        // Verify
        val expectedPngDel = expectedDir.buildUpon().appendPath("$accountId.png").build()
        val expectedJpgDel = expectedDir.buildUpon().appendPath("$accountId.jpg").build()
        assertThat(fileManager.deletedPaths).contains(expectedPngDel)
        assertThat(fileManager.deletedPaths).contains(expectedJpgDel)
    }

    @Test
    fun `update with PNG should copy image to PNG path`() = runTest {
        // Arrange
        val accountId = AccountIdFactory.create()
        val source = "file:///external/picked/photo.png".toKmpUri()
        val expectedDir = getAvatarDir()
        val expectedDest = expectedDir.buildUpon().appendPath("$accountId.png").build()

        // Act
        val returned = testSubject.update(accountId, source)

        // Assert
        assertThat(returned).isEqualTo(expectedDest)
        assertThat(fileManager.lastCopySource).isEqualTo(source)
        assertThat(fileManager.lastCopyDestination).isEqualTo(expectedDest)
    }

    @Test
    fun `delete should remove both JPG and PNG paths`() = runTest {
        // Arrange
        val accountId = AccountIdFactory.create()
        val expectedDir = getAvatarDir()
        val jpgPath = expectedDir.buildUpon().appendPath("$accountId.jpg").build()
        val pngPath = expectedDir.buildUpon().appendPath("$accountId.png").build()

        // Act
        testSubject.delete(accountId)

        // Assert
        assertThat(fileManager.deletedPaths).contains(jpgPath)
        assertThat(fileManager.deletedPaths).contains(pngPath)
    }

    private suspend fun getAvatarDir(): Uri {
        return directoryProvider.getFilesDir().buildUpon()
            .appendPath(AvatarDataContract.DataSource.LocalAvatarImage.DIRECTORY_NAME)
            .build()
    }

    // Fixed SpyFileManager to match interface return types
    class SpyFileManager : FileManager {
        var lastCopySource: Uri? = null
        var lastCopyDestination: Uri? = null
        val deletedPaths = mutableListOf<Uri>()

        override suspend fun copy(sourceUri: Uri, destinationUri: Uri): Outcome<Unit, FileOperationError> {
            lastCopySource = sourceUri
            lastCopyDestination = destinationUri
            return Outcome.Success(Unit)
        }

        override suspend fun delete(uri: Uri): Outcome<Unit, FileOperationError> {
            deletedPaths.add(uri)
            return Outcome.Success(Unit)
        }

        override suspend fun createDirectories(uri: Uri): Outcome<Unit, FileOperationError> {
            return Outcome.Success(Unit)
        }
    }
}
