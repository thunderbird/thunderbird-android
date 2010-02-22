package com.fsck.k9.activity;

import android.content.Intent;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.Toast;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;

public class ManageIdentities extends ChooseIdentity
{
    public static final String EXTRA_IDENTITIES = "com.fsck.k9.EditIdentity_identities";
    private static final int ACTIVITY_EDIT_IDENTITY = 1;

    @Override
    protected void setupClickListeners()
    {
        this.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                editItem(position);
            }
        });

        ListView listView = getListView();
        registerForContextMenu(listView);
    }

    private void editItem(int position)
    {
        if (position < identities.length)
        {
            Intent intent = new Intent(ManageIdentities.this, EditIdentity.class);
            intent.putExtra(EditIdentity.EXTRA_ACCOUNT, mAccount.getUuid());
            intent.putExtra(EditIdentity.EXTRA_IDENTITY, identities[position].getUuid());
            startActivityForResult(intent, ACTIVITY_EDIT_IDENTITY);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.manage_identities_option, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.new_identity:
                Intent intent = new Intent(ManageIdentities.this, EditIdentity.class);
                intent.putExtra(EditIdentity.EXTRA_ACCOUNT, mAccount.getUuid());
                startActivityForResult(intent, ACTIVITY_EDIT_IDENTITY);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle(R.string.manage_identities_context_menu_title);
        getMenuInflater().inflate(R.menu.manage_identities_context, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo)item.getMenuInfo();
        int position = menuInfo.position;

        Preferences preferences = Preferences.getPreferences(getApplication().getApplicationContext());

        switch (item.getItemId())
        {
            case R.id.edit:
                editItem(position);
                break;

            case R.id.up:
                if (position > 0)
                {
                    mAccount.identityMoveUp(identities[position], preferences);
                    refreshView();
                }

                break;

            case R.id.down:
                if (position < identities.length - 1)
                {
                    mAccount.identityMoveDown(identities[position], preferences);
                    refreshView();
                }
                break;

            case R.id.top:
                mAccount.identityMoveToTop(identities[position], preferences);
                refreshView();
                break;

            case R.id.remove:
                if (identities.length > 1)
                {
                    mAccount.deleteIdentity(identities[position], preferences);
                    refreshView();
                }
                else
                {
                    Toast.makeText(this, getString(R.string.no_removable_identity),
                                   Toast.LENGTH_LONG).show();
                }
                break;
        }
        return true;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        refreshView();
    }
}
