package net.thunderbird.feature.changelog.internal

import android.content.Context
import android.content.res.Resources
import androidx.annotation.RawRes
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.navigation.changelog.api.ChangelogConfigProvider

private const val TAG = "ChangelogProvider"
private const val SUPPORTED_SCHEMA_VERSION = 1

class ChangelogProvider(
    private val context: Context,
    private val provider: ChangelogConfigProvider,
    private val logger: Logger,
) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun getChangeLog(): List<ReleaseItem> {
        val indexResourceId = provider.changelogIndexResId

        return try {
            val index = readRawJson<ChangelogIndex>(indexResourceId)

            index.releases.mapNotNull { releaseEntry ->
                val resourceId = context.resources.getIdentifier(
                    releaseEntry.resourceName,
                    "raw",
                    context.packageName,
                )

                if (resourceId == 0) {
                    logger.error(TAG) {
                        "Changelog resource not found: ${releaseEntry.resourceName}"
                    }
                    null
                } else {
                    runCatching {
                        readRawJson<ChangelogRelease>(resourceId)
                            .apply { validateSchemaVersion() }
                            .toReleaseItem()
                    }.onFailure { exception ->
                        logger.error(TAG, exception) {
                            "Failed to load changelog resource: ${releaseEntry.resourceName}"
                        }
                    }.getOrNull()
                }
            }
        } catch (exception: Resources.NotFoundException) {
            logger.error(TAG, exception) {
                "Changelog index resource not found"
            }
            emptyList()
        } catch (exception: SerializationException) {
            logger.error(TAG, exception) {
                "Failed to parse changelog JSON"
            }
            emptyList()
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

    private fun ChangelogRelease.validateSchemaVersion() {
        check(schemaVersion == SUPPORTED_SCHEMA_VERSION) {
            "Unsupported changelog schema version: $schemaVersion"
        }
    }
}

fun ChangelogRelease.toReleaseItem(): ReleaseItem {
    return ReleaseItem(
        versionCode = versioncode,
        versionName = version,
        date = date,
        changes = notes.map { "${it.type.capitalized()}:${it.text}" },
    )
}

private fun String.capitalized() = replaceFirstChar {
    if (it.isLowerCase()) it.titlecase() else it.toString()
}
