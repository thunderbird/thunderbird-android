package com.fsck.k9.endtoend.pages;

import com.fsck.k9.R;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;


public class AccountOptionsPage extends AbstractPage {

    public AccountSetupNamesPage clickNext() {
        onView(withId(R.id.next)).perform(click());
        return new AccountSetupNamesPage();
    }

}
