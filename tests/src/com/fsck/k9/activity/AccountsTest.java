package com.fsck.k9.activity;

import android.test.ActivityInstrumentationTestCase2;
import com.fsck.k9.activity.Accounts;
/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class com.fsck.k9.activity.AccountsTest \
 * com.fsck.k9.tests/android.test.InstrumentationTestRunner
 */
public class AccountsTest extends ActivityInstrumentationTestCase2<Accounts> {

    public AccountsTest() {
        super("com.fsck.k9", Accounts.class);
    }

}
