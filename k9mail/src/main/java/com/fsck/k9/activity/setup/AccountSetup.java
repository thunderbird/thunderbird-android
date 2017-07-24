package com.fsck.k9.activity.setup;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterViewFlipper;
import android.widget.BaseAdapter;

import com.fsck.k9.Account;
import com.fsck.k9.R;


public class AccountSetup extends AbstractAccountSetup {
    private AdapterViewFlipper flipper;

    private int position;
    private AccountState state;

    int[] layoutIds = new int[]{R.layout.account_setup_basics,
            R.layout.account_setup_check_settings, R.layout.account_setup_incoming,
            R.layout.account_setup_outgoing, R.layout.account_setup_names};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.account_setup);

        flipper = (AdapterViewFlipper) findViewById(R.id.view_flipper);

        BaseAdapter adapter = new BaseAdapter() {

            @Override
            public int getCount() {
                return layoutIds.length;
            }

            @Override
            public Object getItem(int position) {
                return position;
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(AccountSetup.this)
                            .inflate(layoutIds[position], parent, false);
                }
                return convertView;
            }
        };

        flipper.setAdapter(adapter);
    }

    private int getPositionFromLayoutId(@LayoutRes int layoutId) {
        for (int i = 0; i < layoutIds.length; i++) {
            if (layoutIds[i] == layoutId) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void goToNext() {
        setSelection(position + 1);
    }

    @Override
    public void goToPrevious() {
        setSelection(position - 1);
    }

    @Override
    public void goToBasics() {
        setSelection(getPositionFromLayoutId(R.layout.account_setup_basics));
    }

    @Override
    public void goToOutgoing() {
        setSelection(getPositionFromLayoutId(R.layout.account_setup_outgoing));
    }

    @Override
    public void goToIncoming() {
        setSelection(getPositionFromLayoutId(R.layout.account_setup_incoming));
    }

    @Override
    public void goToManualSetup() {
        setSelection(getPositionFromLayoutId(R.layout.account_setup_account_type));
    }

    @Override
    public void goToAutoConfiguration() {
        setSelection(getPositionFromLayoutId(R.layout.account_setup_autoconfiguration));
    }

    @Override
    public void goToAccountNames() {
        setSelection(getPositionFromLayoutId(R.layout.account_setup_names));
    }

    @Override
    public AccountState getState() {
        return state;
    }

    private void setSelection(int position) {
        if (position == -1) return;

        this.position = position;
        flipper.setSelection(position);
    }

}
