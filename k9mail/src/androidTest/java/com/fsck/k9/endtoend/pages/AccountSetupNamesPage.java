package com.fsck.k9.endtoend.pages;


import com.fsck.k9.R;

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
        return new AccountsPage();
    }
}
