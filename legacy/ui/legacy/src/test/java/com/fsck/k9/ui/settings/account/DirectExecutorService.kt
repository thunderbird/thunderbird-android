package com.fsck.k9.ui.settings.account

import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.TimeUnit

/** Runs every task immediately on the calling thread. */
internal class DirectExecutorService : AbstractExecutorService() {
    private var isShutdown = false

    override fun execute(command: Runnable) = command.run()

    override fun shutdown() {
        isShutdown = true
    }

    override fun shutdownNow(): List<Runnable> = emptyList<Runnable>().also { shutdown() }

    override fun isShutdown(): Boolean = isShutdown

    override fun isTerminated(): Boolean = isShutdown

    override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean = true
}
