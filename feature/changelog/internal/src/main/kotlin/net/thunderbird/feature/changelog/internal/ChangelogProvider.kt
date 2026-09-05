package net.thunderbird.feature.changelog.internal

import android.content.Context
import androidx.annotation.RawRes
import kotlinx.serialization.json.Json
import net.thunderbird.feature.navigation.changelog.api.ChangelogConfigProvider

class ChangelogProvider(
    private val context: Context,
    private val provider: ChangelogConfigProvider,
) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun getChangeLog(): List<ReleaseItem> {
        val indexResourceId = provider.changelogIndexResId
        val index = readRawJson<ChangelogIndex>(indexResourceId)
        return index.releases.mapNotNull { releaseEntry ->
            val resourceId = context.resources.getIdentifier(
                releaseEntry.resourceName,
                "raw",
                context.packageName,
            )

            if (resourceId == 0) {
                null
            } else {
                readRawJson<ChangelogRelease>(resourceId).toReleaseItem()
            }
        }
    }

    fun getChangeLogSince(lastVersionCode: Int): List<ReleaseItem> {
        return getChangeLog().filter { it.versionCode > lastVersionCode }
    }

    private inline fun <reified T> readRawJson(
        @RawRes resourceId: Int,
    ): T {
        val jsonString = context.resources.openRawResource(resourceId)
            .bufferedReader()
            .use { it.readText() }

        return json.decodeFromString(jsonString)
    }
}

fun ChangelogRelease.toReleaseItem(): ReleaseItem {
    return ReleaseItem(
        versionCode = this.versioncode,
        versionName = this.version,
        date = this.date,
        changes = this.notes.map { it.text },
    )
}
