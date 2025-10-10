package app.k9mail.core.android.common.contact

import android.net.Uri
import net.thunderbird.core.common.cache.Cache
import net.thunderbird.core.common.mail.EmailAddress
import net.thunderbird.core.common.mail.toEmailAddressOrNull

interface ContactRepository {

    fun getContactFor(emailAddress: EmailAddress): Contact?

    fun hasContactFor(emailAddress: EmailAddress): Boolean

    fun hasAnyContactFor(emailAddresses: List<EmailAddress>): Boolean

    fun getPhotoUri(emailAddress: String): Uri?
}

interface CachingRepository {
    fun clearCache()
}

internal class CachingContactRepository(
    private val cache: Cache<EmailAddress, Contact?>,
    private val dataSource: ContactDataSource,
) : ContactRepository, CachingRepository {

    override fun getContactFor(emailAddress: EmailAddress): Contact? {
        if (cache.hasKey(emailAddress)) {
            return cache[emailAddress]
        }

        return dataSource.getContactFor(emailAddress).also {
            cache[emailAddress] = it
        }
    }

    override fun hasContactFor(emailAddress: EmailAddress): Boolean {
        if (cache.hasKey(emailAddress)) {
            return cache[emailAddress] != null
        }

        return dataSource.hasContactFor(emailAddress)
    }

    override fun hasAnyContactFor(emailAddresses: List<EmailAddress>): Boolean =
        emailAddresses.any { emailAddress -> hasContactFor(emailAddress) }

    override fun getPhotoUri(emailAddress: String): Uri? {
        return emailAddress.toEmailAddressOrNull()?.let { emailAddress ->
            getContactFor(emailAddress)?.photoUri
        }
    }

    override fun clearCache() {
        cache.clear()
    }
}
