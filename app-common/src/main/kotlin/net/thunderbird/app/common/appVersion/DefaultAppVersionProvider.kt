package net.thunderbird.app.common.appVersion

import android.content.Context
import android.content.pm.PackageManager
import net.thunderbird.core.common.provider.AppVersionProvider
import net.thunderbird.core.logging.Logger

private const val TAG = "DefaultAppVersionProvider"

class DefaultAppVersionProvider(
    private val context: Context,
    private var logger: Logger,
) : AppVersionProvider {
    override fun getVersionNumber(): String {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            return packageInfo.versionName ?: "?"
        } catch (e: PackageManager.NameNotFoundException) {
            logger.error(TAG, e, { "Error getting PackageInfo" })
            return "?"
        }
    }
}
