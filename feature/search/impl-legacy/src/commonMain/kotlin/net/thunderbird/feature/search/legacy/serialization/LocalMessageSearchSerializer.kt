package net.thunderbird.feature.search.legacy.serialization

import kotlin.text.Charsets
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import net.thunderbird.feature.mail.folder.FolderRole
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.feature.search.legacy.LocalMessageSearchType
import net.thunderbird.feature.search.legacy.UnifiedFolderSelection
import net.thunderbird.feature.search.legacy.api.MessageSearchField
import net.thunderbird.feature.search.legacy.api.SearchField

/**
 * Provides a JSON configuration for serializing and deserializing [LocalMessageSearch] objects.
 *
 * It converts [LocalMessageSearch] to and from a ByteArray using the [Json] library.
 */
object LocalMessageSearchSerializer {
    private val json = Json {
        serializersModule = SerializersModule {
            // The LocalMessageSearch internally uses a SearchCondition that requires a SearchField which is an
            // interface. To allow serialization, the SearchCondition needs to mark it's SearchField property as
            // contextual and we need to provide a serializer for it.
            contextual(SearchField::class, MessageSearchFieldAsSearchFieldSerializer)
        }
    }

    /**
     * Serializes a [LocalMessageSearch] object to a ByteArray.
     *
     * @param search The [LocalMessageSearch] object to serialize.
     * @return The serialized [LocalMessageSearch] as a ByteArray.
     */
    fun serialize(search: LocalMessageSearch): ByteArray {
        val searchString = json.encodeToString(LocalMessageSearch.serializer(), search)
        return searchString.toByteArray(Charsets.UTF_8)
    }

    /**
     * Deserializes a ByteArray to a [LocalMessageSearch] object.
     *
     * @param bytes The ByteArray to deserialize.
     * @return The deserialized [LocalMessageSearch] object.
     */
    fun deserialize(bytes: ByteArray): LocalMessageSearch {
        val searchString = String(bytes, Charsets.UTF_8)
        return json.decodeFromString(LocalMessageSearch.serializer(), searchString).apply {
            upgradeLegacyUnifiedInbox()
        }
    }

    /**
     * Older versions marked the unified inbox only by a condition on the removed integrate flag of folders. Such a
     * search, e.g. from a notification posted before an update, is turned into a unified inbox search.
     */
    private fun LocalMessageSearch.upgradeLegacyUnifiedInbox() {
        val hasIntegrateCondition = leafSet.any { it.condition?.field == MessageSearchField.INTEGRATE }
        if (hasIntegrateCondition && type !is LocalMessageSearchType.Unified) {
            type = LocalMessageSearchType.Unified(UnifiedFolderSelection.Special(FolderRole.INBOX))
        }
    }
}
