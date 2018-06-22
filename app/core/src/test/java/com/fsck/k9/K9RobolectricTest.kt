package com.fsck.k9

import android.app.Application
import org.junit.runner.RunWith
import org.koin.test.AutoCloseKoinTest
import org.robolectric.RobolectricTestRunner

/**
 * A Robolectric test that creates an instance of our [Application] class [K9].
 *
 * See also [RobolectricTest].
 */
@RunWith(RobolectricTestRunner::class)
abstract class K9RobolectricTest : AutoCloseKoinTest()
