package net.thunderbird.feature.applock.impl.data

import android.content.Context
import androidx.core.content.edit
import net.thunderbird.feature.applock.api.AppLockConfig
import net.thunderbird.feature.applock.impl.domain.AppLockConfigRepository

/**
 * Storage for authentication state and configuration.
 *
 * Stores:
 * - Authentication enabled/disabled state
 * - Timeout configuration
 *
 * @param context Application context for creating preferences.
 */
internal class AppLockConfigStore(context: Context) : AppLockConfigRepository {
    private val preferences = context.getSharedPreferences(
        AppLockPreferences.PREFS_FILE_NAME,
        Context.MODE_PRIVATE,
    )

    override fun getConfig(): AppLockConfig {
        return AppLockConfig(
            isEnabled = preferences.getBoolean(
                AppLockPreferences.KEY_ENABLED,
                AppLockConfig.DEFAULT_ENABLED,
            ),
            timeoutMillis = preferences.getLong(
                AppLockPreferences.KEY_TIMEOUT_MILLIS,
                AppLockConfig.DEFAULT_TIMEOUT_MILLIS,
            ),
        )
    }

    override fun setConfig(config: AppLockConfig) {
        preferences.edit {
            putBoolean(AppLockPreferences.KEY_ENABLED, config.isEnabled)
            putLong(AppLockPreferences.KEY_TIMEOUT_MILLIS, config.timeoutMillis)
        }
    }
}
