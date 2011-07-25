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
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;
import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.helper.configxmlparser.AutoconfigInfo;
import com.fsck.k9.helper.configxmlparser.AutoconfigInfo.ServerType;
import com.fsck.k9.helper.configxmlparser.AutoconfigInfo.SocketType;
import com.fsck.k9.helper.configxmlparser.AutoconfigInfo.Server;
import java.util.List;

public abstract class AbstractSetupConfirmActivity extends K9Activity implements View.OnClickListener, OnItemSelectedListener {

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

    // references to current selections ( easier to code with )
    Server mCurrentServer;
    ServerType mCurrentType;
    SocketType mCurrentSocket;

    // gui elements
    Spinner mProtocolSpinner;
    Spinner mSocketTypeSpinner;
    TextView mServerInfoText;
    Button mOkButton;

    // difference between incomming & outgoing
    protected abstract List<? extends Server> getServers();
    protected abstract List<ServerType> getAvailableServerTypes();
    protected abstract void finishAction();

    @Override
    public void onCreate(Bundle savedInstance){
        super.onCreate(savedInstance);
        setContentView(R.layout.account_setup_confirm);

        // initialise gui elements from inflated layout
        mSocketTypeSpinner = (Spinner) findViewById(R.id.spinner_sockettype);
        mProtocolSpinner = (Spinner) findViewById(R.id.spinner_protocol);
        mServerInfoText = (TextView) findViewById(R.id.server_information);
        mOkButton = (Button) findViewById(R.id.confirm_ok_button);

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
        ArrayAdapter<ServerType> protocolAdapter = new ArrayAdapter<ServerType>(this,
                R.layout.account_setup_confirm_spinners_item, getAvailableServerTypes());
        mProtocolSpinner.setAdapter(protocolAdapter);

        List<SocketType> matchingSocketTypeList = mConfigInfo.getAvailableSocketTypes(
                mConfigInfo.getFilteredServerList(getServers(),protocolAdapter.getItem(0), null, null));
        ArrayAdapter<SocketType> socketTypeAdapter = new ArrayAdapter<SocketType>(this,
                R.layout.account_setup_confirm_spinners_item, matchingSocketTypeList);
        mSocketTypeSpinner.setAdapter(socketTypeAdapter);

        // attach the listeners
        mProtocolSpinner.setOnItemSelectedListener(this);
        mSocketTypeSpinner.setOnItemSelectedListener(this);
        mOkButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if( view.getId() == R.id.confirm_ok_button ){
            Toast.makeText(this,"go go go", 400).show();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        switch( parent.getId() ){
            case R.id.spinner_protocol:
                mCurrentType = (ServerType) mProtocolSpinner.getAdapter().getItem(pos);
                // now we have to reset the options in sockettype spinner
                List<SocketType> newTypes = mConfigInfo.getAvailableSocketTypes(
                    mConfigInfo.getFilteredServerList(getServers(),(ServerType) mProtocolSpinner.getAdapter().getItem(pos), null, null));
                mSocketTypeSpinner.setAdapter(new ArrayAdapter(this, R.layout.account_setup_confirm_spinners_item, newTypes));
                mSocketTypeSpinner.invalidate();
                break;
            case R.id.spinner_sockettype:
                // this is called on setup too so it initialises the view too
                mCurrentSocket = (SocketType) mSocketTypeSpinner.getAdapter().getItem(pos);
                mCurrentServer = mConfigInfo.getFilteredServerList(getServers(),mCurrentType, null, mCurrentSocket).get(0);
                setServerInfo(mCurrentServer);
                break;
        }
    }

    private void setServerInfo(Server mCurrentServer) {
        // TODO: string resources
        mServerInfoText.setText("Host: "+mCurrentServer.hostname+"\n"+
                                "Port: "+mCurrentServer.port+"\n"+
                                "Authentication: "+mCurrentServer.authentication);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {}
}
