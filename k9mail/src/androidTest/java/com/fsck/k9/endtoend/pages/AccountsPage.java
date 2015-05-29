package com.fsck.k9.endtoend.pages;

import com.fsck.k9.R;
import com.fsck.k9.endtoend.framework.AccountForTest;
import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.ViewAssertion;
import android.support.test.espresso.ViewInteraction;


import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

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

    public FolderPage clickOnAccount(AccountForTest accountForTest) {
        performOnAccountView(accountForTest, click());
        return new FolderPage();
    }

    public void clickLongOnAccount(AccountForTest accountForTest) {
        performOnAccountView(accountForTest, longClick());
    }

    private ViewInteraction performOnAccountView(AccountForTest accountForTest, ViewAction viewAction) {
        return onView(withText(accountForTest.description)).perform(viewAction);
    }

    public void clickRemoveInAccountMenu() {
        onView(withText("Remove account")).perform(click());
    }

    public void clickOK() {
        onView(withText("OK")).perform(click());
    }

}
