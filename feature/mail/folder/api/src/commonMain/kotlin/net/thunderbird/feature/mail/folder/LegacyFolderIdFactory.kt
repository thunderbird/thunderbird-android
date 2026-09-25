package net.thunderbird.feature.mail.folder

import kotlin.uuid.ExperimentalUuidApi
import net.thunderbird.core.architecture.model.LegacyEntityIdFactory

/**
 * Factory for creating [FolderId] values.
 */
@OptIn(ExperimentalUuidApi::class)
object LegacyFolderIdFactory : LegacyEntityIdFactory<FolderId>(
    entityByteRepresentation = ByteRepresentation.FOLDERS,
    fromUuid = ::FolderId,
)
