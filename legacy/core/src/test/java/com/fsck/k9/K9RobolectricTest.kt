package com.fsck.k9

import android.app.Application
import org.junit.runner.RunWith
import org.koin.test.AutoCloseKoinTest
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A Robolectric test that creates an instance of our [Application] test class [TestApp].
 *
 * See also [RobolectricTest].
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApp::class)
abstract class K9RobolectricTest : AutoCloseKoinTest()
