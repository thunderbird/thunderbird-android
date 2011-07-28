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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
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
    protected Account mAccount;
    protected AutoconfigInfo mConfigInfo;

    // references to current selections ( easier to code with )
    private Server mCurrentServer;
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

        mServerBrowseButtons = (RelativeLayout) findViewById(R.id.confirm_serverbrowse_buttons);
        mServerDocumentation = (RelativeLayout) findViewById(R.id.confirm_documentation_part);

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

        // if there is extra information, display it
        if( mConfigInfo.hasExtraInfo() ){
            fillDocumentation(mDocumentationLinks, mConfigInfo.documentation);
            mServerDocumentation.setVisibility(View.VISIBLE);
        }

        // attach the listeners
        mProtocolSpinner.setOnItemSelectedListener(this);
        mSocketTypeSpinner.setOnItemSelectedListener(this);
        mOkButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch( view.getId() ){
            case R.id.confirm_ok_button:
                finishAction();
                finish();
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
                mSocketTypeSpinner.setAdapter(new ArrayAdapter(this, R.layout.account_setup_confirm_spinners_item, newTypes));
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
        // TODO: string resources
        mServerInfoText.setText("Host: "+mCurrentServer.hostname+"\n"+
                                "Port: "+mCurrentServer.port+"\n"+
                                "Authentication: "+mCurrentServer.authentication);
    }

    private void setServerCount(int count){
        mServerCountLabel.setText("( "+count+" / "+mCurrentServerList.size()+" )");
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {}
}
