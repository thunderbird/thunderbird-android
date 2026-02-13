package net.thunderbird.feature.applock.impl.domain

import net.thunderbird.feature.applock.api.AppLockConfig

/**
 * Storage access for app lock configuration.
 */
internal interface AppLockConfigRepository {
    fun getConfig(): AppLockConfig

    fun setConfig(config: AppLockConfig)
}
