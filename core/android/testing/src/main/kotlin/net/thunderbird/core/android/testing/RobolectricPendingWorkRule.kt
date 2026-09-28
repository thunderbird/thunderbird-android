package net.thunderbird.core.android.testing

import android.os.Looper
import assertk.assertThat
import assertk.assertions.isTrue
import org.junit.rules.ExternalResource
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.util.concurrent.PausedExecutorService
import org.robolectric.shadows.ShadowPausedAsyncTask

/**
 * Controls when work queued during a Robolectric test actually runs.
 *
 * Robolectric runs in `PAUSED` looper mode, where nothing is delivered on its own: an
 * [android.os.AsyncTask]'s `doInBackground()` only runs when the executor is drained, and anything
 * posted to the main looper, such as `onPostExecute()` or a service connection callback, only runs
 * once that looper is idled. Tests therefore have to say when the queued work runs, using
 * [runNextTask], [runAllTasks] or [idleMainLooper].
 *
 * This replaces `Robolectric.getBackgroundThreadScheduler()`, which required the LEGACY looper mode
 * that Robolectric no longer supports above SDK 36.
 *
 * Add it to a test alongside whichever Robolectric base class the test already uses:
 * ```
 * class SomeTest : K9RobolectricTest() {
 *     @get:Rule
 *     val pendingWork = RobolectricPendingWorkRule()
 * }
 * ```
 */
@Suppress("UnstableApiUsage")
class RobolectricPendingWorkRule : ExternalResource() {
    private val backgroundExecutor = PausedExecutorService()

    override fun before() {
        ShadowPausedAsyncTask.overrideExecutor(backgroundExecutor)
    }

    override fun after() {
        ShadowPausedAsyncTask.reset()
    }

    /**
     * Runs the next queued [android.os.AsyncTask] and delivers its result, failing if none is queued.
     *
     * Use this when the test needs to control tasks one at a time; otherwise use [runAllTasks].
     */
    fun runNextTask() {
        assertThat(backgroundExecutor.runNext()).isTrue()
        idleMainLooper()
    }

    /**
     * Runs every queued [android.os.AsyncTask] and delivers their results.
     *
     * Unlike [runNextTask] this does not require a task to be queued, so it is safe to call after an
     * operation that may or may not have started one.
     */
    fun runAllTasks() {
        backgroundExecutor.runAll()
        idleMainLooper()
    }

    /**
     * Runs everything currently queued on the main looper.
     *
     * Use this for asynchronous work that does not go through an [android.os.AsyncTask], such as
     * service connection callbacks, which Robolectric delivers via the main looper.
     */
    fun idleMainLooper() {
        shadowOf(Looper.getMainLooper()).idle()
    }
}
