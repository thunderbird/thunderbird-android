package app.k9mail.core.android.common.contact

import app.k9mail.core.common.cache.Cache
import app.k9mail.core.common.mail.EmailAddress

interface ContactRepository {

    fun getContactFor(emailAddress: EmailAddress): Contact?

    fun hasContactFor(emailAddress: EmailAddress): Boolean

    fun hasAnyContactFor(emailAddresses: List<EmailAddress>): Boolean
}

interface CachingRepository {
    fun clearCache()
}

internal class CachingContactRepository(
    private val cache: Cache<EmailAddress, Contact>,
    private val dataSource: ContactDataSource,
) : ContactRepository, CachingRepository {

    override fun getContactFor(emailAddress: EmailAddress): Contact? {
        if (cache.hasKey(emailAddress)) {
            return cache[emailAddress]
        }

        val contact = dataSource.getContactFor(emailAddress)

        if (contact != null) {
            cache[emailAddress] = contact
        }

        return contact
    }

    override fun hasContactFor(emailAddress: EmailAddress): Boolean {
        return dataSource.hasContactFor(emailAddress)
    }

    override fun hasAnyContactFor(emailAddresses: List<EmailAddress>): Boolean =
        emailAddresses.any { emailAddress -> hasContactFor(emailAddress) }

    override fun clearCache() {
        cache.clear()
    }
}
