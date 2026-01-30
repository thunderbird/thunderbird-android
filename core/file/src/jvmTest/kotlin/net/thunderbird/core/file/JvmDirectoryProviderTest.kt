package net.thunderbird.core.file

import assertk.assertThat
import assertk.assertions.isTrue
import java.io.File
import java.net.URI
import kotlin.test.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class JvmDirectoryProviderTest {

    @JvmField
    @Rule
    val folder = TemporaryFolder()

    @Test
    fun `getCacheDir and getFilesDir should return existing directories`() {
        // Arrange: use TemporaryFolder as user.home so test doesn't touch real FS
        val originalUserHome: String? = System.getProperty("user.home")
        try {
            System.setProperty("user.home", folder.root.absolutePath)

            val appName = "TbTest"
            val provider = JvmDirectoryProvider(appName)

            // Act
            val cacheDir = uriToFile(provider.getCacheDir())
            val filesDir = uriToFile(provider.getFilesDir())

            // Assert
            assertThat(cacheDir.exists() && cacheDir.isDirectory).isTrue()
            assertThat(filesDir.exists() && filesDir.isDirectory).isTrue()
        } finally {
            if (originalUserHome != null) {
                System.setProperty("user.home", originalUserHome)
            } else {
                System.clearProperty("user.home")
            }
        }
    }

    private fun uriToFile(uri: com.eygraber.uri.Uri): File = File(URI(uri.toString()))
}
