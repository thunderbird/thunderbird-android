package com.fsck.k9.endtoend;

import android.app.Activity;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import com.fsck.k9.R;
import com.fsck.k9.endtoend.framework.ApplicationState;
import com.fsck.k9.endtoend.framework.StubMailServer;
import com.fsck.k9.endtoend.pages.WelcomeMessagePage;
import android.support.test.espresso.assertion.ViewAssertions;

import junit.framework.AssertionFailedError;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

public abstract class AbstractEndToEndTest<T extends Activity> extends ActivityInstrumentationTestCase2<T> {

    private ApplicationState state = ApplicationState.getInstance();
    private final boolean bypassWelcome;

    public AbstractEndToEndTest(Class<T> activityClass) {
        this(activityClass, true);
    }

    public AbstractEndToEndTest(Class<T> activityClass, boolean bypassWelcome) {
        super(activityClass);
        this.bypassWelcome = bypassWelcome;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        getActivity();

        if (bypassWelcome) {
            bypassWelcomeScreen();
        }
    }

    private void bypassWelcomeScreen() {
        try {
            onView(withId(R.id.welcome_message)).check(ViewAssertions.doesNotExist());
        } catch (AssertionFailedError ex) {
            /*
             * The view doesn't NOT exist == the view exists, and needs to be bypassed!
             */
            Log.d(getClass().getName(), "Bypassing welcome");
            new AccountSetupFlow(this).setupAccountFromWelcomePage(new WelcomeMessagePage());
        }
    }

    protected StubMailServer setupMailServer() {
        if (null == state.stubMailServer) {
            state.stubMailServer = new StubMailServer();
        }
        return state.stubMailServer;
    }
}
