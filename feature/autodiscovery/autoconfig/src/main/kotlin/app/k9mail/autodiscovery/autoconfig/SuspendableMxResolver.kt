package app.k9mail.autodiscovery.autoconfig

import app.k9mail.core.common.net.Domain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible

internal class SuspendableMxResolver(private val mxResolver: MxResolver) {
    suspend fun lookup(domain: Domain): MxLookupResult {
        return runInterruptible(Dispatchers.IO) {
            mxResolver.lookup(domain)
        }
    }
}
