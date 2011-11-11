package com.fsck.k9.activity.setup;

/*
    After gathering the necessary information one way or another this activity displays it to the user, eventually warns
    him it could be false ( no https ), asks for confirmation, allows selection of protocol,... and then goes on to test
    the final settings.
 */

/*
    NOTE:
        For now there is code and codepaths to enable support multiple hosts for the exact same settings. A user can then
        'browse' through the available hosts. This consists of 2 auto hidden/unhidden buttons and the necessary callbacks.
        The need for this is questionable, if the devs/community decide for, I'll finish the code, if not it will be removed.
 */

import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
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
import com.fsck.k9.helper.configxmlparser.AutoconfigInfo.InformationBlock;
import org.w3c.dom.Text;

import java.util.List;

public abstract class AbstractSetupConfirmActivity extends K9Activity implements View.OnClickListener, OnItemSelectedListener {

    protected static final String EXTRA_ACCOUNT = "account";
    protected static final String EXTRA_CONFIG_INFO = "configInfo";
    protected static final String EXTRA_EMAIL = "email";
    protected static final String EXTRA_PASSWORD = "password";
    protected static final String EXTRA_MAKEDEFAULT = "default";
    
    private final String LOCALPART_EMAIL = "%EMAILLOCALPART%";
    private final String WHOLE_EMAIL = "%EMAILADDRESS%";

    // data
    protected Account mAccount;
    protected AutoconfigInfo mConfigInfo;
    protected String mEmail;
    protected String mUsername;
    private boolean mCustomUsername = false;
    protected String mPassword;
    protected boolean mMakeDefault;

    // references to current selections ( easier to code with )
    protected Server mCurrentServer;
    private ServerType mCurrentType;
    private SocketType mCurrentSocket;
    private List<? extends Server> mCurrentServerList;

    // gui elements
    private Spinner mProtocolSpinner;
    private Spinner mSocketTypeSpinner;
    private TextView mServerInfoText;
    private Button mOkButton;
    private TextView mServerCountLabel;
    private RelativeLayout mServerBrowseButtons;
    private RelativeLayout mServerDocumentation;
    private LinearLayout mUsernameView;
    private EditText mUsernameField;
    private TextView mDocumentationLinks;

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
        mServerCountLabel = (TextView) findViewById(R.id.server_count_label);
        mDocumentationLinks = (TextView) findViewById(R.id.server_documentation_content);
        mUsernameField = (EditText) findViewById(R.id.account_username_field);

        mUsernameView = (LinearLayout) findViewById(R.id.account_custom_username);
        mServerBrowseButtons = (RelativeLayout) findViewById(R.id.confirm_serverbrowse_buttons);
        mServerDocumentation = (RelativeLayout) findViewById(R.id.confirm_documentation_part);

        // get the data out of our intent
        // if no blank account passed make one
        mEmail = getIntent().getStringExtra(EXTRA_EMAIL);
        mPassword = getIntent().getStringExtra(EXTRA_PASSWORD);

        String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
        if(accountUuid != null && !accountUuid.isEmpty())
            mAccount = Preferences.getPreferences(this).getAccount(accountUuid);
        else mAccount = Account.getBlankAccount(this, mEmail, mPassword);

        mConfigInfo = getIntent().getParcelableExtra(EXTRA_CONFIG_INFO);
        mMakeDefault = getIntent().getBooleanExtra(EXTRA_MAKEDEFAULT, false);

        // attach data to gui elements
        ArrayAdapter<ServerType> protocolAdapter = new ArrayAdapter<ServerType>(this,
                android.R.layout.simple_spinner_item, getAvailableServerTypes());
        mProtocolSpinner.setAdapter(protocolAdapter);
        protocolAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        List<SocketType> matchingSocketTypeList = mConfigInfo.getAvailableSocketTypes(
                mConfigInfo.getFilteredServerList(getServers(),protocolAdapter.getItem(0), null, null));
        ArrayAdapter<SocketType> socketTypeAdapter = new ArrayAdapter<SocketType>(this,
                android.R.layout.simple_spinner_item, matchingSocketTypeList);
        mSocketTypeSpinner.setAdapter(socketTypeAdapter);
        socketTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // if there is extra information, display it
        if( mConfigInfo.hasExtraInfo() ){
            fillDocumentation(mDocumentationLinks, mConfigInfo.documentation);
            mServerDocumentation.setVisibility(View.VISIBLE);
        }

        // attach the listeners
        mProtocolSpinner.setOnItemSelectedListener(this);
        mSocketTypeSpinner.setOnItemSelectedListener(this);
        mOkButton.setOnClickListener(this);
        mUsernameField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if( mCustomUsername )
                    if( ! mUsernameField.getText().toString().isEmpty() ) mOkButton.setEnabled(true);
                    else mOkButton.setEnabled(false);
            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });
    }

    @Override
    public void onClick(View view) {
        switch( view.getId() ){
            case R.id.confirm_ok_button:
                finishAction();
                break;
            case R.id.confirm_next_server_button:
                // TODO: write this,... it will probably never be used since no isp has 2 host for exact the same thing
                break;
            case R.id.confirm_prev_server_button:
                // TODO: write this,... it will probably never be used since no isp has 2 host for exact the same thing
                break;
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
                ArrayAdapter<SocketType> tmpAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, newTypes);
                tmpAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                mSocketTypeSpinner.setAdapter(tmpAdapter);
                mSocketTypeSpinner.invalidate();
                break;
            case R.id.spinner_sockettype:
                // this is called on setup too so it initialises the view too
                mCurrentSocket = (SocketType) mSocketTypeSpinner.getAdapter().getItem(pos);
                mCurrentServerList = mConfigInfo.getFilteredServerList(getServers(), mCurrentType, null, mCurrentSocket);
                mCurrentServer = mCurrentServerList.get(0);
                setServerInfo(mCurrentServer);
                setServerCount(1);
                toggleServerChooseButtons();
                break;
        }
    }

    private void toggleServerChooseButtons() {
        if( mCurrentServerList.size() > 1 ){
            mServerBrowseButtons.setVisibility(View.VISIBLE);
        }else{
            mServerBrowseButtons.setVisibility(View.GONE);
        }
    }

    // TODO: if language is provided check against locale, now we just take the first index
    // TODO: use table layout
    private void fillDocumentation(TextView view, List<InformationBlock> info) {
        if( info == null ) return;
        String tmpString = (String) view.getText();

        for( InformationBlock infoBlock : info ){
            tmpString += "\n"+infoBlock.descriptions.get(0).getSecond()+
                    " <a href=\""+infoBlock.url+"\">Read</a><br />";
        }

        view.setText(Html.fromHtml(tmpString));
        view.setMovementMethod(LinkMovementMethod.getInstance());
    }


    private void setServerInfo(Server mCurrentServer) {
        // TODO: use string resources
        mServerInfoText.setText("Host: "+mCurrentServer.hostname+"\n"+
                                "Port: "+mCurrentServer.port+"\n"+
                                "Authentication: "+mCurrentServer.authentication);

        // see if we need a custom username
        if( mCurrentServer.username != null ){
            mServerInfoText.setText(mServerInfoText.getText().toString()+"\n"+
                "Username: "+determineUsername(mCurrentServer));
            mOkButton.setEnabled(true);
            mCustomUsername = false;
            mUsernameView.setVisibility(View.GONE);
        }else{
            mOkButton.setEnabled(false);
            mCustomUsername = true;
            mUsernameView.setVisibility(View.VISIBLE);
        }
    }



    private String determineUsername(Server mCurrentServer) {
        mCustomUsername = false;
        if( mCurrentServer.username.equals(LOCALPART_EMAIL)){
            mUsername = mEmail.split("@")[0];
        }else if( mCurrentServer.username.equals(WHOLE_EMAIL)){
            mUsername = mEmail;
        }else{
            mUsername = mCurrentServer.username;
        }
        return mUsername;
    }


    private void setServerCount(int count){
        mServerCountLabel.setText("( "+count+" / "+mCurrentServerList.size()+" )");
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {}

    /*
        Assuming the first one is set correctly, no checks are build in
     */
    protected String getScheme(){
        String scheme = "";
        scheme = mCurrentType.getSchemeName();
        if( mCurrentSocket != SocketType.plain && mCurrentSocket != SocketType.UNSET )
            scheme += "+"+mCurrentSocket.getSchemeName();
        return scheme;
    }
}

