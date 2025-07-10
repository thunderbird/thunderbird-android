package com.fsck.k9.ui.messageview

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import app.k9mail.legacy.message.controller.MessageReference
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.extensions.withArguments
import com.fsck.k9.ui.messagelist.MessageListItem
import com.fsck.k9.ui.messagelist.MessageListViewModel

/**
 * A fragment that uses [ViewPager2] to allow the user to swipe between messages.
 *
 * Individual messages are displayed using a [MessageViewFragment].
 */
class MessageViewContainerFragment : Fragment() {
    var isActive: Boolean = false
        set(value) {
            field = value
            setMenuVisibility(value)
        }

    private var showAccountChip: Boolean = true

    private var isUseLeftRightGestureNavigation: Boolean = true

    lateinit var messageReference: MessageReference
        private set

    private var activeMessageReference: MessageReference? = null

    private lateinit var fragmentListener: MessageViewContainerListener
    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: MessageViewContainerAdapter

    private val messageViewFragment: MessageViewFragment
        get() {
            check(isResumed)
            return findMessageViewFragment()
        }

    private fun findMessageViewFragment(): MessageViewFragment {
        val itemId = adapter.getItemId(messageReference)

        // ViewPager2/FragmentStateAdapter don't provide an easy way to get hold of the Fragment for the active
        // page. So we're using an implementation detail (the fragment tag) to find the fragment.
        return childFragmentManager.findFragmentByTag("f$itemId") as MessageViewFragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        messageReference = if (savedInstanceState == null) {
            MessageReference.parse(arguments?.getString(ARG_REFERENCE))
                ?: error("Missing argument $ARG_REFERENCE")
        } else {
            MessageReference.parse(savedInstanceState.getString(STATE_MESSAGE_REFERENCE))
                ?: error("Missing state $STATE_MESSAGE_REFERENCE")
        }

        showAccountChip = arguments?.getBoolean(ARG_SHOW_ACCOUNT_CHIP) ?: showAccountChip

        isUseLeftRightGestureNavigation =
            arguments?.getBoolean(ARG_IS_USE_LEFT_RIGHT_GESTURE_NAVIGATION) ?: isUseLeftRightGestureNavigation

        adapter = MessageViewContainerAdapter(this, showAccountChip)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        fragmentListener = try {
            context as MessageViewContainerListener
        } catch (e: ClassCastException) {
            throw ClassCastException("This fragment must be attached to a MessageViewContainerListener")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.message_view_container, container, false)

        val resources = inflater.context.resources
        val pageMargin = resources.getDimension(R.dimen.message_view_pager_page_margin).toInt()

        viewPager = view.findViewById(R.id.message_viewpager)
        viewPager.isUserInputEnabled = isUseLeftRightGestureNavigation
        viewPager.offscreenPageLimit = ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT
        viewPager.setPageTransformer(MarginPageTransformer(pageMargin))
        viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                // The message list is updated each time the active message is changed. To avoid message list updates
                // during the animation, we only set the active message after the animation has finished.
                override fun onPageScrollStateChanged(state: Int) {
                    if (state == ViewPager2.SCROLL_STATE_IDLE) {
                        setActiveMessage(viewPager.currentItem)
                    }
                }

                override fun onPageSelected(position: Int) {
                    if (viewPager.scrollState == ViewPager2.SCROLL_STATE_IDLE) {
                        setActiveMessage(position)
                    }
                }
            },
        )

        val recyclerView = viewPager.getChildAt(0) as? RecyclerView
        recyclerView?.reduceHorizontalDragSensitivity(
            viewPager = viewPager,
            threshold = VIEW_PAGER_SWIPE_THRESHOLD,
            velocityThreshold = VIEW_PAGER_SWIPE_VELOCITY_THRESHOLD,
        )

        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_MESSAGE_REFERENCE, messageReference.toIdentityString())
    }

    fun setViewModel(viewModel: MessageListViewModel) {
        viewModel.getMessageListLiveData().observe(this) { messageListInfo ->
            updateMessageList(messageListInfo.messageListItems)
        }
    }

    private fun updateMessageList(messageListItems: List<MessageListItem>) {
        if (messageListItems.isEmpty() || messageListItems.none { it.messageReference == messageReference }) {
            fragmentListener.closeMessageView()
            return
        }

        val oldPosition = viewPager.currentItem

        adapter.messageList = messageListItems

        // We only set the adapter on ViewPager2 after the message list has been loaded. This way ViewPager2 can
        // restore its saved state after a configuration change.
        if (viewPager.adapter == null) {
            viewPager.adapter = adapter
        }

        val position = adapter.getPosition(messageReference)
        if (position != oldPosition) {
            // The current message now has a different position in the updated list. So point the view pager to it.
            // Note: This cancels ongoing swipe actions/animations. We might want to change this to defer list updates
            // until after swipe actions have been completed.
            viewPager.setCurrentItem(position, false)
        }
    }

    private fun setActiveMessage(position: Int) {
        val newMessageReference = adapter.getMessageReference(position) ?: return
        if (newMessageReference == activeMessageReference) {
            // If the position of current message changes (e.g. because messages were added or removed from the list),
            // we ignore the event.
            return
        }

        messageReference = newMessageReference
        activeMessageReference = newMessageReference
        fragmentListener.setActiveMessage(newMessageReference)
    }

    fun showPreviousMessage(): Boolean {
        val newPosition = viewPager.currentItem - 1
        return if (newPosition >= 0) {
            setActiveMessage(newPosition)
            viewPager.setCurrentItem(newPosition, false)
            true
        } else {
            false
        }
    }

    fun showNextMessage(): Boolean {
        val newPosition = viewPager.currentItem + 1
        return if (newPosition < adapter.itemCount) {
            setActiveMessage(newPosition)
            viewPager.setCurrentItem(newPosition, false)
            true
        } else {
            false
        }
    }

    fun onToggleFlagged() {
        messageViewFragment.onToggleFlagged()
    }

    fun onMove() {
        messageViewFragment.onMove()
    }

    fun onArchive() {
        messageViewFragment.onArchive()
    }

    fun onCopy() {
        messageViewFragment.onCopy()
    }

    fun onToggleRead() {
        messageViewFragment.onToggleRead()
    }

    fun onForward() {
        messageViewFragment.onForward()
    }

    fun onReplyAll() {
        messageViewFragment.onReplyAll()
    }

    fun onReply() {
        messageViewFragment.onReply()
    }

    fun onDelete() {
        messageViewFragment.onDelete()
    }

    private class MessageViewContainerAdapter(
        fragment: Fragment,
        private val showAccountChip: Boolean,
    ) : FragmentStateAdapter(fragment) {

        var messageList: List<MessageListItem> = emptyList()
            set(value) {
                val diffResult = DiffUtil.calculateDiff(
                    MessageListDiffCallback(oldMessageList = messageList, newMessageList = value),
                )

                field = value

                diffResult.dispatchUpdatesTo(this)
            }

        override fun getItemCount(): Int {
            return messageList.size
        }

        override fun getItemId(position: Int): Long {
            return messageList[position].uniqueId
        }

        override fun containsItem(itemId: Long): Boolean {
            return messageList.any { it.uniqueId == itemId }
        }

        override fun createFragment(position: Int): Fragment {
            check(position in messageList.indices)

            val messageReference = messageList[position].messageReference
            return MessageViewFragment.newInstance(messageReference, showAccountChip)
        }

        fun getMessageReference(position: Int): MessageReference? {
            return messageList.getOrNull(position)?.messageReference
        }

        fun getPosition(messageReference: MessageReference): Int {
            return messageList.indexOfFirst { it.messageReference == messageReference }
        }

        fun getItemId(messageReference: MessageReference): Long {
            return messageList.first { it.messageReference == messageReference }.uniqueId
        }
    }

    private class MessageListDiffCallback(
        private val oldMessageList: List<MessageListItem>,
        private val newMessageList: List<MessageListItem>,
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldMessageList.size

        override fun getNewListSize(): Int = newMessageList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldMessageList[oldItemPosition].uniqueId == newMessageList[newItemPosition].uniqueId
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            // Let MessageViewFragment deal with content changes
            return areItemsTheSame(oldItemPosition, newItemPosition)
        }
    }

    interface MessageViewContainerListener {
        fun closeMessageView()
        fun setActiveMessage(messageReference: MessageReference)
    }

    companion object {
        private const val VIEW_PAGER_SWIPE_THRESHOLD = 0.4f
        private const val VIEW_PAGER_SWIPE_VELOCITY_THRESHOLD = 0.8f

        private const val ARG_REFERENCE = "reference"
        private const val ARG_SHOW_ACCOUNT_CHIP = "showAccountChip"

        private const val STATE_MESSAGE_REFERENCE = "messageReference"

        private const val ARG_IS_USE_LEFT_RIGHT_GESTURE_NAVIGATION = "isUseLeftRightGestureNavigation"

        fun newInstance(
            reference: MessageReference,
            showAccountChip: Boolean,
            isUseLeftRightGestureNavigation: Boolean,
        ): MessageViewContainerFragment {
            return MessageViewContainerFragment().withArguments(
                ARG_REFERENCE to reference.toIdentityString(),
                ARG_SHOW_ACCOUNT_CHIP to showAccountChip,
                ARG_IS_USE_LEFT_RIGHT_GESTURE_NAVIGATION to isUseLeftRightGestureNavigation,
            )
        }
    }
}

/**
 * Reduces the sensitivity of horizontal drag gestures.
 *
 * This is a workaround for the issue where ViewPager2 triggers a swipe when the user is scrolling vertically in
 * a WebView. By increasing the swipe threshold and swipe velocity threshold, we can make it harder for the
 * ViewPager2 to trigger a swipe.
 *
 * Note: This is a hack that relies on the internal implementation of ViewPager2, which uses a RecyclerView.
 * This might break in future versions of ViewPager2.
 *
 * @see <a href="https://issuetracker.google.com/issues/123006042">Issue 123006042</a>
 */
private fun RecyclerView.reduceHorizontalDragSensitivity(
    viewPager: ViewPager2,
    threshold: Float,
    velocityThreshold: Float,
) {
    val itemTouchHelper = ItemTouchHelper(
        object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder,
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                when (direction) {
                    ItemTouchHelper.LEFT -> viewPager.currentItem = viewPager.currentItem + 1
                    ItemTouchHelper.RIGHT -> viewPager.currentItem = viewPager.currentItem - 1
                }
            }

            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
                return threshold
            }

            override fun getSwipeVelocityThreshold(defaultValue: Float): Float {
                return velocityThreshold
            }
        },
    )
    itemTouchHelper.attachToRecyclerView(this)
}
