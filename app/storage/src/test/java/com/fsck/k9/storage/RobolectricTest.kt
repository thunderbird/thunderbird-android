package com.fsck.k9.storage

import android.app.Application
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A Robolectric test that does not create an instance of our [Application] class.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = EmptyApplication::class)
abstract class RobolectricTest

class EmptyApplication : Application()
