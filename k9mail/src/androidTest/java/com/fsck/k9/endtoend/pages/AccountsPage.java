package com.fsck.k9.endtoend.pages;

import com.fsck.k9.R;
import com.fsck.k9.endtoend.framework.AccountForTest;
import com.google.android.apps.common.testing.ui.espresso.NoMatchingViewException;
import com.google.android.apps.common.testing.ui.espresso.ViewAssertion;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.longClick;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.scrollTo;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.doesNotExist;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;

public class AccountsPage extends AbstractPage {

    private void assertAccount(String accountDisplayName, boolean exists) {
        ViewAssertion assertion = exists ? matches(isDisplayed()) : doesNotExist();
        onView(withText(accountDisplayName)).check(assertion);
    }

    public AccountSetupPage clickAddNewAccount() {
        // need to click twice for some reason?
        onView(withId(R.id.add_new_account)).perform(click());
        try {
            onView(withId(R.id.add_new_account)).perform(click());
        } catch (NoMatchingViewException ex) {
            // Ignore
        }
        onView(withId(R.id.account_email)).perform(scrollTo());
        return new AccountSetupPage();
    }

    public void assertAccountExists(String accountDisplayName) {
        assertAccount(accountDisplayName, true);
    }

    public void assertAccountDoesNotExist(String accountDisplayName) {
        assertAccount(accountDisplayName, false);
    }

    public void clickLongOnAccount(AccountForTest accountForTest) {
        onView(withText(accountForTest.description)).perform(longClick());
    }

    public void clickRemoveInAccountMenu() {
        onView(withText("Remove account")).perform(click());
    }

    public void clickOK() {
        onView(withText("OK")).perform(click());
    }
}
