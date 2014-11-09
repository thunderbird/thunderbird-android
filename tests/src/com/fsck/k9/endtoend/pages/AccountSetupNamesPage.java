package com.fsck.k9.endtoend.pages;

import com.fsck.k9.R;
import com.google.android.apps.common.testing.ui.espresso.NoMatchingViewException;
import com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.clearText;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.scrollTo;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.typeText;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;


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
            // Ignored. Not the best way of doing this, but Espresso rightly makes
            // conditional flow difficult.
        }
    }
}
