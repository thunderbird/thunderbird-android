package com.fsck.k9.endtoend.pages;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.matcher.ViewMatchers;
import android.util.Log;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.matcher.ViewMatchers.withId;


public class AccountSetupNamesPage extends AbstractPage {

    public AccountSetupNamesPage inputAccountName(String name) {
        onView(withId(R.id.account_name))
                .perform(scrollTo())
                .perform(clearText())
                .perform(typeText(name));
        return this;
    }

    public AccountSetupNamesPage inputAccountDescription(String name) {
        onView(withId(R.id.account_description))
                .perform(scrollTo())
                .perform(clearText())
                .perform(typeText(name));
        return this;
    }

    public AccountsPage clickDone() {
        onView(withId(R.id.done))
                .perform(click());
        dismissChangelog();
        return new AccountsPage();
    }

    private void dismissChangelog() {
        try {
            onView(ViewMatchers.withText("OK")).perform(click());
        } catch (NoMatchingViewException ex) {
            Log.w(K9.LOG_TAG, "did not find Changelog OK button - ignored");
            // Ignored. Not the best way of doing this, but Espresso rightly makes
            // conditional flow difficult.
        }
    }
}
