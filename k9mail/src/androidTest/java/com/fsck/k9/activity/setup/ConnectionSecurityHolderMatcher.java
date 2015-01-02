package com.fsck.k9.activity.setup;


import com.fsck.k9.mail.ConnectionSecurity;
import android.support.test.espresso.matcher.BoundedMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import static com.android.support.test.deps.guava.base.Preconditions.checkNotNull;


public class ConnectionSecurityHolderMatcher {
    public static Matcher<Object> is(final ConnectionSecurity connectionSecurity) {
        checkNotNull(connectionSecurity);
        return new BoundedMatcher<Object, ConnectionSecurityHolder>(ConnectionSecurityHolder.class) {
            @Override
            public boolean matchesSafely(ConnectionSecurityHolder connectionSecurityHolder) {
                return connectionSecurityHolder.connectionSecurity == connectionSecurity;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("connection security is: ");
                description.appendText(connectionSecurity.name());
            }
        };
    }
}
