package com.fsck.k9.ui.messageview

import android.content.Context
import com.fsck.k9.DI.get
import com.fsck.k9.ui.base.ThemeManager
import androidx.viewpager2.widget.ViewPager2
import com.fsck.k9.fragment.MessageListFragment
import com.fsck.k9.controller.MessageReference
import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.View
import androidx.fragment.app.Fragment
import com.fsck.k9.ui.R

class MessageViewPagerFragment : Fragment() {
    private val themeManager = get(ThemeManager::class.java)
    private var viewPager2: ViewPager2? = null
    private var adapter: MessageFragmentStateAdapter? = null
    private var messageListFragment: MessageListFragment? = null

    val currentMessageViewFragment: MessageViewFragment
        get() = adapter!!.getMessageViewFragment(viewPager2!!.currentItem)

    fun setActiveMessage(messageReference: MessageReference) {
        if (viewPager2 != null && adapter != null) {
            viewPager2!!.setCurrentItem(adapter!!.getMessagePosition(messageReference), true)
        }
    }

    fun showPreviousMessage(): Boolean {
        val prev = viewPager2!!.currentItem - 1
        return if (prev >= 0) {
            viewPager2!!.setCurrentItem(prev, true)
            true
        } else {
            false
        }
    }

    fun showNextMessage(): Boolean {
        val next = viewPager2!!.currentItem + 1
        return if (next < adapter?.itemCount ?: 0) {
            viewPager2!!.setCurrentItem(next, true)
            true
        } else {
            false
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val messageViewThemeResourceId = themeManager.messageViewThemeResourceId
        val context: Context = ContextThemeWrapper(inflater.context, messageViewThemeResourceId)
        val layoutInflater = LayoutInflater.from(context)
        val view = layoutInflater.inflate(R.layout.message_viewpager, container, false)
        viewPager2 = view.findViewById(R.id.viewPager)
        if (viewPager2 != null) {
            viewPager2!!.isUserInputEnabled = true
            viewPager2!!.offscreenPageLimit = ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT
        }
        initializeMessageListFragment()
        return view
    }

    fun initializeMessageListFragment() {
        messageListFragment =
            parentFragmentManager.findFragmentById(R.id.message_list_container) as MessageListFragment?
        if (viewPager2 != null && messageListFragment != null) {
            adapter = MessageFragmentStateAdapter(this, messageListFragment!!.getLiveData())
            viewPager2!!.adapter = adapter
        }
    }
}