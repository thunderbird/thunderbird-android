package net.thunderbird.feature.account.avatar.data.datasource

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.eygraber.uri.toKmpUri
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.file.DirectoryProvider
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.avatar.data.AvatarDataContract
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class LocalAvatarImageDataSourceTest {

    @JvmField
    @Rule
    val folder = TemporaryFolder()

    private lateinit var directoryProvider: DirectoryProvider
    private lateinit var fileManager: CapturingFileManager
    private lateinit var testSubject: LocalAvatarImageDataSource

    @BeforeTest
    fun setUp() {
        val appDir = folder.newFolder("app")
        directoryProvider = FakeDirectoryProvider(appDir.absolutePath.toKmpUri())
        fileManager = CapturingFileManager()
        testSubject = LocalAvatarImageDataSource(fileManager, directoryProvider)
    }

    @Test
    fun `update should copy image to expected path and return destination uri`() = runTest {
        // Arrange
        val accountId = AccountIdFactory.create()
        val source = "file:///external/picked/image.jpg".toKmpUri()
        val expectedDir = directoryProvider.getFilesDir().buildUpon()
            .appendPath(AvatarDataContract.DataSource.LocalAvatarImage.DIRECTORY_NAME)
            .build()
        val expectedDest = expectedDir.buildUpon().appendPath("${accountId.asRaw()}.jpg").build()

        // Act
        val returned = testSubject.update(accountId, source)

        // Assert
        assertThat(returned).isEqualTo(expectedDest)
        assertThat(fileManager.lastCreatedDir).isEqualTo(expectedDir)
        assertThat(fileManager.lastCopySource).isEqualTo(source)
        assertThat(fileManager.lastCopyDestination).isEqualTo(expectedDest)
        assertThat(fileManager.lastDeleted).isNull()
    }

    @Test
    fun `delete should remove expected avatar path`() = runTest {
        // Arrange
        val accountId = AccountIdFactory.create()
        val expectedDir = directoryProvider.getFilesDir().buildUpon()
            .appendPath(AvatarDataContract.DataSource.LocalAvatarImage.DIRECTORY_NAME)
            .build()
        val expectedDest = expectedDir.buildUpon().appendPath("${accountId.asRaw()}.jpg").build()

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
