package net.thunderbird.app.common.appConfig

import net.thunderbird.app.common.BuildConfig
import net.thunderbird.core.common.appConfig.PlatformConfigProvider

class AndroidPlatformConfigProvider : PlatformConfigProvider {
    override val isDebug: Boolean = BuildConfig.DEBUG
}
