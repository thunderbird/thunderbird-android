package com.fsck.k9.ui.messagelist

import androidx.core.os.bundleOf
import com.fsck.k9.FontSizes
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.message.list.preferences.MessageListPreferences
import net.thunderbird.feature.mail.message.list.ui.MessageListContract
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.feature.search.legacy.serialization.LocalMessageSearchSerializer
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parameterSetOf

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

    override val messageListAppearance: MessageListAppearance
        get() {
            return requireNotNull(viewModel.state.value.preferences)
                .also {
                    logger.verbose(TAG) { "messageListAppearance.get() called with preferences = $it" }
                }
                .toMessageListAppearance()
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
