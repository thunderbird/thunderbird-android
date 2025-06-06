package net.thunderbird.core.common.resources

/**
 * Represents a comprehensive resource manager that combines string and plural resource handling.
 *
 * This interface extends both [StringsResourceManager] and [PluralsResourceManager], providing a unified
 * interface for accessing different types of string-based resources within the application.
 *
 * Implementations of this interface should provide concrete implementations for fetching both
 * simple strings and pluralized strings based on their respective resource IDs.
 */
interface ResourceManager : StringsResourceManager, PluralsResourceManager
