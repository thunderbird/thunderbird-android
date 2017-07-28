package com.fsck.k9.activity.setup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterViewFlipper;
import android.widget.BaseAdapter;

import com.fsck.k9.R;
import com.fsck.k9.activity.Accounts;
import com.fsck.k9.activity.setup.accounttype.AccountTypeView;
import com.fsck.k9.activity.setup.basics.BasicsView;
import com.fsck.k9.activity.setup.checksettings.CheckSettingsView;
import com.fsck.k9.activity.setup.incoming.IncomingView;
import com.fsck.k9.activity.setup.names.NamesView;
import com.fsck.k9.activity.setup.outgoing.OutgoingView;
import com.fsck.k9.fragment.ConfirmationDialogFragment.ConfirmationDialogFragmentListener;


public class AccountSetup extends AbstractAccountSetup {
    private AdapterViewFlipper flipper;

    private int position;
    private AccountState state;

    private BasicsView basicsView;
    private CheckSettingsView checkSettingsView;
    private IncomingView incomingView;
    private OutgoingView outgoingView;
    private AccountTypeView accountTypeView;
    private NamesView namesView;

    int[] layoutIds = new int[]{R.layout.account_setup_basics,
            R.layout.account_setup_autoconfiguration, R.layout.account_setup_account_type,
            R.layout.account_setup_incoming, R.layout.account_setup_outgoing, R.layout.account_setup_names};

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

        state = new AccountState();

        basicsView = new BasicsView(this);

        basicsView.start();
    }

    private int getPositionFromLayoutId(@LayoutRes int layoutId) {
        for (int i = 0; i < layoutIds.length; i++) {
            if (layoutIds[i] == layoutId) {
                return i;
            }
        }
        return -1;
    }


    public static void actionNewAccount(Context context) {
        Intent i = new Intent(context, AccountSetup.class);
        context.startActivity(i);
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
        state.setStep(AccountState.STEP_AUTO_CONFIGURATION);
        setSelection(getPositionFromLayoutId(R.layout.account_setup_autoconfiguration));
    }

    @Override
    public void goToAccountType() {
        setSelection(getPositionFromLayoutId(R.layout.account_setup_account_type));
    }

    @Override
    public void goToAccountNames() {
        setSelection(getPositionFromLayoutId(R.layout.account_setup_names));
    }

    @Override
    public void goToOutgoingChecking() {
        state.setStep(AccountState.STEP_CHECK_OUTGOING);
        setSelection(getPositionFromLayoutId(R.layout.account_setup_autoconfiguration));
    }

    @Override
    public void goToIncomingChecking() {
        state.setStep(AccountState.STEP_CHECK_INCOMING);
        setSelection(getPositionFromLayoutId(R.layout.account_setup_autoconfiguration));
    }

    @Override
    public void listAccounts() {
        Accounts.listAccounts(this);
    }

    @Override
    public AccountState getState() {
        return state;
    }

    private void setSelection(int position) {
        if (position == -1) return;

        this.position = position;
        flipper.setSelection(position);

        switch (layoutIds[position]) {
            case R.layout.account_setup_basics:
                basicsView = new BasicsView(this);
                basicsView.start();
                break;
            case R.layout.account_setup_autoconfiguration:
                checkSettingsView = new CheckSettingsView(this);
                checkSettingsView.start();
                break;
            case R.layout.account_setup_incoming:
                incomingView = new IncomingView(this);
                incomingView.start();
                break;
            case R.layout.account_setup_account_type:
                accountTypeView = new AccountTypeView(this);
                accountTypeView.start();
                break;
            case R.layout.account_setup_names:
                namesView = new NamesView(this);
                namesView.start();
                break;
        }
    }

}
