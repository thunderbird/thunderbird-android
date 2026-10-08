package net.thunderbird.feature.mail.message

import kotlin.uuid.ExperimentalUuidApi
import net.thunderbird.core.architecture.model.BaseUuidIdentifierFactory

/**
 * Factory for creating [ThreadId] values.
 */
@OptIn(ExperimentalUuidApi::class)
object ThreadIdFactory : BaseUuidIdentifierFactory<ThreadId>(::ThreadId)
