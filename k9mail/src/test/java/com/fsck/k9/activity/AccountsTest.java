package com.fsck.k9.activity;


import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.widget.TextView;

import com.fsck.k9.Account;
import com.fsck.k9.Identity;
import com.fsck.k9.K9;
import com.fsck.k9.K9RobolectricTestRunner;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.setup.WelcomeMessage;
import com.fsck.k9.helper.ParcelableUtil;
import com.fsck.k9.search.LocalSearch;
import org.apache.tools.ant.taskdefs.Local;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.robolectric.Shadows.shadowOf;


@RunWith(K9RobolectricTestRunner.class)
public class AccountsTest {
    private Accounts activity;

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
        addAccountWithIdentity("user@example.org");
        activity = Robolectric.buildActivity(Accounts.class).create().get();

        assertNextStartedActivityIs(UpgradeDatabases.class);
    }

    @Test
    public void onCreate_startsWelcomeMessage_withNoAccounts() {
        K9.setDatabasesUpToDate(false);

        activity = Robolectric.buildActivity(Accounts.class).create().get();

        assertNextStartedActivityIs(WelcomeMessage.class);
    }

    @Test
    public void onCreate_startsMessageListForIntegratedInbox__whenIntegratedInboxEnabled() {
        K9.setStartIntegratedInbox(true);
        K9.setDatabasesUpToDate(false);
        addAccountWithIdentity("user@example.org");

        activity = Robolectric.buildActivity(Accounts.class).create().get();

        Intent nextStartedActivity = shadowOf(activity).getNextStartedActivity();
        assertEquals(MessageList.class.getName(),
                nextStartedActivity.getComponent().getClassName());
        LocalSearch search = ParcelableUtil.unmarshall(nextStartedActivity.getByteArrayExtra("search_bytes"),
                LocalSearch.CREATOR);
        assertEquals(activity.getString(R.string.integrated_inbox_title),
                search.getName());
        //TODO: Decode search for unified inbox
    }

    @Test
    public void onCreate_startsMessageListForSingleInbox__whenOnly1Account() {
        K9.setStartIntegratedInbox(false);
        K9.setDatabasesUpToDate(false);
        String uuid = addAccountWithIdentity("user@example.org");

        activity = Robolectric.buildActivity(Accounts.class).create().get();

        Intent nextStartedActivity = shadowOf(activity).getNextStartedActivity();
        assertEquals(MessageList.class.getName(),
                nextStartedActivity.getComponent().getClassName());
        LocalSearch search = ParcelableUtil.unmarshall(nextStartedActivity.getByteArrayExtra("search_bytes"),
                LocalSearch.CREATOR);
        assertEquals("INBOX",
                search.getName());
        assertEquals(1,
                search.getAccountUuids().length);
        assertEquals(uuid,
                search.getAccountUuids()[0]);
    }

    @Test
    public void onCreate_startsNothing__whenMultipleAccounts() {
        K9.setStartIntegratedInbox(false);
        K9.setDatabasesUpToDate(false);
        addAccountWithIdentity("user@example.org");
        addAccountWithIdentity("user@example2.org");

        activity = Robolectric.buildActivity(Accounts.class).create().get();

        Intent nextStartedActivity = shadowOf(activity).getNextStartedActivity();
        assertNull(nextStartedActivity);
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
        return account.getUuid();

    }
}
