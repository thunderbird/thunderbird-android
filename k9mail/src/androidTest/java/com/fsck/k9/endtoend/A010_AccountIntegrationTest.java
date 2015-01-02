package com.fsck.k9.endtoend;

import com.fsck.k9.activity.Accounts;
import com.fsck.k9.endtoend.framework.AccountForTest;
import com.fsck.k9.endtoend.framework.ApplicationState;
import com.fsck.k9.endtoend.pages.AccountsPage;

/**
 * Creates and removes accounts.
 *
 * Because of the way K-9 shows the start page, there must already be two accounts
 * in existence for this test to work.
 */
public class A010_AccountIntegrationTest extends AbstractEndToEndTest<Accounts>{

    public A010_AccountIntegrationTest() {
        super(Accounts.class);
    }

    public void testCreateAccountDirectly() throws Exception {
        new AccountSetupFlow(this).setupAccountFromAccountsPage(new AccountsPage());
    }

    public void testDeleteAccount() {

        AccountsPage accountsPage = new AccountsPage();

        AccountForTest accountForTest = ApplicationState.getInstance().accounts.get(0);
        accountsPage.assertAccountExists(accountForTest.description);

        accountsPage.clickLongOnAccount(accountForTest);

        accountsPage.clickRemoveInAccountMenu();

        accountsPage.clickOK();

        accountsPage.assertAccountDoesNotExist(accountForTest.description);

    }
}
