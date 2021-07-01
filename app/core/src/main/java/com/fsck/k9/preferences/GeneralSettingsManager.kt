package com.fsck.k9.preferences

import kotlinx.coroutines.flow.Flow

/**
 * Retrieve general settings.
 *
 * TODO: Add a way to mutate general settings.
 */
interface GeneralSettingsManager {
    fun getSettings(): GeneralSettings
    fun getSettingsFlow(): Flow<GeneralSettings>
}
