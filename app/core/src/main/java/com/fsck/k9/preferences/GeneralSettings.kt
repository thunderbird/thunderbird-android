package com.fsck.k9.preferences

/**
 * Stores a snapshot of the app's general settings.
 *
 * TODO: Add more settings as needed.
 */
data class GeneralSettings(
    val backgroundSync: BackgroundSync
)

enum class BackgroundSync {
    ALWAYS,
    NEVER,
    FOLLOW_SYSTEM_AUTO_SYNC
}
