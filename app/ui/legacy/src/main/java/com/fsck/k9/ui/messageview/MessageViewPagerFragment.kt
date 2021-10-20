package com.fsck.k9.ui.messageview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color.DKGRAY
import android.graphics.Rect
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.EdgeEffect
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.fsck.k9.DI.get
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.ThemeManager
import kotlin.math.absoluteValue
import kotlin.math.sign

class MessageViewPagerFragment : Fragment() {
    private val themeManager = get(ThemeManager::class.java)
    private lateinit var fragmentListener: MessageViewPagerFragmentListener
    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: MessageViewPagerAdapter
    private var targetMessage: MessageReference? = null
    private var showedMessage: MessageReference? = null

    companion object {
        private const val ARG_ACTIVE_MESSAGE = "activeMessage"
        private const val FLING_THRESHOLD = 5000 // pixels per second

        fun newInstance(reference: MessageReference): MessageViewPagerFragment {
            val fragment = MessageViewPagerFragment()
            val args = Bundle()
            args.putString(ARG_ACTIVE_MESSAGE, reference.toIdentityString())
            fragment.arguments = args
            return fragment
        }
    }

    /**
     *  Modify the over scroll edge effect colour from white (which one cannot see) to dark gray
     */
    val edgeEffectFactory: RecyclerView.EdgeEffectFactory = object : RecyclerView.EdgeEffectFactory() {
        override fun createEdgeEffect(view: RecyclerView, direction: Int): EdgeEffect {
            return EdgeEffect(view.context).apply { color = DKGRAY }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        fragmentListener = try {
            context as MessageViewPagerFragmentListener
        } catch (e: ClassCastException) {
            error("${context.javaClass} must implement MessageViewPagerFragmentListener")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val arguments = arguments
        if (arguments != null) {
            val reference = arguments.getString(ARG_ACTIVE_MESSAGE)
            if (reference != null && reference.isNotEmpty()) {
                targetMessage = MessageReference.parse(reference)
                return
            }
        }
        targetMessage = null
        return
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val messageViewThemeResourceId = themeManager.messageViewThemeResourceId
        val context: Context = ContextThemeWrapper(inflater.context, messageViewThemeResourceId)
        val layoutInflater = LayoutInflater.from(context)
        val view = layoutInflater.inflate(R.layout.message_viewpager, container, false)
        viewPager = view.findViewById(R.id.viewPager)
        viewPager.isUserInputEnabled = true
        viewPager.offscreenPageLimit = ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT
        adapter = MessageViewPagerAdapter(this)
        viewPager.adapter = adapter
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(@ViewPager2.ScrollState state: Int) {
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    viewPagerSettled()
                }
                super.onPageScrollStateChanged(state)
            }
        })
        viewPager.addItemDecoration(DividerItemDecoration(this.context, DividerItemDecoration.HORIZONTAL))
        viewPager.post { tryShowTargetMessage() }
        return view
    }

    override fun onDestroy() {
        val arguments = arguments
        if (arguments != null) {
            var referenceId: String? = null
            val reference = getMessageReference(viewPager.currentItem)
            if (reference != null) {
                referenceId = reference.toIdentityString()
            }
            arguments.putString(ARG_ACTIVE_MESSAGE, referenceId)
        }
        super.onDestroy()
    }

    private fun viewPagerSettled() {
        viewPager.post {
            fragmentListener.configureMenu()
            webView = null
            val reference = getMessageReference(viewPager.currentItem)
            if (reference != null) {
                fragmentListener.scrollToMessage(reference)
                showedMessage = reference
            }
            activeMessageViewFragment?.setMessageViewed()
        }
    }

    val activeMessageViewFragment: MessageViewFragment?
        get() {
            return try {
                val reference = getMessageReference(viewPager.currentItem)
                adapter.getMessageViewFragment(reference)
            } catch (e: Exception) {
                null
            }
        }

    @SuppressLint("NotifyDataSetChanged")
    fun onMessageListDirty() {
        if (targetMessage == null) {
            targetMessage = showedMessage
        }
        adapter.notifyDataSetChanged()
        tryShowTargetMessage()
    }

    private fun tryShowTargetMessage() {
        if (targetMessage != null) {
            val position = getMessagePosition(targetMessage!!)
            if (0 <= position) {
                viewPager.setCurrentItem(position, false)
                viewPagerSettled()
                targetMessage = null
            }
        }
    }

    fun showPreviousMessage(): Boolean {
        val position = viewPager.currentItem - 1
        return if (0 <= position) {
            viewPager.setCurrentItem(position, true)
            true
        } else {
            false
        }
    }

    fun showNextMessage(): Boolean {
        val position = viewPager.currentItem + 1
        return if (position < getMessageCount()) {
            viewPager.setCurrentItem(position, true)
            true
        } else {
            false
        }
    }

    fun getMessageCount(): Int {
        return try {
            fragmentListener.getMessageCount()
        } catch (e: Exception) {
            0
        }
    }

    private fun getMessagePosition(reference: MessageReference): Int {
        return try {
            fragmentListener.getMessagePosition(reference)
        } catch (e: Exception) {
            -1
        }
    }

    fun getMessageReference(position: Int): MessageReference? {
        return try {
            fragmentListener.getMessageReference(position)
        } catch (e: Exception) {
            null
        }
    }

    private var webView: WebView? = null
        get() {
            if (field == null) {
                val reference = getMessageReference(viewPager.currentItem)
                val view: View? = adapter.getMessageViewFragment(reference)?.view?.findViewById(R.id.message_content)
                field = if (view is WebView) view else null
            }
            return field
        }

    /**
     *  WebView extension function to scroll the WebView by means of API calls
     */
    private fun WebView.externalScrollBy(scrollX: Float) {
        val newScrollX = startScrollX + scrollX
        if (newScrollX < this.scrollX) {
            // scroll left
            this.scrollTo(newScrollX.coerceAtLeast(0f).toInt(), this.scrollY)
        } else {
            // scroll right; coerceAtMost() not possible because right scroll limit not exposed by API
            while (this.canScrollHorizontally(1) && (newScrollX > this.scrollX)) {
                this.scrollBy(5, 0)
            }
        }
    }

    private var startEvent: MotionEvent? = null
    private var startScrollX = 0
    private var flingTracker: VelocityTracker? = null

    @SuppressLint("Recycle")
    private fun obtainScrollParameters(event: MotionEvent) {
        startEvent = MotionEvent.obtainNoHistory(event)
        startScrollX = webView!!.scrollX
        flingTracker?.clear()
        flingTracker = flingTracker ?: VelocityTracker.obtain()
        flingTracker?.addMovement(event)
    }

    private fun recycleScrollParameters() {
        startEvent?.recycle()
        flingTracker?.recycle()
        startEvent = null
        flingTracker = null
    }

    private fun getScrollFlingXVelocity(event: MotionEvent): Float {
        flingTracker?.apply {
            addMovement(event)
            computeCurrentVelocity(1000)
            return -getXVelocity(0)
        }
        return 0f
    }

    /**
     * Let the WebView capture left/right swipe actions when it is scrollable
     */
    fun onInterceptTouchEvent(recyclerView: RecyclerView, thisEvent: MotionEvent): Boolean {
        if (webView != null) {
            when (thisEvent.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    val webViewRect = Rect()
                    val webViewOrigin = IntArray(2)
                    webView!!.getLocationOnScreen(webViewOrigin)
                    webView!!.getHitRect(webViewRect)
                    webViewRect.offset(webViewOrigin[0], webViewOrigin[1])
                    if (webViewRect.contains(thisEvent.rawX.toInt(), thisEvent.rawY.toInt()) &&
                        (webView!!.canScrollHorizontally(-1) || webView!!.canScrollHorizontally(1))
                    ) {
                        obtainScrollParameters(thisEvent)
                    } else {
                        recycleScrollParameters()
                    }
                }
                MotionEvent.ACTION_CANCEL,
                MotionEvent.ACTION_UP -> {
                    webView!!.parent?.requestDisallowInterceptTouchEvent(false)
                    recycleScrollParameters()
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    webView!!.parent?.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (startEvent != null) {
                        val vX = getScrollFlingXVelocity(thisEvent).toInt()
                        if ((vX.absoluteValue > FLING_THRESHOLD) && (webView!!.canScrollHorizontally(vX.sign))) {
                            webView!!.flingScroll(vX, 0)
                            viewPager.setCurrentItem(viewPager.currentItem, false)
                        } else {
                            val dX = startEvent!!.x - thisEvent.x
                            val dY = startEvent!!.y - thisEvent.y
                            val slop = ViewConfiguration.get(webView!!.context).scaledTouchSlop
                            if ((dX.absoluteValue > slop) && (dX.absoluteValue > dY.absoluteValue)) {
                                if (webView!!.canScrollHorizontally(dX.sign.toInt())) {
                                    webView!!.parent?.requestDisallowInterceptTouchEvent(true)
                                    /**
                                     * There exist special edge cases where the ViewPager has already started
                                     * to scroll before requestDisallowInterceptTouchEvent(true) gets called,
                                     * i.e. the ViewPager already started intercepting the gesture, and in those
                                     * cases the ViewPager does NOT honour the request to disallow intercepts; so
                                     * we call WebView.extendedScrollToX() to handle the scrolling instead.
                                     */
                                    if (recyclerView.scrollState != RecyclerView.SCROLL_STATE_IDLE) {
                                        webView!!.externalScrollBy(dX)
                                        viewPager.setCurrentItem(viewPager.currentItem, false)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return false
    }
}

interface MessageViewPagerFragmentListener {
    fun getMessageCount(): Int
    fun getMessagePosition(reference: MessageReference): Int
    fun getMessageReference(position: Int): MessageReference
    fun configureMenu()
    fun scrollToMessage(reference: MessageReference)
}
