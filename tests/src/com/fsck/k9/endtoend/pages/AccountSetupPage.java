package com.fsck.k9.endtoend.pages;

import com.fsck.k9.R;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.typeText;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;

public class AccountSetupPage extends AbstractPage {

    public AccountSetupPage inputEmailAddress(String emailAddress) {
        onView(withId(R.id.account_email)).perform(typeText(emailAddress));
        return this;
    }

    public AccountSetupPage inputPassword(String password) {
        onView(withId(R.id.account_password)).perform(typeText(password));
        return this;
    }

    public AccountTypePage clickManualSetup() {
        onView(withId(R.id.manual_setup)).perform(click());
        return new AccountTypePage();
    }

}
