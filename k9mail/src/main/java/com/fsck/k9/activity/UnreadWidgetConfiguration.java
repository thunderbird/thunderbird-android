package com.fsck.k9.activity;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.helper.UnreadWidgetProperties;
import com.fsck.k9.provider.UnreadWidgetProvider;
import com.fsck.k9.search.SearchAccount;

import timber.log.Timber;


/**
 * Activity to select an account for the unread widget.
 */
public class UnreadWidgetConfiguration extends K9PreferenceActivity {
    /**
     * Name of the preference file to store the widget configuration.
     */
    private static final String PREFS_NAME = "unread_widget_configuration.xml";

    /**
     * Prefixes for the preference keys
     */
    private static final String PREF_PREFIX_KEY = "unread_widget.";
    private static final String PREF_FOLDER_NAME_SUFFIX_KEY = ".folder_name";

    private static final String PREFERENCE_UNREAD_ACCOUNT = "unread_account";
    private static final String PREFERENCE_UNREAD_FOLDER_ENABLED = "unread_folder_enabled";
    private static final String PREFERENCE_UNREAD_FOLDER = "unread_folder";

    /**
     * The ID of the widget we are configuring.
     */
    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private static final int REQUEST_CHOOSE_ACCOUNT = 1;
    private static final int REQUEST_CHOOSE_FOLDER = 2;

    private Preference unreadAccount;
    private CheckBoxPreference unreadFolderEnabled;
    private Preference unreadFolder;

    private String selectedAccountUuid;
    private String selectedFolderName;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Find the widget ID from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If they gave us an intent without the widget ID, just bail.
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Timber.e("Received an invalid widget ID");
            finish();
            return;
        }

        addPreferencesFromResource(R.xml.unread_widget_configuration);
        unreadAccount = findPreference(PREFERENCE_UNREAD_ACCOUNT);
        unreadAccount.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(UnreadWidgetConfiguration.this, ChooseAccount.class);
                startActivityForResult(intent, REQUEST_CHOOSE_ACCOUNT);
                return false;
            }
        });

        unreadFolderEnabled = (CheckBoxPreference) findPreference(PREFERENCE_UNREAD_FOLDER_ENABLED);
        unreadFolderEnabled.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                unreadFolder.setSummary(getString(R.string.unread_widget_folder_summary));
                selectedFolderName = null;
                return true;
            }
        });

        unreadFolder = findPreference(PREFERENCE_UNREAD_FOLDER);
        unreadFolder.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(UnreadWidgetConfiguration.this, ChooseFolder.class);
                intent.putExtra(ChooseFolder.EXTRA_ACCOUNT, selectedAccountUuid);
                intent.putExtra(ChooseFolder.EXTRA_SHOW_DISPLAYABLE_ONLY, "yes");
                startActivityForResult(intent, REQUEST_CHOOSE_FOLDER);
                return false;
            }
        });
        setTitle(R.string.unread_widget_select_account);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CHOOSE_ACCOUNT:
                    handleChooseAccount(data.getStringExtra(ChooseAccount.EXTRA_ACCOUNT_UUID));
                    break;
                case REQUEST_CHOOSE_FOLDER:
                    handleChooseFolder(data.getStringExtra(ChooseFolder.EXTRA_NEW_FOLDER));
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void handleChooseAccount(String accountUuid) {
        boolean userSelectedSameAccount = accountUuid.equals(selectedAccountUuid);
        if (userSelectedSameAccount) {
            return;
        }

        selectedAccountUuid = accountUuid;
        selectedFolderName = null;
        unreadFolder.setSummary(getString(R.string.unread_widget_folder_summary));
        if (SearchAccount.UNIFIED_INBOX.equals(selectedAccountUuid) ||
                SearchAccount.ALL_MESSAGES.equals(selectedAccountUuid)) {
            handleSearchAccount();
        } else {
            handleRegularAccount();
        }
    }

    private void handleSearchAccount() {
        if (SearchAccount.UNIFIED_INBOX.equals(selectedAccountUuid)) {
            unreadAccount.setSummary(R.string.unread_widget_unified_inbox_account_summary);
        } else if (SearchAccount.ALL_MESSAGES.equals(selectedAccountUuid)) {
            unreadAccount.setSummary(R.string.unread_widget_all_messages_account_summary);
        }
        unreadFolderEnabled.setEnabled(false);
        unreadFolderEnabled.setChecked(false);
        unreadFolder.setEnabled(false);
        selectedFolderName = null;
    }

    private void handleRegularAccount() {
        Account selectedAccount = Preferences.getPreferences(this).getAccount(selectedAccountUuid);
        String summary = selectedAccount.getDescription();
        if (summary == null || summary.isEmpty()) {
            summary = selectedAccount.getEmail();
        }
        unreadAccount.setSummary(summary);
        unreadFolderEnabled.setEnabled(true);
        unreadFolder.setEnabled(true);
    }

    private void handleChooseFolder(String folderName) {
        selectedFolderName = folderName;
        unreadFolder.setSummary(selectedFolderName);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.unread_widget_option, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.done:
                if (validateWidget()) {
                    updateWidgetAndExit();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean validateWidget() {
        if (selectedAccountUuid == null) {
            Toast.makeText(this, R.string.unread_widget_account_not_selected, Toast.LENGTH_LONG).show();
            return false;
        } else if (unreadFolderEnabled.isChecked() && selectedFolderName == null) {
            Toast.makeText(this, R.string.unread_widget_folder_not_selected, Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private void updateWidgetAndExit() {
        UnreadWidgetProperties properties = new UnreadWidgetProperties(appWidgetId, selectedAccountUuid,selectedFolderName);
        saveWidgetProperties(this, properties);

        // Update widget
        Context context = getApplicationContext();
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        UnreadWidgetProvider.updateWidget(context, appWidgetManager, properties);

        // Let the caller know that the configuration was successful
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }

    private static void saveWidgetProperties(Context context, UnreadWidgetProperties properties) {
        int appWidgetId = properties.getAppWidgetId();
        SharedPreferences.Editor editor =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(PREF_PREFIX_KEY + appWidgetId, properties.getAccountUuid());
        editor.putString(PREF_PREFIX_KEY + appWidgetId + PREF_FOLDER_NAME_SUFFIX_KEY, properties.getFolderName());
        editor.apply();
    }

    public static UnreadWidgetProperties getWidgetProperties(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String accountUuid = prefs.getString(PREF_PREFIX_KEY + appWidgetId, null);
        String folderName = prefs.getString(PREF_PREFIX_KEY + appWidgetId + PREF_FOLDER_NAME_SUFFIX_KEY, null);
        return new UnreadWidgetProperties(appWidgetId, accountUuid, folderName);
    }

    public static void deleteWidgetConfiguration(Context context, int appWidgetId) {
        Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.remove(PREF_PREFIX_KEY + appWidgetId);
        editor.remove(PREF_PREFIX_KEY + appWidgetId + PREF_FOLDER_NAME_SUFFIX_KEY);
        editor.apply();
    }
}
