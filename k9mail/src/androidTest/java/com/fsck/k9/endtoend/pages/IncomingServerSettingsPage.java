package com.fsck.k9.endtoend.pages;

import com.fsck.k9.R;
import com.fsck.k9.mail.ConnectionSecurity;

import static com.fsck.k9.activity.setup.ConnectionSecurityHolderMatcher.is;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onData;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.clearText;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.scrollTo;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.typeText;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isClickable;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;

public class IncomingServerSettingsPage extends AbstractPage {

    public IncomingServerSettingsPage inputImapServer(String imapServer) {
        onView(withId(R.id.account_server))
                .perform(scrollTo())
                .perform(clearText())
                .perform(typeText(imapServer));
        return this;
    }

    public IncomingServerSettingsPage inputImapSecurity(ConnectionSecurity security) {
        onView(withId(R.id.account_security_type))
                .perform(scrollTo())
                .perform(click());
        onData(is(security)).perform(click());
        return this;
    }

    public IncomingServerSettingsPage inputPort(int port) {
        onView(withId(R.id.account_port))
                .perform(scrollTo())
                .perform(clearText())
                .perform(typeText(String.valueOf(port)));
        return this;
    }


    public OutgoingServerSettingsPage clickNext() {
        onView(withId(R.id.next))
//                .perform(scrollTo())
                .check(matches(isClickable()))
                .perform(click());

        // We know this view is on the next page, this functions as a wait.
        onView(withText("SMTP server")).perform(scrollTo());
        return new OutgoingServerSettingsPage();
    }

    public IncomingServerSettingsPage inputUsername(String loginUsername) {
        onView(withId(R.id.account_username))
                .perform(scrollTo())
                .perform(clearText())
                .perform(typeText(loginUsername));
        return this;
    }

}
