package com.fsck.k9.activity.setup;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.fsck.k9.Account;
import com.fsck.k9.Identity;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.K9ListActivity;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalFolder;

import java.util.List;

/**
 * Created by ConteDiMonteCristo on 19/07/15.
 */
public class ChooseLocalFolder extends K9ListActivity {
    public static final String EXTRA_ACCOUNT = "com.fsck.k9.ChooseLocalFolder_account";
    public static final String EXTRA_CHOICE = "com.fsck.k9.ChooseLocalFolder_choice";
    public static final int ACTIVITY_LOCAL_FOLDER = 1;
    Account mAccount;
    ArrayAdapter<String> mAdapter;
    protected List<LocalFolder> mLocalFolders = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.list_content_simple);

        getListView().setTextFilterEnabled(true);
        getListView().setItemsCanFocus(false);
        getListView().setChoiceMode(ListView.CHOICE_MODE_NONE);
        Intent intent = getIntent();
        String accountUuid = intent.getStringExtra(EXTRA_ACCOUNT);

        mAccount = Preferences.getPreferences(this).getAccount(accountUuid);
        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

        setListAdapter(mAdapter);
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                LocalFolder lf = mLocalFolders.get(position);
                try {
                    if (lf.getMessageCount() > 0)
                    {
                        Toast toast = Toast.makeText(getApplication(), getString(R.string.local_folder_delete_not_empty), Toast.LENGTH_SHORT);
                        toast.show();
                        Intent intent = new Intent();
                        setResult(RESULT_CANCELED, intent);
                        finish();
                        return;
                    }
                    Log.i(K9.LOG_TAG, String.format("Local folder to be deleted: %s", lf.getName()));
                    lf.delete(false);
                    Intent intent = new Intent();
                    intent.putExtra(EXTRA_CHOICE,lf.getName());
                    setResult(ACTIVITY_LOCAL_FOLDER, intent);
                    finish();
                } catch (MessagingException e) {
                    Log.e(K9.LOG_TAG, "Unable to delete a local folder", e);
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            refreshView();
        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "Unable to refresh list of local folders", e);
        }
    }

    protected void refreshView() throws MessagingException {
        mAdapter.setNotifyOnChange(false);
        mAdapter.clear();

        mLocalFolders = mAccount.getLocalFolders();
        for(LocalFolder lf : mLocalFolders)
        {
            mAdapter.add(lf.getName());
        }

        mAdapter.notifyDataSetChanged();
    }


}
