package net.thunderbird.core.architecture.model

import kotlin.uuid.Uuid

/**
 * A base class for identifiers that use a [Uuid] as their underlying value.
 *
 * @param value The UUID value of the identifier.
 */
abstract class BaseUuidIdentifier(value: Uuid) : BaseIdentifier<Uuid>(value)
