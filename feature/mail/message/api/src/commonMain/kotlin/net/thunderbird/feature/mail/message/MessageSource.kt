package net.thunderbird.feature.mail.message

import net.thunderbird.piisafe.annotation.PiiSafe

/**
 * The representation a [Message] was created from, when it came from outside the domain
 * (a server sync, an import, a legacy store). Repositories that can persist this form
 * directly should prefer it over re-encoding the domain fields, since the domain model is
 * a lossy view of a MIME message. Implementations live in the data layer.
 */
@PiiSafe.HasPii
interface MessageSource
