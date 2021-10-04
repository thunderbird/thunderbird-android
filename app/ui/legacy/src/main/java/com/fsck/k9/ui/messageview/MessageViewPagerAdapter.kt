package com.fsck.k9.ui.messageview

import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.fsck.k9.controller.MessageReference
import java.lang.ref.WeakReference

class MessageViewPagerAdapter(
    private val viewPagerFragment: MessageViewPagerFragment,
) : FragmentStateAdapter(viewPagerFragment) {

    /**
     *  This is a 'weak' cache of created fragments, so we can find them again if needed,
     *  but they will be disposed when the ViewPager releases them
     */
    private val fragmentCache = mutableMapOf<Long, WeakReference<MessageViewFragment>>()

    override fun createFragment(position: Int): MessageViewFragment {
        val reference = viewPagerFragment.getMessageReference(position)
        val fragment = MessageViewFragment.newInstance(reference)
        fragmentCache[getMessageUid(reference)] = WeakReference(fragment)
        return fragment
    }

    override fun getItemCount(): Int {
        return viewPagerFragment.getMessageCount()
    }

    override fun getItemId(position: Int): Long {
        return getMessageUid(viewPagerFragment.getMessageReference(position))
    }

    /**
     *  This lets the WebView capture swipe actions if it is scrollable
     */
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        recyclerView.addOnItemTouchListener(
            object : RecyclerView.SimpleOnItemTouchListener() {
                override fun onInterceptTouchEvent(view: RecyclerView, event: MotionEvent): Boolean {
                    viewPagerFragment.doInterceptTouchEvent(event)
                    return super.onInterceptTouchEvent(view, event)
                }
            }
        )
    }

    private fun getMessageUid(reference: MessageReference?): Long {
        return reference?.toIdentityString().hashCode().toLong()
    }

    fun getMessageViewFragment(reference: MessageReference?): MessageViewFragment? {
        return fragmentCache[getMessageUid(reference)]?.get()
    }
}
