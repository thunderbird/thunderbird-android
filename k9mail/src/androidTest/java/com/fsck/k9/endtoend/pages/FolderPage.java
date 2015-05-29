package com.fsck.k9.endtoend.pages;


import com.fsck.k9.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

public class FolderPage extends AbstractPage {

    public ComposePage clickCompose() {
        onView(withId(R.id.compose)).perform(click());
        return new ComposePage();
    }

}
