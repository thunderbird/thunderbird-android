package com.fsck.k9.endtoend.pages;

import com.fsck.k9.R;
import com.fsck.k9.mail.ConnectionSecurity;

import static com.fsck.k9.activity.setup.ConnectionSecurityHolderMatcher.is;
import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.matcher.ViewMatchers.withId;


public class OutgoingServerSettingsPage extends AbstractPage {

    public OutgoingServerSettingsPage inputSmtpServer(String serverAddress) {
        onView(withId(R.id.account_server))
                .perform(scrollTo())
                .perform(clearText())
                .perform(typeText(serverAddress));
        return this;

    }

    public OutgoingServerSettingsPage inputSmtpSecurity(ConnectionSecurity security) {
        onView(withId(R.id.account_security_type))
                .perform(scrollTo())
                .perform(click());
        onData(is(security)).perform(click());
        return this;
    }

    public OutgoingServerSettingsPage inputPort(int port) {
        onView(withId(R.id.account_port))
                .perform(scrollTo())
                .perform(clearText())
                .perform(typeText(String.valueOf(port)));
        return this;
    }

    public OutgoingServerSettingsPage inputRequireSignIn(boolean requireSignInInput) {
        onView(withId(R.id.account_require_login))
                .perform(scrollTo());
        /*
         * Make this smarter; click if necessary.
         */
        if (!requireSignInInput) {
            onView(withId(R.id.account_require_login))
                    .perform(click());
        }
//        Matcher<View> checkedOrNot = requireSignInInput ? isChecked(): isNotChecked();
//        try {
//            onView(withId(R.id.account_require_login)).check((matches(checkedOrNot)));
//        } catch (AssertionFailedWithCauseError ex) {
//            onView(withId(R.id.account_require_login)).perform(click());
//        }
        return this;
    }

    public AccountOptionsPage clickNext() {
        onView(withId(R.id.next)).perform(click());
        return new AccountOptionsPage();
    }

}
