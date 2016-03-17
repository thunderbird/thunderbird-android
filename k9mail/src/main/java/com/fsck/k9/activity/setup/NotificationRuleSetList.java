package com.fsck.k9.activity.setup;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Button;
import android.widget.ArrayAdapter;

import com.fsck.k9.Account;
import com.fsck.k9.NotificationSetting;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.K9ListActivity;

/**
 * Activity for notification rule sets list
 */
public class NotificationRuleSetList extends K9ListActivity implements AdapterView.OnItemClickListener {
    private static final String EXTRA_ACCOUNT = "account";
    private static final String EXTRA_RULE_SET_POSITION = "rule_set_position";

    private Account mAccount;

    private Button mNewRuleSetButton;
    private ListView mListView;
    ArrayAdapter<String> mRuleAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notification_rule_set_list);

        String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
        mAccount = Preferences.getPreferences(this).getAccount(accountUuid);

        mNewRuleSetButton = (Button)findViewById(R.id.new_rule_set);
        mNewRuleSetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNewRuleSet();
            }
        });


        mListView = getListView();
        mListView.setOnItemClickListener(this);
        mListView.setItemsCanFocus(false);

        mRuleAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, mAccount.getNotificationSetting().getNotificationRuleSetNames());
        mListView.setAdapter(mRuleAdapter);

    }

    @Override
    public void onResume() {
        super.onResume();
        mRuleAdapter.clear();
        mRuleAdapter.addAll(mAccount.getNotificationSetting().getNotificationRuleSetNames());
        mRuleAdapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        onNotificationRuleSetSettingsActivity(position);
    }

    private void onNewRuleSet() {
        onNotificationRuleSetSettingsActivity(-1);
    }

    private void onNotificationRuleSetSettingsActivity(int position) {
        Intent i = new Intent(this, NotificationRuleSetSettings.class);
        i.putExtra(EXTRA_ACCOUNT, mAccount.getUuid());
        i.putExtra(EXTRA_RULE_SET_POSITION, position);
        this.startActivity(i);
    }
}
