package com.fsck.k9.activity.compose;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import net.thunderbird.core.android.account.LegacyAccountDto;
import app.k9mail.legacy.di.DI;
import net.thunderbird.core.android.account.Identity;
import com.fsck.k9.Preferences;
import com.fsck.k9.ui.R;
import com.fsck.k9.ui.identity.IdentityFormatter;
import com.google.android.material.textview.MaterialTextView;

/**
 * Adapter for the <em>Choose identity</em> list view.
 *
 * <p>
 * Account names are displayed as section headers, identities as selectable list items.
 * </p>
 */
public class IdentityAdapter extends BaseAdapter {
    private final IdentityFormatter identityFormatter = DI.get(IdentityFormatter.class);

    private LayoutInflater mLayoutInflater;
    private List<Object> mItems;

    public IdentityAdapter(Context context) {
        mLayoutInflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        List<Object> items = new ArrayList<>();
        Preferences prefs = Preferences.getPreferences();
        Collection<LegacyAccountDto> accounts = prefs.getAccounts();
        for (LegacyAccountDto account : accounts) {
            items.add(account);
            List<Identity> identities = account.getIdentities();
            for (Identity identity : identities) {
                items.add(new IdentityContainer(identity, account));
            }
        }
        mItems = items;
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return (mItems.get(position) instanceof LegacyAccountDto) ? 0 : 1;
    }

    @Override
    public boolean isEnabled(int position) {
        return (mItems.get(position) instanceof IdentityContainer);
    }

    @Override
    public Object getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Object item = mItems.get(position);

        View view = null;
        if (item instanceof LegacyAccountDto) {
            if (convertView != null && convertView.getTag() instanceof AccountHolder) {
                view = convertView;
            } else {
                view = mLayoutInflater.inflate(R.layout.choose_account_item, parent, false);
                AccountHolder holder = new AccountHolder();
                holder.name = view.findViewById(R.id.name);
                holder.chip = view.findViewById(R.id.chip);
                view.setTag(holder);
            }

            LegacyAccountDto account = (LegacyAccountDto) item;
            AccountHolder holder = (AccountHolder) view.getTag();
            holder.name.setText(account.getDisplayName());
            holder.chip.setBackgroundColor(account.getChipColor());
        } else if (item instanceof IdentityContainer) {
            if (convertView != null && convertView.getTag() instanceof IdentityHolder) {
                view = convertView;
            } else {
                view = mLayoutInflater.inflate(R.layout.choose_identity_item, parent, false);
                IdentityHolder holder = new IdentityHolder();
                holder.name = view.findViewById(R.id.name);
                holder.description = view.findViewById(R.id.description);
                view.setTag(holder);
            }

            IdentityContainer identityContainer = (IdentityContainer) item;
            Identity identity = identityContainer.identity;
            IdentityHolder holder = (IdentityHolder) view.getTag();
            holder.name.setText(identity.getDescription());
            holder.description.setText(identityFormatter.getEmailDisplayName(identity));
        }

        return view;
    }

    /**
     * Used to store an {@link Identity} instance together with the {@link LegacyAccountDto} it belongs to.
     *
     * @see IdentityAdapter
     */
    public static class IdentityContainer {
        public final Identity identity;
        public final LegacyAccountDto account;

        IdentityContainer(Identity identity, LegacyAccountDto account) {
            this.identity = identity;
            this.account = account;
        }
    }

    static class AccountHolder {
        public MaterialTextView name;
        public View chip;
    }

    static class IdentityHolder {
        public MaterialTextView name;
        public MaterialTextView description;
    }
}
