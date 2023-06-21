package app.k9mail.core.android.testing

import android.app.Application
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A Robolectric test that does not create an instance of our [Application].
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = EmptyApplication::class)
abstract class RobolectricTest

class EmptyApplication : Application()
