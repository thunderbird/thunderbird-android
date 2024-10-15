package app.k9mail.feature.settings.import.ui

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.test.core.app.ApplicationProvider
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

private const val MY_PACKAGE_NAME = "net.thunderbird.android"

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ImportAppFetcherTest {
    private val appContext = ApplicationProvider.getApplicationContext<Context>()
    private val context = createPackageContext()
    private val importAppFetcher = ImportAppFetcher(context)

    @Test
    fun `isAtLeastOneAppInstalled() without any other apps installed`() {
        val result = importAppFetcher.isAtLeastOneAppInstalled()

        assertThat(result).isFalse()
    }

    @Test
    fun `isAtLeastOneAppInstalled() with another app installed`() {
        installPackage("com.fsck.k9")

        val result = importAppFetcher.isAtLeastOneAppInstalled()

        assertThat(result).isTrue()
    }

    @Test
    fun `getAppInfoList() without any other apps installed`() {
        val result = importAppFetcher.getAppInfoList()

        assertThat(result).isEmpty()
    }

    @Test
    fun `getAppInfoList() with another supported app installed`() {
        installPackage("com.fsck.k9", "K-9 Mail", versionCode = 40000)

        val result = importAppFetcher.getAppInfoList()

        assertThat(result).containsExactly(
            AppInfo("com.fsck.k9", "K-9 Mail", isImportSupported = true),
        )
    }

    @Test
    fun `getAppInfoList() with another unsupported app installed`() {
        installPackage("com.fsck.k9", "K-9 Mail", versionCode = 39004)

        val result = importAppFetcher.getAppInfoList()

        assertThat(result).containsExactly(
            AppInfo("com.fsck.k9", "K-9 Mail", isImportSupported = false),
        )
    }

    @Test
    fun `getAppInfoList() with multiple other supported apps installed`() {
        installPackage("com.fsck.k9", "K-9 Mail", versionCode = 39005)
        installPackage("net.thunderbird.android.beta", "Thunderbird Beta", versionCode = 4)
        installPackage("net.thunderbird.android.daily", "Thunderbird Daily", versionCode = 1)

        val result = importAppFetcher.getAppInfoList()

        assertThat(result).containsExactlyInAnyOrder(
            AppInfo("com.fsck.k9", "K-9 Mail", isImportSupported = true),
            AppInfo("net.thunderbird.android.beta", "Thunderbird Beta", isImportSupported = true),
            AppInfo("net.thunderbird.android.daily", "Thunderbird Daily", isImportSupported = true),
        )
    }

    private fun createPackageContext(): Context {
        // Robolectric doesn't easily support setting the package name of the test app. So we create a Context instance
        // whose getPackageName() method will return the desired package name.
        installPackage(MY_PACKAGE_NAME, "Test App")
        return appContext.createPackageContext(MY_PACKAGE_NAME, 0)
    }

    private fun installPackage(packageName: String, name: String = "irrelevant", versionCode: Int = 1) {
        val packageManager = shadowOf(appContext.packageManager)

        packageManager.installPackage(
            PackageInfo().apply {
                this.packageName = packageName
                this.applicationInfo = ApplicationInfo().apply {
                    this.name = name
                }
                this.versionCode = versionCode
            },
        )
    }
}
