package app.k9mail.autodiscovery.autoconfig

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import net.thunderbird.core.common.net.Domain

internal class SuspendableMxResolver(private val mxResolver: MxResolver) {
    suspend fun lookup(domain: Domain): MxLookupResult {
        return runInterruptible(Dispatchers.IO) {
            mxResolver.lookup(domain)
        }
    }
}
