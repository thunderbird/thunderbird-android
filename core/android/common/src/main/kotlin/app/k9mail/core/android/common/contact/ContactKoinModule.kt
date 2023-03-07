package app.k9mail.core.android.common.contact

import app.k9mail.core.common.cache.Cache
import app.k9mail.core.common.cache.ExpiringCache
import app.k9mail.core.common.cache.SynchronizedCache
import app.k9mail.core.common.mail.EmailAddress
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal val contactModule = module {
    single<Cache<EmailAddress, Contact>>(named(CACHE_NAME)) {
        SynchronizedCache(
            delegateCache = ExpiringCache(clock = get()),
        )
    }
    factory<ContactDataSource> {
        ContentResolverContactDataSource(context = get())
    }
    factory<ContactRepository> {
        CachingContactRepository(
            cache = get(named(CACHE_NAME)),
            dataSource = get(),
        )
    }
}

internal const val CACHE_NAME = "ContactCache"
