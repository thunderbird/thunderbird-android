package net.thunderbird.app.common.feature.mail.message

import com.fsck.k9.K9
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.Part
import com.fsck.k9.mailstore.DefaultAttachmentViewInfoMapper
import com.fsck.k9.ui.helper.SizeFormatter
import net.thunderbird.app.common.feature.mail.message.data.repository.DefaultMessageLifecycleRepository
import net.thunderbird.app.common.feature.mail.message.data.repository.DefaultMessageQueryRepository
import net.thunderbird.app.common.feature.mail.message.list.LegacyUpdateSortCriteria
import net.thunderbird.app.common.feature.mail.message.mapper.AttachmentResolver
import net.thunderbird.app.common.feature.mail.message.mapper.DefaultMessageDataMapper
import net.thunderbird.core.android.account.SortType
import net.thunderbird.core.architecture.model.LegacyEntityIdFactory
import net.thunderbird.core.architecture.model.LegacyEntityIdFactory.ByteRepresentation
import net.thunderbird.feature.mail.folder.FolderId
import net.thunderbird.feature.mail.folder.LegacyFolderIdFactory
import net.thunderbird.feature.mail.message.LegacyMessageIdFactory
import net.thunderbird.feature.mail.message.LegacyThreadIdFactory
import net.thunderbird.feature.mail.message.MessageId
import net.thunderbird.feature.mail.message.ThreadId
import net.thunderbird.feature.mail.message.domain.MessageLifecycleRepository
import net.thunderbird.feature.mail.message.domain.MessageQueryRepository
import net.thunderbird.feature.mail.message.export.DefaultMessageFileNameSuggester
import net.thunderbird.feature.mail.message.export.MessageExporter
import net.thunderbird.feature.mail.message.export.MessageFileNameSuggester
import net.thunderbird.feature.mail.message.export.eml.EmlMessageExporter
import net.thunderbird.feature.mail.message.list.domain.model.SortCriteria
import net.thunderbird.feature.mail.message.list.extension.toSortType
import net.thunderbird.feature.mail.message.mapper.MessageDataMapper
import net.thunderbird.feature.mail.message.reader.api.domain.mapper.AttachmentViewInfoMapper
import org.koin.android.ext.koin.androidApplication
import org.koin.core.qualifier.named
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

    single<AttachmentViewInfoMapper<Part>> {
        DefaultAttachmentViewInfoMapper(SizeFormatter(androidApplication().resources)::formatSize)
    }

    single<LegacyEntityIdFactory<MessageId>>(named(ByteRepresentation.MESSAGE)) { LegacyMessageIdFactory }
    single<LegacyEntityIdFactory<ThreadId>>(named(ByteRepresentation.THREADS)) { LegacyThreadIdFactory }
    single<LegacyEntityIdFactory<FolderId>>(named(ByteRepresentation.FOLDERS)) { LegacyFolderIdFactory }
    factory {
        AttachmentResolver(logger = get(), context = androidApplication())
    }

    single<MessageDataMapper<Message>> {
        DefaultMessageDataMapper(
            logger = get(),
            messageIdLegacyEntityIdFactory = get<LegacyEntityIdFactory<MessageId>>(named(ByteRepresentation.MESSAGE)),
            threadIdLegacyEntityIdFactory = get<LegacyEntityIdFactory<ThreadId>>(named(ByteRepresentation.THREADS)),
            folderIdLegacyEntityIdFactory = get<LegacyEntityIdFactory<FolderId>>(named(ByteRepresentation.FOLDERS)),
            attachmentResolver = get(),
            context = get(),
        )
    }

    single<MessageQueryRepository> {
        DefaultMessageQueryRepository(
            logger = get(),
            accountManager = get(),
            localStoreProvider = get(),
            messageIdLegacyEntityIdFactory = get<LegacyEntityIdFactory<MessageId>>(named(ByteRepresentation.MESSAGE)),
            folderIdLegacyEntityIdFactory = get<LegacyEntityIdFactory<FolderId>>(named(ByteRepresentation.FOLDERS)),
        )
    }

    single<MessageLifecycleRepository> {
        DefaultMessageLifecycleRepository(
            logger = get(),
            messageStoreManager = get(),
            saveMessageDataCreator = get(),
            messageMapper = get(),
            messageIdLegacyEntityIdFactory = get<LegacyEntityIdFactory<MessageId>>(named(ByteRepresentation.MESSAGE)),
            folderIdLegacyEntityIdFactory = get<LegacyEntityIdFactory<FolderId>>(named(ByteRepresentation.FOLDERS)),
            outboxFolderManager = get(),
        )
    }
}
