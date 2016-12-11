
package com.fsck.k9.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import com.fsck.k9.Account;
import com.fsck.k9.Account.FolderMode;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.controller.SnoozeController;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mailstore.LocalFolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class ChooseSnooze extends K9ListActivity {
    public static final String EXTRA_SNOOZE_UNTIL = "com.fsck.k9.ChooseSnooze_EXTRA_SNOOZE_UNTIL";

    String mFolder;
    String mSelectFolder;
    Account mAccount;
    MessageReference mMessageReference;
    ArrayAdapter<SnoozeTime> mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.list_content_simple);

        getListView().setFastScrollEnabled(true);
        getListView().setItemsCanFocus(false);
        getListView().setChoiceMode(ListView.CHOICE_MODE_NONE);
        Intent intent = getIntent();
        String accountUuid = intent.getStringExtra(ChooseFolder.EXTRA_ACCOUNT);
        mAccount = Preferences.getPreferences(this).getAccount(accountUuid);
        mMessageReference = intent.getParcelableExtra(ChooseFolder.EXTRA_MESSAGE);
        mFolder = intent.getStringExtra(ChooseFolder.EXTRA_CUR_FOLDER);
        mSelectFolder = intent.getStringExtra(ChooseFolder.EXTRA_SEL_FOLDER);

        if (mFolder == null)
            mFolder = "";

        mAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        mAdapter.addAll(getTimes());

        setListAdapter(mAdapter);

        this.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SnoozeTime time = mAdapter.getItem(position);

                if (time.timestamp <= 0) {
                    // choose custom time from picker
                    // TODO(tf):
                    return;
                }

                finishWithResult(time.timestamp);
            }
        });
    }

    private void finishWithResult(long timestamp) {
        Intent result = new Intent();
        result.putExtra(ChooseFolder.EXTRA_ACCOUNT, mAccount.getUuid());
        result.putExtra(ChooseFolder.EXTRA_CUR_FOLDER, mFolder);
        result.putExtra(EXTRA_SNOOZE_UNTIL, timestamp);
        result.putExtra(ChooseFolder.EXTRA_MESSAGE, mMessageReference);
        setResult(RESULT_OK, result);
        finish();
    }

    private List<SnoozeTime> getTimes() {
        List<SnoozeTime> times = new LinkedList<>();

        long now = System.currentTimeMillis();
        times.add(new SnoozeTime(now + TimeUnit.HOURS.toMillis(1)));
        times.add(new SnoozeTime(now + TimeUnit.DAYS.toMillis(1)));
        times.add(new SnoozeTime(now + TimeUnit.DAYS.toMillis(7)));
        // TODO(tf): next monday

        // TODO(tf): add most recently chosen custom times

        times.add(new SnoozeTime(getResources().getString(R.string.pick_a_time), 0));

        return times;
    }


//
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        super.onCreateOptionsMenu(menu);
//        getMenuInflater().inflate(R.menu.folder_select_option, menu);
//        configureFolderSearchView(menu);
//        return true;
//    }


    private static class SnoozeTime {
        public final long timestamp;
        public final CharSequence label;

        private SnoozeTime(@NonNull CharSequence label, long timestamp) {
            this.timestamp = timestamp;
            this.label = label;
        }

        private SnoozeTime(long timestamp) {
            this.timestamp = timestamp;
            this.label = SnoozeController.getSnoozeMessage(timestamp);
        }

        @Override
        public String toString() {
            return label.toString();
        }
    }
}
