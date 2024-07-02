package com.fsck.k9.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.Toast;

import com.fsck.k9.Identity;
import com.fsck.k9.Preferences;
import com.fsck.k9.ui.R;

public class ManageIdentities extends ChooseIdentity {
    private boolean mIdentitiesChanged = false;

    private static final int ACTIVITY_EDIT_IDENTITY = 1;

    public static void start(Activity activity, String accountUuid) {
        Intent intent = new Intent(activity, ManageIdentities.class);
        intent.putExtra(ChooseIdentity.EXTRA_ACCOUNT, accountUuid);
        activity.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.manage_identities_title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void setupClickListeners() {
        this.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                editItem(position);
            }
        });

        ListView listView = getListView();
        registerForContextMenu(listView);
    }

    private void editItem(int i) {
        Intent intent = new Intent(ManageIdentities.this, EditIdentity.class);
        intent.putExtra(EditIdentity.EXTRA_ACCOUNT, mAccount.getUuid());
        intent.putExtra(EditIdentity.EXTRA_IDENTITY, mAccount.getIdentity(i));
        intent.putExtra(EditIdentity.EXTRA_IDENTITY_INDEX, i);
        startActivityForResult(intent, ACTIVITY_EDIT_IDENTITY);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.manage_identities_option, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.new_identity) {
            Intent intent = new Intent(ManageIdentities.this, EditIdentity.class);
            intent.putExtra(EditIdentity.EXTRA_ACCOUNT, mAccount.getUuid());
            startActivityForResult(intent, ACTIVITY_EDIT_IDENTITY);
        } else if (item.getItemId() == android.R.id.home) {
            finish();
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle(R.string.manage_identities_context_menu_title);
        getMenuInflater().inflate(R.menu.manage_identities_context, menu);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo)item.getMenuInfo();
        int id = item.getItemId();
        if (id == R.id.edit) {
            editItem(menuInfo.position);
        } else if (id == R.id.up) {
            if (menuInfo.position > 0) {
                Identity identity = identities.remove(menuInfo.position);
                identities.add(menuInfo.position - 1, identity);
                mIdentitiesChanged = true;
                refreshView();
            }
        } else if (id == R.id.down) {
            if (menuInfo.position < identities.size() - 1) {
                Identity identity = identities.remove(menuInfo.position);
                identities.add(menuInfo.position + 1, identity);
                mIdentitiesChanged = true;
                refreshView();
            }
        } else if (id == R.id.top) {
            Identity identity = identities.remove(menuInfo.position);
            identities.add(0, identity);
            mIdentitiesChanged = true;
            refreshView();
        } else if (id == R.id.remove) {
            if (identities.size() > 1) {
                identities.remove(menuInfo.position);
                mIdentitiesChanged = true;
                refreshView();
            } else {
                Toast.makeText(this, getString(R.string.no_removable_identity),
                               Toast.LENGTH_LONG).show();
            }
        }
        return true;
    }


    @Override
    public void onResume() {
        super.onResume();
        //mAccount.refresh(Preferences.getPreferences(getApplication().getApplicationContext()));
        refreshView();
    }


    @Override
    public void onStop() {
        // TODO: Instead of saving the changes when the activity is stopped, save the changes (in a background thread)
        //  immediately after modifying the list of identities.
        saveIdentities();
        super.onStop();
    }

    private void saveIdentities() {
        if (mIdentitiesChanged) {
            mAccount.setIdentities(identities);
            Preferences.getPreferences().saveAccount(mAccount);
        }
    }
}
