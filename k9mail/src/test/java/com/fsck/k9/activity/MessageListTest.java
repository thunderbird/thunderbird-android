package com.fsck.k9.activity;


import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.fsck.k9.Account;
import com.fsck.k9.Identity;
import com.fsck.k9.K9;
import com.fsck.k9.K9RobolectricTestRunner;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.Accounts;
import com.fsck.k9.activity.MessageList;
import com.fsck.k9.activity.UpgradeDatabases;
import com.fsck.k9.activity.setup.WelcomeMessage;
import com.fsck.k9.helper.ParcelableUtil;
import com.fsck.k9.mail.Message;
import com.fsck.k9.search.LocalSearch;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.robolectric.Shadows.shadowOf;


@RunWith(K9RobolectricTestRunner.class)
public class MessageListTest {
    private MessageList activity;

    @Before
    public void before() throws Exception {
        Field databasesUpToDate = K9.class.getDeclaredField("sDatabasesUpToDate");
        databasesUpToDate.setAccessible(true);
        databasesUpToDate.set(null, false);

        List<Account> accounts = Preferences.getPreferences(RuntimeEnvironment.application).getAccounts();
        for (Account account : accounts) {
            Preferences.getPreferences(RuntimeEnvironment.application).deleteAccount(account);
        }
    }

    @Test
    public void onCreate_startsUpgradeDatabase_whenNotUpToDate() {
        activity = Robolectric.buildActivity(MessageList.class).create().get();

        assertNextStartedActivityIs(UpgradeDatabases.class);
    }

    @Test
    public void onCreate_startsAccounts_whenNoAccountsExist() {
        K9.setDatabasesUpToDate(false);

        activity = Robolectric.buildActivity(MessageList.class).create().get();

        assertNextStartedActivityIs(Accounts.class);
    }

    @Test
    public void onCreate_startsAccounts_whenAnAccountExistsButNoSearchProvided() {
        K9.setDatabasesUpToDate(false);
        addAccountWithIdentity("user@example.org");

        activity = Robolectric.buildActivity(MessageList.class).create().get();

        assertNextStartedActivityIs(Accounts.class);
    }

    @Test
    public void onCreate_startsNothing_whenSearchProvided() {
        K9.setDatabasesUpToDate(false);
        String uuid = addAccountWithIdentity("user@example.org");
        Intent intent = new Intent();
        LocalSearch localSearch = new LocalSearch();
        localSearch.addAccountUuid(uuid);
        localSearch.addAllowedFolder("INBOX");
        intent.putExtra("search_bytes", ParcelableUtil.marshall(localSearch));

        activity = Robolectric.buildActivity(MessageList.class, intent).create().get();

        assertNull(shadowOf(activity).getNextStartedActivity());
    }

    private void assertNextStartedActivityIs(Class<? extends Activity> nextActivity) {
        assertEquals(nextActivity.getName(),
                shadowOf(activity).getNextStartedActivity().getComponent().getClassName());
    }

    private String addAccountWithIdentity(String email) {
        Preferences prefs = Preferences.getPreferences(RuntimeEnvironment.application);
        Account account = prefs.newAccount();
        ArrayList<Identity> identities = new ArrayList<>();
        Identity identity = new Identity();
        identity.setEmail(email);
        identities.add(identity);
        account.setIdentities(identities);
        account.setStoreUri("imap://PLAIN:user:password@server:port");
        return account.getUuid();

    }
}
