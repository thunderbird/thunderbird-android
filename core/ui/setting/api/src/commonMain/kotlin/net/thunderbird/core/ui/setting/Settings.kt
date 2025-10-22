package net.thunderbird.core.ui.setting

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * A immutable list of settings.
 */
typealias Settings = ImmutableList<Setting>

/**
 * Returns an empty list of settings.
 */
fun emptySettings(): Settings = persistentListOf()
