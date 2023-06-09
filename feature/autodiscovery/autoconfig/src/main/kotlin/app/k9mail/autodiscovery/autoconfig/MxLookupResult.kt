package app.k9mail.autodiscovery.autoconfig

import app.k9mail.core.common.net.Domain

/**
 * Result for [MxResolver].
 *
 * @param mxNames The hostnames from the MX records.
 * @param isTrusted `true` iff the results were properly signed (DNSSEC) or were retrieved using a secure channel.
 */
data class MxLookupResult(
    val mxNames: List<Domain>,
    val isTrusted: Boolean,
)
