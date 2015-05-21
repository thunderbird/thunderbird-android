package com.fsck.k9.endtoend;

import com.fsck.k9.activity.setup.WelcomeMessage;
import com.fsck.k9.endtoend.pages.WelcomeMessagePage;
import org.junit.Test;


/**
 * Creates a new IMAP account via the getting started flow.
 */
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

