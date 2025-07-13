package net.thunderbird.core.preference.network

import net.thunderbird.core.preference.BackgroundOps

data class NetworkSettings(
    val backgroundOps: BackgroundOps = BackgroundOps.ALWAYS,
)
