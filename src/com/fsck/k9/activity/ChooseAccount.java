package com.fsck.k9.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.TextView;

import com.fsck.k9.Account;
import com.fsck.k9.Identity;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;

import java.io.Serializable;
import java.util.List;

/**
 * Activity displaying list of accounts/identity for user choice
 * 
 * @see K9ExpandableListActivity
 */
public class ChooseAccount extends K9ExpandableListActivity
{

    /**
     * {@link Intent} extended data name for storing {@link Account#getUuid()
     * account UUID}
     */
    public static final String EXTRA_ACCOUNT = ChooseAccount.class.getName() + "_account";

    /**
     * {@link Intent} extended data name for storing serialized {@link Identity}
     */
    public static final String EXTRA_IDENTITY = ChooseAccount.class.getName() + "_identity";

    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        final ExpandableListView expandableListView = getExpandableListView();
        expandableListView.setTextFilterEnabled(true);
        expandableListView.setItemsCanFocus(false);
        expandableListView.setChoiceMode(ListView.CHOICE_MODE_NONE);

        final ExpandableListAdapter adapter = createAdapter();

        setListAdapter(adapter);

        expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener()
        {

            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                    int childPosition, long id)
            {
                final Identity identity = (Identity) adapter.getChild(groupPosition, childPosition);
                final Account account = (Account) adapter.getGroup(groupPosition);

                final Intent intent = new Intent();

                intent.putExtra(EXTRA_ACCOUNT, account.getUuid());
                intent.putExtra(EXTRA_IDENTITY, identity);

                setResult(RESULT_OK, intent);

                finish();
                return true;
            }
        });

        final Bundle extras = getIntent().getExtras();
        final String uuid = extras.getString(EXTRA_ACCOUNT);
        if (uuid != null)
        {
            final Account[] accounts = Preferences.getPreferences(this).getAccounts();
            final int length = accounts.length;
            for (int i = 0; i < length; i++)
            {
                final Account account = accounts[i];
                if (uuid.equals(account.getUuid()))
                {
                    // setSelectedChild() doesn't seem to obey the
                    // shouldExpandGroup parameter (2.1), manually expanding
                    // group
                    expandableListView.expandGroup(i);

                    final List<Identity> identities = account.getIdentities();
                    final Identity identity = (Identity) extras.getSerializable(EXTRA_IDENTITY);
                    if (identity == null)
                    {
                        expandableListView.setSelectedChild(i, 0, true);
                        break;
                    }
                    for (int j = 0; j < identities.size(); j++)
                    {
                        final Identity loopIdentity = identities.get(j);
                        if (identity.equals(loopIdentity))
                        {
                            expandableListView.setSelectedChild(i, j, true);
                            break;
                        }
                    }
                    break;
                }
            }
        }
    }

    private ExpandableListAdapter createAdapter()
    {
        return new IdentitiesAdapter(this);
    }

    /**
     * Dynamically provides accounts/identities data for
     * {@link ExpandableListView#setAdapter(ExpandableListAdapter)}:
     * 
     * <ul>
     * <li>Groups represent {@link Account accounts}</li>
     * <li>Children represent {@link Identity identities} of the parent account</li>
     * </ul>
     */
    public/* static */class IdentitiesAdapter extends BaseExpandableListAdapter
    {

        private Context mContext;
        private LayoutInflater mLayoutInflater;

        public IdentitiesAdapter(final Context context)
        {
            mContext = context;
            // mLayoutInflater = (LayoutInflater) context
            // .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            mLayoutInflater = ChooseAccount.this.getLayoutInflater();
        }

        @Override
        public Object getChild(int groupPosition, int childPosition)
        {
            return getAccounts()[groupPosition].getIdentity(childPosition);
        }

        @Override
        public long getChildId(int groupPosition, int childPosition)
        {
            return Integer.valueOf(childPosition).longValue();
        }

        @Override
        public int getChildrenCount(int groupPosition)
        {
            return getAccounts()[groupPosition].getIdentities().size();
        }

        @Override
        public Object getGroup(int groupPosition)
        {
            return getAccounts()[groupPosition];
        }

        @Override
        public int getGroupCount()
        {
            return getAccounts().length;
        }

        @Override
        public long getGroupId(int groupPosition)
        {
            return Integer.valueOf(groupPosition).longValue();
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                ViewGroup parent)
        {
            final View v;
            if (convertView == null)
            {
                // is it okay to reuse?
                v = mLayoutInflater.inflate(R.layout.accounts_item, parent, false);
            }
            else
            {
                v = convertView;
            }

            final TextView description = (TextView) v.findViewById(R.id.description);
            final Account account = getAccounts()[groupPosition];
            description.setText(account.getDescription());
            description
                    .setTextSize(TypedValue.COMPLEX_UNIT_DIP, K9.getFontSizes().getAccountName());

            // since we're reusing existing layout, manually hide unwanted views
            v.findViewById(R.id.chip).setBackgroundColor(account.getChipColor());
            v.findViewById(R.id.active_icons).setVisibility(View.GONE);
            v.findViewById(R.id.folders).setVisibility(View.INVISIBLE);
            v.findViewById(R.id.email).setVisibility(View.GONE);

            return v;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                View convertView, ViewGroup parent)
        {
            final Identity identity = getAccounts()[groupPosition].getIdentity(childPosition);

            final View v;
            if (convertView == null)
            {
                v = mLayoutInflater.inflate(android.R.layout.simple_expandable_list_item_2, parent,
                        false);
            }
            else
            {
                v = convertView;
            }

            final TextView textView1 = (TextView) v.findViewById(android.R.id.text1);
            final TextView textView2 = (TextView) v.findViewById(android.R.id.text2);
            textView1.setText(identity.getDescription());
            // XXX: consider proper string?
            textView2.setText(mContext.getString(R.string.message_view_from_format,
                    identity.getName(), identity.getEmail()));
            textView2.setHorizontallyScrolling(true);

            return v;
        }

        @Override
        public boolean hasStableIds()
        {
            // returning false since accounts/identities are mutable
            return false;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition)
        {
            return true;
        }

        private Account[] getAccounts()
        {
            return Preferences.getPreferences(mContext).getAccounts();
        }

    }

}
