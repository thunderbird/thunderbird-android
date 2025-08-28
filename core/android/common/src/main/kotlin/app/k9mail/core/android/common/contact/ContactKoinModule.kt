package app.k9mail.core.android.common.contact

import android.content.Context
import kotlin.time.ExperimentalTime
import net.thunderbird.core.common.cache.Cache
import net.thunderbird.core.common.cache.ExpiringCache
import net.thunderbird.core.common.cache.SynchronizedCache
import net.thunderbird.core.common.mail.EmailAddress
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal val contactModule = module {
    single<Cache<EmailAddress, Contact?>>(named(CACHE_NAME)) {
        @OptIn(ExperimentalTime::class)
        SynchronizedCache(
            delegateCache = ExpiringCache(clock = get()),
        )
    }
    factory<ContactDataSource> {
        ContentResolverContactDataSource(
            contentResolver = get<Context>().contentResolver,
            contactPermissionResolver = get(),
        )
    }
    factory<ContactRepository> {
        CachingContactRepository(
            cache = get(named(CACHE_NAME)),
            dataSource = get(),
        )
    }
    factory<ContactPermissionResolver> {
        AndroidContactPermissionResolver(context = get())
    }
}

internal const val CACHE_NAME = "ContactCache"
