package net.thunderbird.core.common.provider
/**
 * Provides the application version.
 */
interface AppVersionProvider {
    fun getVersionNumber(): String
}
