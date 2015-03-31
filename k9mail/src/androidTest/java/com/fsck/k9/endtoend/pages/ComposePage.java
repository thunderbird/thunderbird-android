package com.fsck.k9.endtoend.pages;

import com.fsck.k9.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

public class ComposePage extends AbstractPage {

    public void inputTo(String toAddress) {
        onView(withId(R.id.to)).perform(typeText(toAddress));
    }

    public void inputSubject(String subject) {
        onView(withId(R.id.subject)).perform(typeText(subject));
    }

    public void inputMessageContent(String messageText) {
        onView(withId(R.id.message_content)).perform(typeText(messageText));
    }

    public void send() {
        onView(withId(R.id.send)).perform(click());
    }
}
