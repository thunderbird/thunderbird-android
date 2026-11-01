package net.thunderbird.core.preference.network

import net.thunderbird.core.preference.BackgroundOps

val NETWORK_SETTINGS_DEFAULT_BACKGROUND_OPS = BackgroundOps.ALWAYS

data class NetworkSettings(
    val backgroundOps: BackgroundOps = NETWORK_SETTINGS_DEFAULT_BACKGROUND_OPS,
)
