
package com.fsck.k9.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import com.fsck.k9.Account;
import com.fsck.k9.Identity;
import com.fsck.k9.K9ListActivity;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import java.util.List;

public class ChooseIdentity extends K9ListActivity
{
    Account mAccount;
    String mUID;
    ArrayAdapter<String> adapter;

    public static final String EXTRA_ACCOUNT = "com.fsck.k9.ChooseIdentity_account";
    public static final String EXTRA_IDENTITY = "com.fsck.k9.ChooseIdentity_identity";

    protected List<Identity> identities = null;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        getListView().setTextFilterEnabled(true);
        getListView().setItemsCanFocus(false);
        getListView().setChoiceMode(ListView.CHOICE_MODE_NONE);
        Intent intent = getIntent();
        String accountUuid = intent.getStringExtra(EXTRA_ACCOUNT);
        mAccount = Preferences.getPreferences(this).getAccount(accountUuid);

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

        setListAdapter(adapter);
        setupClickListeners();
    }


    @Override
    public void onResume()
    {
        super.onResume();
        refreshView();
    }


    protected void refreshView()
    {
        adapter.clear();

        identities = mAccount.getIdentities();
        for (Identity identity : identities)
        {
            String description = identity.getDescription();
            if (description == null || description.trim().length() == 0)
            {
                description = getString(R.string.message_view_from_format, identity.getName(), identity.getEmail());
            }
            adapter.add(description);
        }

    }

    protected void setupClickListeners()
    {
        this.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                Identity identity = mAccount.getIdentity(position);
                String email = identity.getEmail();
                if (email != null && email.trim().equals("") == false)
                {
                    Intent intent = new Intent();

                    intent.putExtra(EXTRA_IDENTITY, mAccount.getIdentity(position));
                    setResult(RESULT_OK, intent);
                    finish();
                }
                else
                {
                    Toast.makeText(ChooseIdentity.this, getString(R.string.identity_has_no_email),
                                   Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    class ChooseIdentityHandler extends Handler
    {

        private static final int MSG_PROGRESS = 2;
        private static final int MSG_DATA_CHANGED = 3;

        @Override
        public void handleMessage(android.os.Message msg)
        {
            switch (msg.what)
            {
                case MSG_PROGRESS:
                    setProgressBarIndeterminateVisibility(msg.arg1 != 0);
                    break;
                case MSG_DATA_CHANGED:
                    adapter.notifyDataSetChanged();
                    break;
            }
        }

        public void progress(boolean progress)
        {
            android.os.Message msg = new android.os.Message();
            msg.what = MSG_PROGRESS;
            msg.arg1 = progress ? 1 : 0;
            sendMessage(msg);
        }

        public void dataChanged()
        {
            sendEmptyMessage(MSG_DATA_CHANGED);
        }
    }

}
