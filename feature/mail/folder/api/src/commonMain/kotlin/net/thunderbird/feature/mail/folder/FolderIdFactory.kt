package net.thunderbird.feature.mail.folder

import kotlin.uuid.ExperimentalUuidApi
import net.thunderbird.core.architecture.model.BaseUuidIdentifierFactory

/**
 * Factory for creating [FolderId] values.
 */
@OptIn(ExperimentalUuidApi::class)
object FolderIdFactory : BaseUuidIdentifierFactory<FolderId>(::FolderId)
