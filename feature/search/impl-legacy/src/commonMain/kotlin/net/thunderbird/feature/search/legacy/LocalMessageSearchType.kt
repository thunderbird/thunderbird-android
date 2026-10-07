package net.thunderbird.feature.search.legacy

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import net.thunderbird.feature.mail.folder.FolderType

/**
 * The type of a [LocalMessageSearch], defining how the search is bound to accounts and folders.
 */
@Serializable
sealed interface LocalMessageSearchType {
    /**
     * A search bound to a single account, spanning 1..n folders and the criteria defined in the search.
     */
    @Serializable
    @SerialName("account")
    data object Account : LocalMessageSearchType

    /**
     * The list of new messages, e.g. opened from a notification.
     *
     * This is always bound to a single account (see [LocalMessageSearch.id]). A cross-account variant should be
     * modeled as a unified search, e.g. `Unified(UnifiedFolderSelection.NewMessages)`.
     */
    @Serializable
    @SerialName("new_messages")
    data object NewMessages : LocalMessageSearchType

    /**
     * A search across accounts, made of 1 account → 1 folder relations.
     *
     * Still backed by the legacy `INTEGRATE` flag until migrated.
     */
    @Serializable
    @SerialName("unified")
    data class Unified(val folder: UnifiedFolderSelection) : LocalMessageSearchType
}

/**
 * Selects the folder of each account that is part of a [LocalMessageSearchType.Unified] search.
 */
@Serializable
sealed interface UnifiedFolderSelection {
    /**
     * The special folder of the given [folderType] in each account.
     */
    @Serializable
    @SerialName("special")
    data class Special(val folderType: FolderType) : UnifiedFolderSelection
}

/**
 * Serializer for [LocalMessageSearchType] that falls back to [LocalMessageSearchType.Account] for unknown values.
 */
internal object LocalMessageSearchTypeSerializer : KSerializer<LocalMessageSearchType> {
    private val delegate = LocalMessageSearchType.serializer()

    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: LocalMessageSearchType) {
        delegate.serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): LocalMessageSearchType {
        val jsonDecoder = decoder as? JsonDecoder ?: return delegate.deserialize(decoder)
        val element = jsonDecoder.decodeJsonElement()
        return try {
            jsonDecoder.json.decodeFromJsonElement(delegate, element)
        } catch (_: SerializationException) {
            LocalMessageSearchType.Account
        }
    }
}
