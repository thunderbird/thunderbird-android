package com.fsck.k9.activity.setup.accounttype;


import java.net.URISyntaxException;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.fsck.k9.R;
import com.fsck.k9.activity.setup.AbstractAccountSetup;
import com.fsck.k9.activity.setup.AbstractAccountSetup.AccountState;
import com.fsck.k9.activity.setup.accounttype.AccountTypeContract.Presenter;
import timber.log.Timber;

import static com.fsck.k9.mail.ServerSettings.Type.IMAP;
import static com.fsck.k9.mail.ServerSettings.Type.POP3;


public class AccountTypeView implements AccountTypeContract.View, OnClickListener {
    private Presenter presenter;
    private AbstractAccountSetup activity;
    private AccountState state;

    public AccountTypeView(AbstractAccountSetup activity) {
        setActivity(activity);
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void setActivity(AbstractAccountSetup activity) {
        this.activity = activity;
        state = activity.getState();
    }

    @Override
    public void start() {
        activity.findViewById(R.id.pop).setOnClickListener(this);
        activity.findViewById(R.id.imap).setOnClickListener(this);
        activity.findViewById(R.id.webdav).setOnClickListener(this);

        presenter = new AccountTypePresenter(this, activity.getState());
    }

    @Override
    public void onClick(View v) {
        try {
            switch (v.getId()) {
                case R.id.pop:
                    presenter.setupStoreAndSmtpTransport(POP3, "pop3+ssl+");
                    break;
                case R.id.imap:
                    presenter.setupStoreAndSmtpTransport(IMAP, "imap+ssl+");
                    break;
                case R.id.webdav:
                    presenter.setupDav();
                    break;
            }
        } catch (URISyntaxException e) {
            failure(e);
        }
    }

    private void failure(Exception use) {
        Timber.e(use, "Failure");
        String toastText = activity.getString(R.string.account_setup_bad_uri, use.getMessage());

        Toast toast = Toast.makeText(activity, toastText, Toast.LENGTH_LONG);
        toast.show();
    }

    @Override
    public void onSetupFinished() {
        activity.goToIncoming();
    }
}
