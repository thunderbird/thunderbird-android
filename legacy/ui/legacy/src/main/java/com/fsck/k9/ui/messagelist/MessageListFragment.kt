package com.fsck.k9.ui.messagelist

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.fsck.k9.FontSizes
import kotlinx.coroutines.launch
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.message.list.extension.toDomainSortType
import net.thunderbird.feature.mail.message.list.extension.toSortType
import net.thunderbird.feature.mail.message.list.preferences.MessageListPreferences
import net.thunderbird.feature.mail.message.list.ui.MessageListContract
import net.thunderbird.feature.mail.message.list.ui.effect.MessageListEffect
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.SortType
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.feature.search.legacy.serialization.LocalMessageSearchSerializer
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parameterSetOf
import net.thunderbird.core.android.account.SortType as DomainSortType

private const val TAG = "MessageListFragment"

// TODO: Move this fragment to :feature:mail:message:list once all migration to the new
//       MessageListFragment to MVI is done.
class MessageListFragment : AbstractMessageListFragment() {
    override val logTag: String = TAG
    private val logger: Logger by inject()
    private val viewModel: MessageListContract.ViewModel by inject {
        decodeArguments()
        parameterSetOf(accountUuids.map { AccountIdFactory.of(it) }.toSet())
    }

    private val selectedSortType: SortType?
        get() {
            val key = if (isSingleAccountMode) account?.id else null
            val state = viewModel.state.value
            return state.selectedSortTypes[key]
        }

    override var sortType: DomainSortType
        get() = selectedSortType
            ?.toDomainSortType()
            ?.first
            .also {
                logger.debug(TAG) { "Selected sort type = $it" }
            }
            ?: DomainSortType.SORT_DATE
        set(value) {
            // We still allow the sortType to be set as we didn't migrate the message loading yet.
            // Once it is migrated, we should override the `changeSort(sortType: SortType)` and
            // `changeSort(sortType: SortType, sortAscending: Boolean?)` methods.
            // The next line will only update the value of the `selectedSortTypes` to reflect the new
            // selection.
            viewModel.event(
                event = MessageListEvent.ChangeSortType(
                    accountId = account?.id,
                    sortType = value.toSortType(
                        isAscending = account?.sortAscending[value] ?: value.isDefaultAscending,
                    ),
                ),
            )
        }

    override var sortAscending: Boolean
        get() = selectedSortType
            ?.toDomainSortType()
            ?.second
            .also {
                logger.debug(TAG) { "Selected sort type = $it" }
            } ?: false
        set(value) {
            val newSort = when (selectedSortType) {
                SortType.DateAsc if !value -> SortType.DateDesc
                SortType.DateDesc if value -> SortType.DateAsc
                SortType.ArrivalAsc if !value -> SortType.ArrivalDesc
                SortType.ArrivalDesc if value -> SortType.ArrivalAsc
                SortType.SenderAsc if !value -> SortType.SenderDesc
                SortType.SenderDesc if value -> SortType.SenderAsc
                SortType.UnreadAsc if !value -> SortType.UnreadDesc
                SortType.UnreadDesc if value -> SortType.UnreadAsc
                SortType.FlaggedAsc if !value -> SortType.FlaggedDesc
                SortType.FlaggedDesc if value -> SortType.FlaggedAsc
                SortType.AttachmentAsc if !value -> SortType.AttachmentDesc
                SortType.AttachmentDesc if value -> SortType.AttachmentAsc
                SortType.SubjectAsc if !value -> SortType.SubjectDesc
                SortType.SubjectDesc if value -> SortType.SubjectAsc
                else -> selectedSortType
            }
            newSort?.let {
                viewModel.event(event = MessageListEvent.ChangeSortType(accountId = account?.id, sortType = it))
            }
        }

    override val messageListAppearance: MessageListAppearance
        get() {
            return requireNotNull(viewModel.state.value.preferences)
                .also {
                    logger.verbose(TAG) { "messageListAppearance.get() called with preferences = $it" }
                }
                .toMessageListAppearance()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        // TODO(#10251): Required as the current implementation of sortType and sortAscending
                        //  returns null before we load the sort type. That should be removed when
                        //  the message list item's load is switched to the new state.
                        is MessageListEffect.RefreshMessageList -> loadMessageList()
                        else -> Unit
                    }
                }
            }
        }
    }

    override fun initializeSortSettings() {
        // The sort type settings is now loaded by the GetMessageListPreferencesUseCase.
        // Therefore, we override this method with an empty implementation, removing the
        // legacy implementation.
    }

    private fun MessageListPreferences.toMessageListAppearance(): MessageListAppearance = MessageListAppearance(
        fontSizes = FontSizes(),
        previewLines = excerptLines,
        stars = showFavouriteButton,
        senderAboveSubject = senderAboveSubject,
        showContactPicture = showMessageAvatar,
        showingThreadedList = groupConversations,
        backGroundAsReadIndicator = colorizeBackgroundWhenRead,
        showAccountIndicator = isShowAccountIndicator,
        density = density,
    )

    companion object Factory : AbstractMessageListFragment.Factory {
        override fun newInstance(
            search: LocalMessageSearch,
            isThreadDisplay: Boolean,
            threadedList: Boolean,
        ): MessageListFragment {
            val searchBytes = LocalMessageSearchSerializer.serialize(search)

            return MessageListFragment().apply {
                arguments = bundleOf(
                    ARG_SEARCH to searchBytes,
                    ARG_IS_THREAD_DISPLAY to isThreadDisplay,
                    ARG_THREADED_LIST to threadedList,
                )
            }
        }
    }
}
