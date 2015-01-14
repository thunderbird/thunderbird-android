package com.fsck.k9.endtoend;

import android.support.test.runner.AndroidJUnit4;

import com.fsck.k9.activity.setup.WelcomeMessage;
import com.fsck.k9.endtoend.framework.ApplicationState;
import com.fsck.k9.endtoend.pages.WelcomeMessagePage;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Creates a new IMAP account via the getting started flow.
 */
@RunWith(AndroidJUnit4.class)
public class A000_WelcomeAndSetupAccountIntegrationTest extends AbstractEndToEndTest<WelcomeMessage> {

    public A000_WelcomeAndSetupAccountIntegrationTest() {
        super(WelcomeMessage.class, false);
    }

    @Test
    public void createAccount() throws Exception {
        new AccountSetupFlow().setupAccountFromWelcomePage(new WelcomeMessagePage());
    }

    @Test
    public void createSecondAccount() throws Exception {
        new AccountSetupFlow().setupAccountFromWelcomePage(new WelcomeMessagePage());
    }
}

