package com.fsck.k9.activity.setup;

/*
    After gathering the necessary information one way or another this activity displays it to the user, eventually warns
    him it could be false ( no https ), asks for confirmation, allows selection of protocol,... and then goes on to test
    the final settings.
 */

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.helper.configxmlparser.AutoconfigInfo;
import com.fsck.k9.helper.configxmlparser.AutoconfigInfo.ServerType;

public class AccountSetupConfirmIncoming extends K9Activity implements View.OnClickListener {

    private static final String EXTRA_ACCOUNT = "account";
    private static final String EXTRA_CONFIG_INFO = "configInfo";
    private static final String EXTRA_EMAIL = "email";
    private static final String EXTRA_PASSWORD = "password";


    public static void actionConfirmIncoming(Context context, Account account, AutoconfigInfo info) {
        Intent i = new Intent(context, AccountSetupConfirmIncoming.class);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        i.putExtra(EXTRA_CONFIG_INFO, info);
        context.startActivity(i);
    }

    public static void actionConfirmIncoming(Context context, String email, String password, AutoconfigInfo info){
        Intent i = new Intent(context, AccountSetupConfirmIncoming.class);
        i.putExtra(EXTRA_EMAIL, email);
        i.putExtra(EXTRA_PASSWORD, password);
        i.putExtra(EXTRA_CONFIG_INFO, info);
        context.startActivity(i);
    }

    // data
    Account mAccount;
    AutoconfigInfo mConfigInfo;

    // gui elements
    Spinner mProtocolSpinner;
    Spinner mHostnameSpinner;

    @Override
    public void onCreate(Bundle savedInstance){
        super.onCreate(savedInstance);
        setContentView(R.layout.account_setup_confirm);

        // initialise gui elements from inflated layout
        mHostnameSpinner = (Spinner) findViewById(R.id.spinner_hostname);
        mProtocolSpinner = (Spinner) findViewById(R.id.spinner_protocol);

        // get the data out of our intent
        // if no blank account passed make one
        String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
        if(accountUuid != null)
            mAccount = Preferences.getPreferences(this).getAccount(accountUuid);
        else mAccount = Account.getBlankAccount(this,
                    getIntent().getStringExtra(EXTRA_EMAIL),
                    getIntent().getStringExtra(EXTRA_PASSWORD));
        mConfigInfo = getIntent().getParcelableExtra(EXTRA_CONFIG_INFO);

        // attach data to gui elements
        // TODO: could make it's own layout xml..
        ArrayAdapter<ServerType> protocolAdapter =
                new ArrayAdapter<ServerType>(this, R.layout.account_setup_index_list_item, mConfigInfo.getAvailableIncomingServerTypes());
        mHostnameSpinner.setAdapter(protocolAdapter);

        // attach the listeners

    }

    @Override
    public void onClick(View view) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

}
