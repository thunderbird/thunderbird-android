package com.fsck.k9.storage

import android.app.Application
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.logging.testing.TestLogger
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A Robolectric test that does not create an instance of our [Application] class.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = EmptyApplication::class)
abstract class RobolectricTest

/**
 * Only used for Robolectric tests that do not require an instance of our [Application] class.
 *
 * This class sets up the static [Log.logger] to a [TestLogger] instance, allowing tests to log messages without
 * needing a full application context.
 */
class EmptyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Log.logger = logger
    }

    companion object {
        val logger: Logger = TestLogger()
    }
}
