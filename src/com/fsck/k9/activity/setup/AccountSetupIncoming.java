
package com.fsck.k9.activity.setup;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import com.fsck.k9.*;
import com.fsck.k9.activity.ChooseFolder;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class AccountSetupIncoming extends K9Activity implements OnClickListener
{
    private static final String EXTRA_ACCOUNT = "account";
    private static final String EXTRA_MAKE_DEFAULT = "makeDefault";

    private static final int SELECT_DRAFT_FOLDER = 100;
    private static final int SELECT_SENT_FOLDER = 101;
    private static final int SELECT_TRASH_FOLDER = 102;
    //private static final int SELECT_OUTBOX_FOLDER = 103;

    private static final int popPorts[] =
    {
        110, 995, 995, 110, 110
    };
    private static final String popSchemes[] =
    {
        "pop3", "pop3+ssl", "pop3+ssl+", "pop3+tls", "pop3+tls+"
    };
    private static final int imapPorts[] =
    {
        143, 993, 993, 143, 143
    };
    private static final String imapSchemes[] =
    {
        "imap", "imap+ssl", "imap+ssl+", "imap+tls", "imap+tls+"
    };
    private static final int webdavPorts[] =
    {
        80, 443, 443, 443, 443
    };
    private static final String webdavSchemes[] =
    {
        "webdav", "webdav+ssl", "webdav+ssl+", "webdav+tls", "webdav+tls+"
    };

    private static final String authTypes[] =
    {
        "PLAIN", "CRAM_MD5"
    };


    private int mAccountPorts[];
    private String mAccountSchemes[];
    private EditText mUsernameView;
    private EditText mPasswordView;
    private EditText mServerView;
    private EditText mPortView;
    private Spinner mSecurityTypeView;
    private Spinner mAuthTypeView;
    private EditText mImapPathPrefixView;
    private Button mImapFolderDrafts;
    private Button mImapFolderSent;
    private Button mImapFolderTrash;
    private EditText mImapFolderOutbox;
    private EditText mWebdavPathPrefixView;
    private EditText mWebdavAuthPathView;
    private EditText mWebdavMailboxPathView;
    private Button mNextButton;
    private Account mAccount;
    private boolean mMakeDefault;

    public static void actionIncomingSettings(Activity context, Account account, boolean makeDefault)
    {
        Intent i = new Intent(context, AccountSetupIncoming.class);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        i.putExtra(EXTRA_MAKE_DEFAULT, makeDefault);
        context.startActivity(i);
    }

    public static void actionEditIncomingSettings(Activity context, Account account)
    {
        Intent i = new Intent(context, AccountSetupIncoming.class);
        i.setAction(Intent.ACTION_EDIT);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_setup_incoming);

        mUsernameView = (EditText)findViewById(R.id.account_username);
        mPasswordView = (EditText)findViewById(R.id.account_password);
        TextView serverLabelView = (TextView) findViewById(R.id.account_server_label);
        mServerView = (EditText)findViewById(R.id.account_server);
        mPortView = (EditText)findViewById(R.id.account_port);
        mSecurityTypeView = (Spinner)findViewById(R.id.account_security_type);
        mAuthTypeView = (Spinner)findViewById(R.id.account_auth_type);
        mImapPathPrefixView = (EditText)findViewById(R.id.imap_path_prefix);
        mImapFolderDrafts = (Button)findViewById(R.id.account_imap_folder_drafts);
        mImapFolderSent = (Button)findViewById(R.id.account_imap_folder_sent);
        mImapFolderTrash = (Button)findViewById(R.id.account_imap_folder_trash);
        mImapFolderOutbox = (EditText)findViewById(R.id.account_imap_folder_outbox);
        mWebdavPathPrefixView = (EditText)findViewById(R.id.webdav_path_prefix);
        mWebdavAuthPathView = (EditText)findViewById(R.id.webdav_auth_path);
        mWebdavMailboxPathView = (EditText)findViewById(R.id.webdav_mailbox_path);
        mNextButton = (Button)findViewById(R.id.next);

        mImapFolderDrafts.setOnClickListener(this);
        mImapFolderSent.setOnClickListener(this);
        mImapFolderTrash.setOnClickListener(this);
        mNextButton.setOnClickListener(this);

        SpinnerOption securityTypes[] =
        {
            new SpinnerOption(0, getString(R.string.account_setup_incoming_security_none_label)),
            new SpinnerOption(1,
            getString(R.string.account_setup_incoming_security_ssl_optional_label)),
            new SpinnerOption(2, getString(R.string.account_setup_incoming_security_ssl_label)),
            new SpinnerOption(3,
            getString(R.string.account_setup_incoming_security_tls_optional_label)),
            new SpinnerOption(4, getString(R.string.account_setup_incoming_security_tls_label)),
        };

        // This needs to be kept in sync with the list at the top of the file.
        // that makes me somewhat unhappy
        SpinnerOption authTypeSpinnerOptions[] =
        {
            new SpinnerOption(0, "PLAIN"),
            new SpinnerOption(1, "CRAM_MD5")
        };

        ArrayAdapter<SpinnerOption> securityTypesAdapter = new ArrayAdapter<SpinnerOption>(this,
                android.R.layout.simple_spinner_item, securityTypes);
        securityTypesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSecurityTypeView.setAdapter(securityTypesAdapter);

        ArrayAdapter<SpinnerOption> authTypesAdapter = new ArrayAdapter<SpinnerOption>(this,
                android.R.layout.simple_spinner_item, authTypeSpinnerOptions);
        authTypesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mAuthTypeView.setAdapter(authTypesAdapter);

        /*
         * Updates the port when the user changes the security type. This allows
         * us to show a reasonable default which the user can change.
         */
        mSecurityTypeView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            public void onItemSelected(AdapterView arg0, View arg1, int arg2, long arg3)
            {
                updatePortFromSecurityType();
            }

            public void onNothingSelected(AdapterView<?> arg0)
            {
            }
        });

        /*
         * Calls validateFields() which enables or disables the Next button
         * based on the fields' validity.
         */
        TextWatcher validationTextWatcher = new TextWatcher()
        {
            public void afterTextChanged(Editable s)
            {
                validateFields();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
            }
        };
        mUsernameView.addTextChangedListener(validationTextWatcher);
        mPasswordView.addTextChangedListener(validationTextWatcher);
        mServerView.addTextChangedListener(validationTextWatcher);
        mPortView.addTextChangedListener(validationTextWatcher);

        /*
         * Only allow digits in the port field.
         */
        mPortView.setKeyListener(DigitsKeyListener.getInstance("0123456789"));

        String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
        mAccount = Preferences.getPreferences(this).getAccount(accountUuid);
        mMakeDefault = (boolean)getIntent().getBooleanExtra(EXTRA_MAKE_DEFAULT, false);

        /*
         * If we're being reloaded we override the original account with the one
         * we saved
         */
        if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_ACCOUNT))
        {
            accountUuid = savedInstanceState.getString(EXTRA_ACCOUNT);
            mAccount = Preferences.getPreferences(this).getAccount(accountUuid);
        }

        try
        {
            URI uri = new URI(mAccount.getStoreUri());
            String username = null;
            String password = null;
            String authType = null;

            if (uri.getUserInfo() != null)
            {
                String[] userInfoParts = uri.getUserInfo().split(":");
                if (userInfoParts.length == 3)
                {
                    authType = userInfoParts[0];
                    username = URLDecoder.decode(userInfoParts[1], "UTF-8");
                    password = URLDecoder.decode(userInfoParts[2], "UTF-8");
                }
                else if (userInfoParts.length == 2)
                {
                    username = URLDecoder.decode(userInfoParts[0], "UTF-8");
                    password = URLDecoder.decode(userInfoParts[1], "UTF-8");
                }
                else if (userInfoParts.length == 1)
                {
                    username = URLDecoder.decode(userInfoParts[0], "UTF-8");
                }
            }



            if (username != null)
            {
                mUsernameView.setText(username);
            }

            if (password != null)
            {
                mPasswordView.setText(password);
            }

            if (authType != null)
            {
                for (int i = 0; i < authTypes.length; i++)
                {
                    if (authTypes[i].equals(authType))
                    {
                        SpinnerOption.setSpinnerOptionValue(mAuthTypeView, i);
                    }
                }
            }


            mImapFolderDrafts.setText(mAccount.getDraftsFolderName());
            mImapFolderSent.setText(mAccount.getSentFolderName());
            mImapFolderTrash.setText(mAccount.getTrashFolderName());
            mImapFolderOutbox.setText(mAccount.getOutboxFolderName());

            if (uri.getScheme().startsWith("pop3"))
            {
                serverLabelView.setText(R.string.account_setup_incoming_pop_server_label);
                mAccountPorts = popPorts;
                mAccountSchemes = popSchemes;

                findViewById(R.id.imap_path_prefix_section).setVisibility(View.GONE);
                findViewById(R.id.imap_folder_setup_section).setVisibility(View.GONE);
                findViewById(R.id.webdav_path_prefix_section).setVisibility(View.GONE);
                findViewById(R.id.webdav_path_debug_section).setVisibility(View.GONE);
                findViewById(R.id.account_auth_type).setVisibility(View.GONE);
                mAccount.setDeletePolicy(Account.DELETE_POLICY_NEVER);


            }
            else if (uri.getScheme().startsWith("imap"))
            {
                serverLabelView.setText(R.string.account_setup_incoming_imap_server_label);
                mAccountPorts = imapPorts;
                mAccountSchemes = imapSchemes;

                if (uri.getPath() != null && uri.getPath().length() > 0)
                {
                    mImapPathPrefixView.setText(uri.getPath().substring(1));
                }
                findViewById(R.id.webdav_path_prefix_section).setVisibility(View.GONE);
                findViewById(R.id.webdav_path_debug_section).setVisibility(View.GONE);
                mAccount.setDeletePolicy(Account.DELETE_POLICY_ON_DELETE);

                if (! Intent.ACTION_EDIT.equals(getIntent().getAction()))
                {
                    findViewById(R.id.imap_folder_setup_section).setVisibility(View.GONE);
                }

            }
            else if (uri.getScheme().startsWith("webdav"))
            {
                serverLabelView.setText(R.string.account_setup_incoming_webdav_server_label);
                mAccountPorts = webdavPorts;
                mAccountSchemes = webdavSchemes;

                /** Hide the unnecessary fields */
                findViewById(R.id.imap_path_prefix_section).setVisibility(View.GONE);
                findViewById(R.id.account_auth_type).setVisibility(View.GONE);
                if (uri.getPath() != null && uri.getPath().length() > 0)
                {
                    String[] pathParts = uri.getPath().split("\\|");

                    for (int i = 0, count = pathParts.length; i < count; i++)
                    {
                        if (i == 0)
                        {
                            if (pathParts[0] != null &&
                                    pathParts[0].length() > 1)
                            {
                                mWebdavPathPrefixView.setText(pathParts[0].substring(1));
                            }
                        }
                        else if (i == 1)
                        {
                            if (pathParts[1] != null &&
                                    pathParts[1].length() > 1)
                            {
                                mWebdavAuthPathView.setText(pathParts[1]);
                            }
                        }
                        else if (i == 2)
                        {
                            if (pathParts[2] != null &&
                                    pathParts[2].length() > 1)
                            {
                                mWebdavMailboxPathView.setText(pathParts[2]);
                            }
                        }
                    }
                }
                mAccount.setDeletePolicy(Account.DELETE_POLICY_ON_DELETE);
            }
            else
            {
                throw new Exception("Unknown account type: " + mAccount.getStoreUri());
            }

            for (int i = 0; i < mAccountSchemes.length; i++)
            {
                if (mAccountSchemes[i].equals(uri.getScheme()))
                {
                    SpinnerOption.setSpinnerOptionValue(mSecurityTypeView, i);
                }
            }

            if (uri.getHost() != null)
            {
                mServerView.setText(uri.getHost());
            }

            if (uri.getPort() != -1)
            {
                mPortView.setText(Integer.toString(uri.getPort()));
            }
            else
            {
                updatePortFromSecurityType();
            }

            validateFields();
        }
        catch (Exception e)
        {
            failure(e);
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_ACCOUNT, mAccount.getUuid());
    }

    private void validateFields()
    {
        mNextButton
        .setEnabled(Utility.requiredFieldValid(mUsernameView)
                    && Utility.requiredFieldValid(mPasswordView)
                    && Utility.domainFieldValid(mServerView)
                    && Utility.requiredFieldValid(mPortView));
        Utility.setCompoundDrawablesAlpha(mNextButton, mNextButton.isEnabled() ? 255 : 128);
    }

    private void updatePortFromSecurityType()
    {
        if (mAccountPorts != null)
        {
            int securityType = (Integer)((SpinnerOption)mSecurityTypeView.getSelectedItem()).value;
            mPortView.setText(Integer.toString(mAccountPorts[securityType]));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode == RESULT_OK)
        {
            switch (requestCode)
            {
                case SELECT_DRAFT_FOLDER:
                    mImapFolderDrafts.setText(data.getStringExtra(ChooseFolder.EXTRA_NEW_FOLDER));
                    return;
                case SELECT_SENT_FOLDER:
                    mImapFolderSent.setText(data.getStringExtra(ChooseFolder.EXTRA_NEW_FOLDER));
                    return;
                case SELECT_TRASH_FOLDER:
                    mImapFolderTrash.setText(data.getStringExtra(ChooseFolder.EXTRA_NEW_FOLDER));
                    return;
            }
            if (Intent.ACTION_EDIT.equals(getIntent().getAction()))
            {
                mAccount.save(Preferences.getPreferences(this));
                finish();
            }
            else
            {
                /*
                 * Set the username and password for the outgoing settings to the username and
                 * password the user just set for incoming.
                 */
                try
                {
                    String usernameEnc = URLEncoder.encode(mUsernameView.getText().toString(), "UTF-8"); 
                    String passwordEnc = URLEncoder.encode(mPasswordView.getText().toString(), "UTF-8"); 
                    URI oldUri = new URI(mAccount.getTransportUri());
                    URI uri = new URI(
                        oldUri.getScheme(),
                        usernameEnc + ":" + passwordEnc,
                        oldUri.getHost(),
                        oldUri.getPort(),
                        null,
                        null,
                        null);
                    mAccount.setTransportUri(uri.toString());
                }
                catch (UnsupportedEncodingException enc)
                {
                    // This really shouldn't happen since the encoding is hardcoded to UTF-8
                    Log.e(K9.LOG_TAG, "Couldn't urlencode username or password.", enc);
                }
                catch (URISyntaxException use)
                {
                    /*
                     * If we can't set up the URL we just continue. It's only for
                     * convenience.
                     */
                }


                AccountSetupOutgoing.actionOutgoingSettings(this, mAccount, mMakeDefault);
                finish();
            }
        }
    }

    private void onNext()
    {
        try
        {
            int securityType = (Integer)((SpinnerOption)mSecurityTypeView.getSelectedItem()).value;
            String path = null;
            if (mAccountSchemes[securityType].startsWith("imap"))
            {
                path = "/" + mImapPathPrefixView.getText();
            }
            else if (mAccountSchemes[securityType].startsWith("webdav"))
            {
                path = "/" + mWebdavPathPrefixView.getText();
                path = path + "|" + mWebdavAuthPathView.getText();
                path = path + "|" + mWebdavMailboxPathView.getText();
            }

            final String userInfo;
            String user = mUsernameView.getText().toString();
            String password = mPasswordView.getText().toString();
            String userEnc = URLEncoder.encode(user, "UTF-8");        
            String passwordEnc = URLEncoder.encode(password, "UTF-8");

            if (mAccountSchemes[securityType].startsWith("imap"))
            {
                String authType = ((SpinnerOption)mAuthTypeView.getSelectedItem()).label;
                userInfo = authType + ":" + userEnc + ":" + passwordEnc;
            }
            else
            {
                userInfo = userEnc + ":" + passwordEnc;
            }
            URI uri = new URI(
                mAccountSchemes[securityType],
                userInfo,
                mServerView.getText().toString(),
                Integer.parseInt(mPortView.getText().toString()),
                path, // path
                null, // query
                null);
            mAccount.setStoreUri(uri.toString());


            mAccount.setDraftsFolderName(mImapFolderDrafts.getText().toString());
            mAccount.setSentFolderName(mImapFolderSent.getText().toString());
            mAccount.setTrashFolderName(mImapFolderTrash.getText().toString());
            mAccount.setOutboxFolderName(mImapFolderOutbox.getText().toString());
            AccountSetupCheckSettings.actionCheckSettings(this, mAccount, true, false);
        }
        catch (Exception e)
        {
            failure(e);
        }

    }

    public void onClick(View v)
    {
        try
        {
            switch (v.getId())
            {
                case R.id.next:
                    onNext();
                    break;
                case R.id.account_imap_folder_drafts:
                    selectImapFolder(SELECT_DRAFT_FOLDER);
                    break;
                case R.id.account_imap_folder_sent:
                    selectImapFolder(SELECT_SENT_FOLDER);
                    break;
                case R.id.account_imap_folder_trash:
                    selectImapFolder(SELECT_TRASH_FOLDER);
                    break;
            }
        }
        catch (Exception e)
        {
            failure(e);
        }
    }

    private void selectImapFolder(int activityCode)
    {
        String curFolder = null;
        boolean showFolderNone = false;
        switch (activityCode)
        {
            case SELECT_DRAFT_FOLDER:
                curFolder = mImapFolderDrafts.getText().toString();
                break;
            case SELECT_SENT_FOLDER:
                curFolder = mImapFolderSent.getText().toString();
                showFolderNone = true;
                break;
            case SELECT_TRASH_FOLDER:
                curFolder = mImapFolderTrash.getText().toString();
                showFolderNone = true;
                break;
            default:
                throw new IllegalArgumentException(
                    "Cannot select folder for: " + activityCode);
        }

        Intent selectIntent = new Intent(this, ChooseFolder.class);
        String uri = mAccount.getStoreUri();
        if (showFolderNone && uri.startsWith("imap"))
        {
            selectIntent.putExtra(ChooseFolder.EXTRA_SHOW_FOLDER_NONE, "yes");
        }
        selectIntent.putExtra(ChooseFolder.EXTRA_ACCOUNT, mAccount.getUuid());
        selectIntent.putExtra(ChooseFolder.EXTRA_CUR_FOLDER, curFolder);
        selectIntent.putExtra(ChooseFolder.EXTRA_SHOW_CURRENT, "yes");
        startActivityForResult(selectIntent, activityCode);
    }

    private void failure(Exception use)
    {
        Log.e(K9.LOG_TAG, "Failure", use);
        String toastText = getString(R.string.account_setup_bad_uri, use.getMessage());

        Toast toast = Toast.makeText(getApplication(), toastText, Toast.LENGTH_LONG);
        toast.show();
    }
}
