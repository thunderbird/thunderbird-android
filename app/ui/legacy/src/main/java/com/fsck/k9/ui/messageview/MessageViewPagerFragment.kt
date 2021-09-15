package com.fsck.k9.ui.messageview

import android.content.Context
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.fsck.k9.DI.get
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.fragment.MessageListFragment
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.ThemeManager

class MessageViewPagerFragment : Fragment() {
    private val themeManager = get(ThemeManager::class.java)
    private var viewPager: ViewPager2? = null
    private var adapter: MessageFragmentStateAdapter? = null
    private var messageListFragment: MessageListFragment? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val messageViewThemeResourceId = themeManager.messageViewThemeResourceId
        val context: Context = ContextThemeWrapper(inflater.context, messageViewThemeResourceId)
        val layoutInflater = LayoutInflater.from(context)
        val view = layoutInflater.inflate(R.layout.message_viewpager, container, false)
        viewPager = view.findViewById(R.id.viewPager)
        if (viewPager != null) {
            viewPager!!.isUserInputEnabled = true
            viewPager!!.offscreenPageLimit = ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT
        }
        return view
    }

    override fun onResume() {
        super.onResume()
        resetMessageListFragment()
    }

    val selectedMessageViewFragment: MessageViewFragment?
        get() { return adapter!!.getCurrentMessageViewFragment() }

    fun resetMessageListFragment() {
        val newMessageListFragment =
            parentFragmentManager.findFragmentById(R.id.message_list_container) as MessageListFragment?
        if (newMessageListFragment != messageListFragment) {
            messageListFragment = newMessageListFragment
            if (viewPager != null && messageListFragment != null) {
                adapter = MessageFragmentStateAdapter(this, messageListFragment!!, viewPager!!)
            }
        }
    }

    fun setActiveMessage(messageReference: MessageReference) {
        if (viewPager != null && adapter != null) {
            viewPager!!.setCurrentItem(adapter!!.getMessagePosition(messageReference), true)
        }
    }

    fun showPreviousMessage(): Boolean {
        val position = viewPager!!.currentItem - 1
        return if (position >= 0) {
            viewPager!!.setCurrentItem(position, true)
            true
        } else {
            false
        }
    }

    fun showNextMessage(): Boolean {
        val position = viewPager!!.currentItem + 1
        return if (position < adapter!!.itemCount) {
            viewPager!!.setCurrentItem(position, true)
            true
        } else {
            false
        }
    }
}
