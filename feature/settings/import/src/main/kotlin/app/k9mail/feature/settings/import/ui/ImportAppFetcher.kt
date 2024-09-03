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
        return supportedApps.any { packageName -> packageManager.isAppInstalled(packageName) }
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
            .mapNotNull { packageName -> packageManager.loadAppInfo(packageName) }
            .toList()
    }

    @Suppress("SwallowedException")
    private fun PackageManager.loadAppInfo(packageName: String): AppInfo? {
        return try {
            val applicationInfo = getApplicationInfo(packageName, 0)
            val appName = packageManager.getApplicationLabel(applicationInfo).toString()

            AppInfo(packageName, appName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /**
     * Get the list of application IDs of supported apps excluding our own app.
     */
    private val supportedApps: Sequence<String>
        get() {
            val myPackageName = context.packageName
            return SUPPORTED_APPS
                .asSequence()
                .filterNot { packageName -> packageName == myPackageName }
        }

    companion object {
        private val SUPPORTED_APPS = listOf(
            // K-9 Mail
            "com.fsck.k9",
            // Thunderbird for Android (release)
            "net.thunderbird.android",
            // Thunderbird for Android (beta)
            "net.thunderbird.android.beta",
            // Thunderbird for Android (daily)
            "net.thunderbird.android.daily",
        )
    }
}

internal data class AppInfo(val packageName: String, private val appName: String) {
    // ArrayAdapter is using `toString()` when rendering list items. See PickAppDialogFragment.
    override fun toString() = appName
}
