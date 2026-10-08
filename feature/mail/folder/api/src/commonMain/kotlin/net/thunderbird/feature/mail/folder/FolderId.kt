package net.thunderbird.feature.mail.folder

import kotlin.uuid.Uuid
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import net.thunderbird.core.architecture.model.BaseUuidIdentifier
import net.thunderbird.core.architecture.model.UuidIdentifierSerializer

/**
 * Identifies a local folder record across all accounts.
 */
@Serializable(with = FolderIdSerializer::class)
public class FolderId(
    value: Uuid,
) : BaseUuidIdentifier(value)

public object FolderIdSerializer : KSerializer<FolderId> by UuidIdentifierSerializer(FolderIdFactory::of)
