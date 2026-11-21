package net.thunderbird.feature.account.avatar.data.datasource

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.eygraber.uri.Uri
import com.eygraber.uri.toKmpUri
import java.io.IOException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.core.file.DirectoryProvider
import net.thunderbird.core.file.FileManager
import net.thunderbird.core.file.FileOperationError
import net.thunderbird.core.file.MimeType
import net.thunderbird.core.file.MimeTypeResolver
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
    private lateinit var fileManager: SpyFileManager
    private lateinit var mimeTypeResolver: FakeMimeTypeResolver
    private lateinit var clock: TestClock
    private lateinit var testSubject: LocalAvatarImageDataSource

    @BeforeTest
    fun setUp() {
        val appDir = folder.newFolder("app")
        directoryProvider = FakeDirectoryProvider(appDir.absolutePath.toKmpUri())
        fileManager = SpyFileManager()
        mimeTypeResolver = FakeMimeTypeResolver()
        clock = TestClock(Instant.fromEpochMilliseconds(1_000))

        testSubject = LocalAvatarImageDataSource(
            fileManager,
            directoryProvider,
            mimeTypeResolver,
            clock,
        )
    }

    @Test
    fun `update with JPG should copy image to JPG path and clean up old files`() = runTest {
        val accountId = AccountIdFactory.create()
        val source = "file:///external/picked/image.jpg".toKmpUri()
        val expectedDir = getAvatarDir()
        val expectedDest = expectedDir.buildUpon().appendPath("$accountId.jpg").build()
        val expectedVersioned = expectedDest.buildUpon()
            .appendQueryParameter("v", "1000")
            .build()

        mimeTypeResolver.mimeTypeToReturn = MimeType.JPEG

        val returned = testSubject.update(accountId, source)

        assertThat(returned).isEqualTo(expectedVersioned)
        assertThat(fileManager.lastCopySource).isEqualTo(source)
        assertThat(fileManager.lastCopyDestination).isEqualTo(expectedDest)

        val expectedPngDel = expectedDir.buildUpon().appendPath("$accountId.png").build()

        assertThat(fileManager.deletedPaths).containsExactly(expectedPngDel)
    }

    @Test
    fun `update should throw and keep existing files when copy fails`() = runTest {
        val accountId = AccountIdFactory.create()
        val source = "file:///external/picked/image.jpg".toKmpUri()
        mimeTypeResolver.mimeTypeToReturn = MimeType.JPEG
        fileManager.copyOutcome = Outcome.Failure(FileOperationError.Unknown("copy failed"))

        assertFailure { testSubject.update(accountId, source) }
            .isInstanceOf<IOException>()

        assertThat(fileManager.deletedPaths).isEmpty()
    }

    @Test
    fun `update should succeed when removing the stale file fails`() = runTest {
        val accountId = AccountIdFactory.create()
        val source = "file:///external/picked/image.jpg".toKmpUri()
        mimeTypeResolver.mimeTypeToReturn = MimeType.JPEG
        fileManager.deleteOutcome = Outcome.Failure(FileOperationError.Unknown("delete failed"))

        val returned = testSubject.update(accountId, source)

        val expectedDest = getAvatarDir().buildUpon().appendPath("$accountId.jpg").build()
        assertThat(returned).isEqualTo(expectedDest.buildUpon().appendQueryParameter("v", "1000").build())
    }

    @Test
    fun `update with PNG should copy image to PNG path`() = runTest {
        val accountId = AccountIdFactory.create()
        val source = "file:///external/picked/photo.png".toKmpUri()
        val expectedDir = getAvatarDir()
        val expectedDest = expectedDir.buildUpon().appendPath("$accountId.png").build()
        val expectedVersioned = expectedDest.buildUpon()
            .appendQueryParameter("v", "1000")
            .build()

        mimeTypeResolver.mimeTypeToReturn = MimeType.PNG

        val returned = testSubject.update(accountId, source)

        assertThat(returned).isEqualTo(expectedVersioned)
        assertThat(fileManager.lastCopySource).isEqualTo(source)
        assertThat(fileManager.lastCopyDestination).isEqualTo(expectedDest)
    }

    @Test
    fun `delete should remove both JPG and PNG paths`() = runTest {
        val accountId = AccountIdFactory.create()
        val expectedDir = getAvatarDir()
        val jpgPath = expectedDir.buildUpon().appendPath("$accountId.jpg").build()
        val pngPath = expectedDir.buildUpon().appendPath("$accountId.png").build()

        testSubject.delete(accountId)

        assertThat(fileManager.deletedPaths).containsExactly(jpgPath, pngPath)
    }

    @Test
    fun `delete should throw when a file can not be deleted`() = runTest {
        val accountId = AccountIdFactory.create()
        fileManager.deleteOutcome = Outcome.Failure(FileOperationError.Unknown("delete failed"))

        assertFailure { testSubject.delete(accountId) }
            .isInstanceOf<IOException>()
    }

    private fun getAvatarDir(): Uri {
        return directoryProvider.getFilesDir().buildUpon()
            .appendPath(AvatarDataContract.DataSource.LocalAvatarImage.DIRECTORY_NAME)
            .build()
    }

    class FakeMimeTypeResolver : MimeTypeResolver {
        var mimeTypeToReturn: MimeType? = null
        override fun getMimeType(uri: Uri): MimeType? = mimeTypeToReturn
    }

    class SpyFileManager : FileManager {
        var lastCopySource: Uri? = null
        var lastCopyDestination: Uri? = null
        val deletedPaths = mutableListOf<Uri>()
        var copyOutcome: Outcome<Unit, FileOperationError> = Outcome.Success(Unit)
        var deleteOutcome: Outcome<Unit, FileOperationError> = Outcome.Success(Unit)

        override suspend fun copy(sourceUri: Uri, destinationUri: Uri): Outcome<Unit, FileOperationError> {
            lastCopySource = sourceUri
            lastCopyDestination = destinationUri
            return copyOutcome
        }

        override suspend fun delete(uri: Uri): Outcome<Unit, FileOperationError> {
            deletedPaths.add(uri)
            return deleteOutcome
        }

        override suspend fun createDirectories(uri: Uri): Outcome<Unit, FileOperationError> {
            return Outcome.Success(Unit)
        }
    }
}
