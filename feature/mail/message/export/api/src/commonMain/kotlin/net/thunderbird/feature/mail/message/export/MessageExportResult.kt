package net.thunderbird.feature.mail.message.export

import net.thunderbird.core.outcome.Outcome

/**
 * Result type for message export using the shared Outcome abstraction.
 *
 * Success carries Unit. Failure carries an [MessageExportError].
 */
typealias MessageExportResult = Outcome<Unit, MessageExportError>
