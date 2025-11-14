package net.thunderbird.core.file

/**
 * Indicates how a sink should be opened for writing.
 *
 * - [Truncate]: Overwrite existing content or create if missing
 * - [Append]: Append to existing content or create if missing
 */
enum class WriteMode {
    Truncate,
    Append,
}
