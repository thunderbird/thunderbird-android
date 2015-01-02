package com.fsck.k9.endtoend;

import com.fsck.k9.activity.setup.WelcomeMessage;
import com.fsck.k9.endtoend.pages.WelcomeMessagePage;

/**
 * Creates a new IMAP account via the getting started flow.
 */
public class A000_WelcomeAndSetupAccountIntegrationTest extends AbstractEndToEndTest<WelcomeMessage> {

    public A000_WelcomeAndSetupAccountIntegrationTest() {
        super(WelcomeMessage.class, false);
    }

    public void testCreateAccount() throws Exception {
        new AccountSetupFlow(this).setupAccountFromWelcomePage(new WelcomeMessagePage());
    }

    public void testCreateSecondAccount() throws Exception {
        new AccountSetupFlow(this).setupAccountFromWelcomePage(new WelcomeMessagePage());
    }

}

