package net.thunderbird.core.validation

import net.thunderbird.core.outcome.Outcome

val ValidationSuccess: ValidationOutcome
    get() = Outcome.Success(Unit)
