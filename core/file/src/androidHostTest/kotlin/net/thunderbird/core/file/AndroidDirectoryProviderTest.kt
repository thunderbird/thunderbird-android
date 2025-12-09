package net.thunderbird.core.file

import android.content.Context
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.eygraber.uri.toAndroidUri
import java.io.File
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AndroidDirectoryProviderTest {

    private val appContext: Context = RuntimeEnvironment.getApplication()

    private val provider = AndroidDirectoryProvider(appContext)

    @Test
    fun `getCacheDir and getFilesDir should return existing directories and match context paths`() {
        // Act
        val cacheUri = provider.getCacheDir()
        val filesUri = provider.getFilesDir()

        val cacheAndroidUri = cacheUri.toAndroidUri()
        val filesAndroidUri = filesUri.toAndroidUri()

        val cacheDirFromProvider = File(requireNotNull(cacheAndroidUri.path))
        val filesDirFromProvider = File(requireNotNull(filesAndroidUri.path))

        // Assert: directories exist
        assertThat(cacheDirFromProvider.exists() && cacheDirFromProvider.isDirectory).isTrue()
        assertThat(filesDirFromProvider.exists() && filesDirFromProvider.isDirectory).isTrue()

        // Assert: paths match the Context-provided directories
        assertThat(cacheDirFromProvider.absolutePath).isEqualTo(appContext.cacheDir.absolutePath)
        assertThat(filesDirFromProvider.absolutePath).isEqualTo(appContext.filesDir.absolutePath)
    }
}
