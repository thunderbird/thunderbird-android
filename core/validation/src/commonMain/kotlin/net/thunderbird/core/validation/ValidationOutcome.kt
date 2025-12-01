package net.thunderbird.core.validation

import net.thunderbird.core.outcome.Outcome

/**
 * Represents the result of a validation operation.
 */
typealias ValidationOutcome = Outcome<Unit, ValidationError>
