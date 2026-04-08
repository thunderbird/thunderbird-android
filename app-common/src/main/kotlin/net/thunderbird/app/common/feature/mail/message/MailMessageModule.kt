package net.thunderbird.app.common.feature.mail.message

import com.fsck.k9.K9
import net.thunderbird.app.common.feature.mail.message.list.LegacyUpdateSortCriteria
import net.thunderbird.core.android.account.SortType
import net.thunderbird.feature.mail.message.export.DefaultMessageFileNameSuggester
import net.thunderbird.feature.mail.message.export.MessageExporter
import net.thunderbird.feature.mail.message.export.MessageFileNameSuggester
import net.thunderbird.feature.mail.message.export.eml.EmlMessageExporter
import net.thunderbird.feature.mail.message.list.domain.model.SortCriteria
import net.thunderbird.feature.mail.message.list.extension.toSortType
import org.koin.dsl.module
import net.thunderbird.feature.mail.message.list.domain.DomainContract as MessageListDomainContract

internal val mailMessageModule = module {
    single<MessageFileNameSuggester> { DefaultMessageFileNameSuggester() }

    single<MessageExporter> {
        EmlMessageExporter(
            fileManager = get(),
        )
    }

    single<MessageListDomainContract.UseCase.GetDefaultSortCriteria> {
        MessageListDomainContract.UseCase.GetDefaultSortCriteria {
            val primary = K9.sortType.toSortType(isAscending = K9.isSortAscending(K9.sortType))
            val secondary = K9.sortType
                .takeUnless { it in setOf(SortType.SORT_DATE, SortType.SORT_ARRIVAL) }
                ?.let(K9::isSortAscending)
                ?.let(SortType.SORT_DATE::toSortType)
            SortCriteria(primary = primary, secondary = secondary)
        }
    }

    single<MessageListDomainContract.UseCase.UpdateSortCriteria> {
        LegacyUpdateSortCriteria(logger = get(), accountManager = get())
    }
}
