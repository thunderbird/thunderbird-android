package com.fsck.k9.activity;


import java.lang.reflect.Field;
import java.util.ArrayList;

import android.app.Activity;
import android.widget.TextView;

import com.fsck.k9.Account;
import com.fsck.k9.Identity;
import com.fsck.k9.K9;
import com.fsck.k9.K9RobolectricTestRunner;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;
import static org.robolectric.Shadows.shadowOf;


@RunWith(K9RobolectricTestRunner.class)
public class MessageComposeTest {
    private MessageCompose activity;

    @Before
    public void before() throws Exception {
        Field databasesUpToDate = K9.class.getDeclaredField("sDatabasesUpToDate");
        databasesUpToDate.setAccessible(true);
        databasesUpToDate.set(null, false);
    }

    @Test
    public void onCreate_startsUpgradeDatabase_whenNotUpToDate() {
        activity = Robolectric.buildActivity(MessageCompose.class).create().get();

        assertNextStartedActivityIs(UpgradeDatabases.class);
    }

    @Test
    public void onCreate_startsAccounts_whenNoAccountsAvailable() {
        K9.setDatabasesUpToDate(false);

        activity = Robolectric.buildActivity(MessageCompose.class).create().get();

        assertNextStartedActivityIs(Accounts.class);
    }

    @Test
    public void onCreate_usesDefaultAccountFirstIdentity_whenNoAccountProvided() {
        K9.setDatabasesUpToDate(false);
        addDefaultAccountWithIdentity("user@example.org");

        activity = Robolectric.buildActivity(MessageCompose.class).create().get();

        assertEquals("user@example.org",
                ((TextView) activity.findViewById(R.id.identity)).getText());
    }

    private void assertNextStartedActivityIs(Class<? extends Activity> nextActivity) {
        assertEquals(nextActivity.getName(),
                shadowOf(activity).getNextStartedActivity().getComponent().getClassName());
    }

    private void addDefaultAccountWithIdentity(String email) {
        Preferences prefs = Preferences.getPreferences(RuntimeEnvironment.application);
        Account account = prefs.newAccount();
        ArrayList<Identity> identities = new ArrayList<>();
        Identity identity = new Identity();
        identity.setEmail(email);
        identities.add(identity);
        account.setIdentities(identities);
        Preferences.getPreferences(RuntimeEnvironment.application).setDefaultAccount(account);

    }
}
