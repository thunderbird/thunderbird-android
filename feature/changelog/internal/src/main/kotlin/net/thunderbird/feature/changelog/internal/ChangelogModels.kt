package net.thunderbird.feature.changelog.internal

import kotlinx.serialization.Serializable

@Serializable
data class ChangelogIndex(
    val schemaVersion: Int,
    val releases: List<ReleaseEntry>,
)

@Serializable
data class ReleaseEntry(
    val version: String,
    val versioncode: Int,
    val date: String,
    val resourceName: String,
)

@Serializable
data class ChangelogRelease(
    val schemaVersion: Int,
    val versioncode: Int,
    val version: String,
    val date: String,
    val notes: List<ChangelogNote>,
)

@Serializable
data class ChangelogNote(
    val type: String,
    val text: String,
    val source: String,
)
