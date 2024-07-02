package com.fsck.k9.helper

import java.util.concurrent.ThreadFactory

class NamedThreadFactory(private val threadNamePrefix: String) : ThreadFactory {
    var counter: Int = 0

    override fun newThread(runnable: Runnable) = Thread(runnable, "$threadNamePrefix-${counter++}")
}
