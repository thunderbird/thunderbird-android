package app.k9mail.core.android.common.contact

import org.koin.dsl.module

internal val contactModule = module {
    factory<ContactDataSource> {
        ContentResolverContactDataSource(context = get())
    }
}
