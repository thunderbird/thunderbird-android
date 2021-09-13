package com.fsck.k9.ui.messageview

import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.ui.messagelist.MessageListInfo
import com.fsck.k9.ui.messagelist.MessageListItem
import java.util.HashMap

class MessageFragmentStateAdapter(fragment: Fragment, liveData: LiveData<MessageListInfo>) :
    FragmentStateAdapter(fragment) {

    private val liveData: LiveData<MessageListInfo> = liveData
    private val fragmentMap: MutableMap<Int, MessageViewFragment> = HashMap()

    val messageList: List<MessageListItem>
        get() = liveData.value?.messageListItems ?: emptyList()

    private fun innerCreate(position: Int): MessageViewFragment {
        val reference = getMessageReference(position)
        val fragment = MessageViewFragment.newInstance(reference)
        fragmentMap[position] = fragment
        return fragment
    }

    private fun getMessageReference(position: Int): MessageReference {
        if (position < messageList.size && position >= 0) {
            val item = messageList[position]
            return MessageReference(item.account.uuid, item.folderId, item.messageUid, null)
        }
        return MessageReference("", 0, "", null)
    }

    override fun createFragment(position: Int): MessageViewFragment {
        return innerCreate(position)
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    fun getItem(position: Int): MessageListItem {
        return messageList[position]
    }

    fun getMessageViewFragment(position: Int): MessageViewFragment {
        return fragmentMap[position] ?: innerCreate(position)
    }

    fun getMessagePosition(ref: MessageReference): Int {
        // TODO: this could be too slow; so maybe use a HashMap instead ??
        for ((position, item) in messageList.withIndex()) {
            if (ref.folderId == item.folderId && ref.uid.equals(item.messageUid)
                && ref.accountUuid.equals(item.account.uuid)) {
                return position
            }
        }
        return 0
    }
}