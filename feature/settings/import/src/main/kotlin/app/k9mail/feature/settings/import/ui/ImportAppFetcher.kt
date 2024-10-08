package app.k9mail.feature.settings.import.ui

import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.WorkerThread

internal class ImportAppFetcher(
    private val context: Context,
) {
    private val packageManager by lazy { context.packageManager }

    /**
     * Returns `true` if at least one app is installed from which we can import settings.
     */
    @WorkerThread
    fun isAtLeastOneAppInstalled(): Boolean {
        return supportedApps.any { app -> packageManager.isAppInstalled(app.packageName) }
    }

    @Suppress("SwallowedException")
    private fun PackageManager.isAppInstalled(packageName: String): Boolean {
        return try {
            getApplicationInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Get list of apps from which we can import settings.
     */
    @WorkerThread
    fun getAppInfoList(): List<AppInfo> {
        return supportedApps
            .mapNotNull { app -> packageManager.loadAppInfo(app) }
            .toList()
    }

    @Suppress("SwallowedException")
    private fun PackageManager.loadAppInfo(app: AppVersion): AppInfo? {
        return try {
            val packageInfo = getPackageInfo(app.packageName, 0)
            val isImportSupported = packageInfo.versionCode >= app.minVersionCode

            val applicationInfo = getApplicationInfo(app.packageName, 0)
            val appName = packageManager.getApplicationLabel(applicationInfo).toString()

            AppInfo(app.packageName, appName, isImportSupported)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /**
     * Get the list of application IDs of supported apps excluding our own app.
     */
    private val supportedApps: Sequence<AppVersion>
        get() {
            val myPackageName = context.packageName
            return SUPPORTED_APPS
                .asSequence()
                .filterNot { app -> app.packageName == myPackageName }
        }

    companion object {
        private val SUPPORTED_APPS = listOf(
            // K-9 Mail
            AppVersion("com.fsck.k9", 39005),
            // Thunderbird for Android (release)
            AppVersion("net.thunderbird.android", 1),
            // Thunderbird for Android (beta)
            AppVersion("net.thunderbird.android.beta", 4),
            // Thunderbird for Android (daily)
            AppVersion("net.thunderbird.android.daily", 1),
        )
    }
}

private data class AppVersion(val packageName: String, val minVersionCode: Int)

internal data class AppInfo(
    val packageName: String,
    val appName: String,
    val isImportSupported: Boolean,
) {
    // ArrayAdapter is using `toString()` when rendering list items. See PickAppDialogFragment.
    override fun toString() = appName
}
