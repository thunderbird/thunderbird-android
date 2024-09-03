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
    fun `getAppInfoList() with another app installed`() {
        installPackage("com.fsck.k9", "K-9 Mail")

        val result = importAppFetcher.getAppInfoList()

        assertThat(result).containsExactly(
            AppInfo("com.fsck.k9", "K-9 Mail"),
        )
    }

    @Test
    fun `getAppInfoList() with multiple other apps installed`() {
        installPackage("com.fsck.k9", "K-9 Mail")
        installPackage("net.thunderbird.android.beta", "Thunderbird Beta")
        installPackage("net.thunderbird.android.daily", "Thunderbird Daily")

        val result = importAppFetcher.getAppInfoList()

        assertThat(result).containsExactlyInAnyOrder(
            AppInfo("com.fsck.k9", "K-9 Mail"),
            AppInfo("net.thunderbird.android.beta", "Thunderbird Beta"),
            AppInfo("net.thunderbird.android.daily", "Thunderbird Daily"),
        )
    }

    private fun createPackageContext(): Context {
        // Robolectric doesn't easily support setting the package name of the test app. So we create a Context instance
        // whose getPackageName() method will return the desired package name.
        installPackage(MY_PACKAGE_NAME, "Test App")
        return appContext.createPackageContext(MY_PACKAGE_NAME, 0)
    }

    private fun installPackage(packageName: String, name: String = "irrelevant") {
        val packageManager = shadowOf(appContext.packageManager)

        packageManager.installPackage(
            PackageInfo().apply {
                this.packageName = packageName
                this.applicationInfo = ApplicationInfo().apply {
                    this.name = name
                }
            },
        )
    }
}
