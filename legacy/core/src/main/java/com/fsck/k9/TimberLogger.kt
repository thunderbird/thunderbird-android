package com.fsck.k9

import android.os.Build
import com.fsck.k9.logging.Logger
import java.util.regex.Pattern
import timber.log.Timber

class TimberLogger : Logger {
    override fun v(message: String?, vararg args: Any?) {
        setTimberTag()
        Timber.v(message, *args)
    }

    override fun v(t: Throwable?, message: String?, vararg args: Any?) {
        setTimberTag()
        Timber.v(t, message, *args)
    }

    override fun v(t: Throwable?) {
        setTimberTag()
        Timber.v(t)
    }

    override fun d(message: String?, vararg args: Any?) {
        setTimberTag()
        Timber.d(message, *args)
    }

    override fun d(t: Throwable?, message: String?, vararg args: Any?) {
        setTimberTag()
        Timber.d(t, message, *args)
    }

    override fun d(t: Throwable?) {
        setTimberTag()
        Timber.d(t)
    }

    override fun i(message: String?, vararg args: Any?) {
        setTimberTag()
        Timber.i(message, *args)
    }

    override fun i(t: Throwable?, message: String?, vararg args: Any?) {
        setTimberTag()
        Timber.i(t, message, *args)
    }

    override fun i(t: Throwable?) {
        setTimberTag()
        Timber.i(t)
    }

    override fun w(message: String?, vararg args: Any?) {
        setTimberTag()
        Timber.w(message, *args)
    }

    override fun w(t: Throwable?, message: String?, vararg args: Any?) {
        setTimberTag()
        Timber.w(t, message, *args)
    }

    override fun w(t: Throwable?) {
        setTimberTag()
        Timber.w(t)
    }

    override fun e(message: String?, vararg args: Any?) {
        setTimberTag()
        Timber.e(message, *args)
    }

    override fun e(t: Throwable?, message: String?, vararg args: Any?) {
        setTimberTag()
        Timber.e(t, message, *args)
    }

    override fun e(t: Throwable?) {
        setTimberTag()
        Timber.e(t)
    }

    private fun setTimberTag() {
        val tag = Throwable().stackTrace
            .first { it.className !in IGNORE_CLASSES }
            .let(::createStackElementTag)

        // We explicitly set a tag, otherwise Timber will always derive the tag "TimberLogger".
        Timber.tag(tag)
    }

    private fun createStackElementTag(element: StackTraceElement): String {
        var tag = element.className.substringAfterLast('.')
        val matcher = ANONYMOUS_CLASS.matcher(tag)
        if (matcher.find()) {
            tag = matcher.replaceAll("")
        }

        // Tag length limit was removed in API 26.
        return if (tag.length <= MAX_TAG_LENGTH || Build.VERSION.SDK_INT >= 26) {
            tag
        } else {
            tag.substring(0, MAX_TAG_LENGTH)
        }
    }

    companion object {
        private const val MAX_TAG_LENGTH = 23
        private val ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$")

        private val IGNORE_CLASSES = setOf(
            Timber::class.java.name,
            Timber.Forest::class.java.name,
            Timber.Tree::class.java.name,
            Timber.DebugTree::class.java.name,
            TimberLogger::class.java.name,
            com.fsck.k9.logging.Timber::class.java.name,
        )
    }
}
