package com.fsck.k9.ui.messageview

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.fsck.k9.DI.get
import com.fsck.k9.activity.MessageList
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.ThemeManager

class MessageViewPagerFragment(private val messageList: MessageList) : Fragment() {
    private val themeManager = get(ThemeManager::class.java)
    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: MessageFragmentStateAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val messageViewThemeResourceId = themeManager.messageViewThemeResourceId
        val context: Context = ContextThemeWrapper(inflater.context, messageViewThemeResourceId)
        val layoutInflater = LayoutInflater.from(context)
        val view = layoutInflater.inflate(R.layout.message_viewpager, container, false)
        viewPager = view.findViewById(R.id.viewPager)
        viewPager.isUserInputEnabled = true
        viewPager.offscreenPageLimit = ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT
        adapter = MessageFragmentStateAdapter(this)
        viewPager.adapter = adapter
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(@ViewPager2.ScrollState state: Int) {
                doPageScrollStateChanged(state)
                super.onPageScrollStateChanged(state)
            }
        })
        return view
    }

    private fun doPageScrollStateChanged(@ViewPager2.ScrollState state: Int) {
        if (state == ViewPager2.SCROLL_STATE_IDLE) {
            messageList.configureMenu()
            adapter.resetWebView()
        }
    }

    val activeMessageViewFragment: MessageViewFragment?
        get() { return adapter.getActiveMessageViewFragment() }

    @SuppressLint("NotifyDataSetChanged")
    fun notifyDataSetChanged() = try {
        val reference = getMessageReference(viewPager.currentItem)
        adapter.clearFragmentCache()
        adapter.notifyDataSetChanged()
        showMessage(reference)
    } catch (e: UninitializedPropertyAccessException) {
        // swallow UninitializedPropertyAccessException
    }

    fun showMessage(messageReference: MessageReference?) {
        if (messageReference != null) {
            viewPager.setCurrentItem(getMessagePosition(messageReference), true)
        }
    }

    fun showPreviousMessage(): Boolean {
        val position = viewPager.currentItem - 1
        return if (position >= 0) {
            viewPager.setCurrentItem(position, true)
            true
        } else {
            false
        }
    }

    fun showNextMessage(): Boolean {
        val position = viewPager.currentItem + 1
        return if (position < adapter.itemCount) {
            viewPager.setCurrentItem(position, true)
            true
        } else {
            false
        }
    }

    fun getMessageCount(): Int {
        return try {
            messageList.getMessageCount()
        } catch (e: UninitializedPropertyAccessException) {
            0
        }
    }

    private fun getMessagePosition(reference: MessageReference): Int {
        return try {
            messageList.getMessagePosition(reference)
        } catch (e: UninitializedPropertyAccessException) {
            0
        }
    }

    fun getMessageReference(position: Int): MessageReference? {
        return try {
            messageList.getMessageReference(position)
        } catch (e: UninitializedPropertyAccessException) {
            null
        }
    }

    fun getActivePosition(): Int {
        return viewPager.currentItem
    }
}
