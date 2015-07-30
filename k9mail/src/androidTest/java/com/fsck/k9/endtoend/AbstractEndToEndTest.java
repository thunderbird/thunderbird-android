package com.fsck.k9.endtoend;

import android.app.Activity;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.assertion.ViewAssertions;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import com.fsck.k9.R;
import com.fsck.k9.endtoend.framework.ApplicationState;
import com.fsck.k9.endtoend.framework.StubMailServer;
import com.fsck.k9.endtoend.pages.WelcomeMessagePage;
import de.cketti.library.changelog.ChangeLog;
import junit.framework.AssertionFailedError;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public abstract class AbstractEndToEndTest<T extends Activity> extends ActivityInstrumentationTestCase2<T> {
    private final boolean bypassWelcome;

    public AbstractEndToEndTest(Class<T> activityClass) {
        this(activityClass, true);
    }

    public AbstractEndToEndTest(Class<T> activityClass, boolean bypassWelcome) {
        super(activityClass);
        this.bypassWelcome = bypassWelcome;
    }

    @BeforeClass
    public static void beforeClass() {
        ApplicationState.getInstance().stubMailServer = new StubMailServer();
    }

    @AfterClass
    public static void afterClass() {
        ApplicationState.getInstance().stubMailServer.stop();
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());

        skipChangeLogDialog();

        getActivity();

        if (bypassWelcome) {
            bypassWelcomeScreen();
        }
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    private void skipChangeLogDialog() {
        Context context = getInstrumentation().getTargetContext();
        new ChangeLog(context).skipLogDialog();
    }

    private void bypassWelcomeScreen() {
        try {
            onView(withId(R.id.welcome_message)).check(ViewAssertions.doesNotExist());
        } catch (AssertionFailedError ex) {
            /*
             * The view doesn't NOT exist == the view exists, and needs to be bypassed!
             */
            Log.d(getClass().getName(), "Bypassing welcome");
            new AccountSetupFlow().setupAccountFromWelcomePage(new WelcomeMessagePage());
        }
    }


    public void testEmpty() {
        // workaround, needs to be empty so that JUnit4 test gets picked up
    }
}
