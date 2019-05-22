package com.fsck.k9.activity

import java.util.Locale

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.text.TextUtils
import android.view.GestureDetector
import android.view.MotionEvent

import com.fsck.k9.K9
import com.fsck.k9.K9.Theme
import com.fsck.k9.activity.misc.SwipeGestureDetector
import com.fsck.k9.activity.misc.SwipeGestureDetector.OnSwipeGestureListener
import com.fsck.k9.ui.R


/**
 * This class implements functionality common to most activities used in K-9 Mail.
 *
 * @see K9Activity
 * @see K9ListActivity
 */
class K9ActivityCommon(private val activity: Activity) {
    private var gestureDetector: GestureDetector? = null

    /**
     * Call this before calling `super.onCreate(Bundle)`.
     */
    fun preOnCreate() {
        setLanguage(activity, K9.k9Language)
        activity.setTheme(k9ThemeResourceId)
    }

    /**
     * Call this before calling `super.dispatchTouchEvent(MotionEvent)`.
     */
    fun preDispatchTouchEvent(event: MotionEvent) {
        gestureDetector?.onTouchEvent(event)
    }

    /**
     * Call this if you wish to use the swipe gesture detector.
     *
     * @param listener A listener that will be notified if a left to right or right to left swipe has been detected.
     */
    fun setupGestureDetector(listener: OnSwipeGestureListener) {
        gestureDetector = GestureDetector(activity, SwipeGestureDetector(activity, listener))
    }


    companion object {
        @JvmStatic
        fun setLanguage(context: Context, language: String) {
            val locale = if (TextUtils.isEmpty(language)) {
                Resources.getSystem().configuration.locale
            } else if (language.length == 5 && language[2] == '_') {
                // language is in the form: en_US
                Locale(language.substring(0, 2), language.substring(3))
            } else {
                Locale(language)
            }

            val resources = context.resources
            val config = resources.configuration
            config.locale = locale
            resources.updateConfiguration(config, resources.displayMetrics)
        }

        @JvmStatic
        fun getK9ThemeResourceId(themeId: Theme): Int {
            return if (themeId === Theme.LIGHT) R.style.Theme_K9_Light else R.style.Theme_K9_Dark
        }

        @JvmStatic
        val k9ActionBarThemeResourceId: Int
            get() = if (k9ThemeResourceId == R.style.Theme_K9_Light)
                R.style.Theme_K9_Light_ActionBar
            else
                R.style.Theme_K9_Dark_ActionBar

        @JvmStatic
        val k9ThemeResourceId: Int
            get() = getK9ThemeResourceId(K9.k9Theme)
    }

    /**
     * Base activities need to implement this interface.
     *
     * The implementing class simply has to call through to the implementation of these methods
     * in [K9ActivityCommon].
     */
    interface K9ActivityMagic {
        fun setupGestureDetector(listener: OnSwipeGestureListener)
    }
}
