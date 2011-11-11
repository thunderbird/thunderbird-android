package com.fsck.k9.activity.setup;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.*;
import android.os.Process;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.helper.configxmlparser.AutoconfigInfo;
import com.fsck.k9.helper.configxmlparser.ConfigurationXMLHandler;
import com.fsck.k9.mail.store.TrustManagerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.net.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

/**
 * User: dzan
 * Date: 30/06/11
 */

public class AccountSetupAutoConfiguration extends K9Activity implements View.OnClickListener {

    private static final String EMAIL_ADDRESS = "account";
    private static final String PASSWORD = "password";
    private static final String MAKEDEFAULT = "default";

    // timeout for testing services availability ( in ms )
    private static final int TIMEOUT = 5000;

    // location of mozilla's ispdb
    private String databaseBaseUrl = "https://live.mozillamessaging.com/autoconfig/v1.1/";

    // for now there are only 2 so I just hardcode them here
    // also note: order they are listed is the order they'll be checked
    // info: https://developer.mozilla.org/en/Thunderbird/Autoconfiguration
    private ArrayList<String> urlTemplates = new ArrayList<String>(Arrays.asList(
        "https://%domain%/.well-known/autoconfig/mail/config-v1.1.xml",
        "https://autoconfig.%domain%/mail/config-v1.1.xml?emailaddress=%address%",
        databaseBaseUrl+"%domain%",
        "http://%domain%/.well-known/autoconfig/mail/config-v1.1.xml",
        "http://autoconfig.%domain%/mail/config-v1.1.xml?emailaddress=%address%"));

    // info matching the urls above
    private ArrayList<Integer> urlInfoStatements = new ArrayList<Integer>(Arrays.asList(
        R.string.account_setup_autoconfig_test_safe_serverside,
        R.string.account_setup_autoconfig_test_safe_serverside,
        R.string.account_setup_autoconfig_test_ispdb,
        R.string.account_setup_autoconfig_test_unsafe_serverside,
        R.string.account_setup_autoconfig_test_unsafe_serverside
    ));

    // marks the beginning of unsafe urls
    private final int UNSAFE_URL_START = 3;

    // our hook in the gui thread
    private Handler mHandler = new Handler();

    // events that affect the thread
    private boolean mCanceled;
    private boolean mDestroyed;

    private TextView mMessageView;
    private ProgressBar mProgressCircle;
    private Button mCancelButton;
    private Button mNextButton;
    private TextView mWarningMsg;

    private String mEmailAddress;
    private String mPassword;
    private String mLastMessage;
    private boolean mMakeDefault;
    private AutoconfigInfo mAutoConfigInfo;
    private boolean bForceManual = false;
    private boolean bDoneSearching = false;
    private boolean bFound = false;
    private boolean bParseFailed = false;
    private boolean bUnsafe = false;

    /*
        Start the auto-configuration activity
     */
    public static void actionAttemptConfiguration(Activity context, String email, String password, boolean makedefault) {
        Intent i = new Intent(context, AccountSetupAutoConfiguration.class);
        i.putExtra(EMAIL_ADDRESS, email);
        i.putExtra(PASSWORD, password);
        i.putExtra(MAKEDEFAULT, makedefault);
        context.startActivity(i);
    }

    /*
        'Constructor'; Most of the logic is in here.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setting up the view
        setContentView(R.layout.account_setup_autoconfig);
        mMessageView = (TextView)findViewById(R.id.status_message);
        mWarningMsg = (TextView)findViewById(R.id.autoconfig_warning);
        mWarningMsg.setVisibility(View.INVISIBLE);
        mProgressCircle = (ProgressBar)findViewById(R.id.autoconfig_progress);
        mProgressCircle.setIndeterminate(true);
        mProgressCircle.setVisibility(View.VISIBLE);

        mCancelButton = (Button)findViewById(R.id.autoconfig_button_cancel);
        mCancelButton.setOnClickListener(this);
        mNextButton = (Button)findViewById(R.id.autoconfig_button_next);
        mNextButton.setOnClickListener(this);
        mNextButton.setEnabled(false);

        // Getting our data to work with
        mEmailAddress = getIntent().getStringExtra(EMAIL_ADDRESS);
        mPassword = getIntent().getStringExtra(PASSWORD);
        mMakeDefault = getIntent().getBooleanExtra(MAKEDEFAULT, false);

        // The real action, in a separate thread
        new Thread() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

                // declare some variables
                String data = "";    // used to store downloaded xml before parsing
                String tmpURL = "";

                // notify the user we'll get started
                setMessage(R.string.account_setup_autoconfig_info, true);

                // divide the address
                String[] emailParts = splitEmail(mEmailAddress);
                String user = emailParts[0];
                String domain = emailParts[1];

                /*
                    Check if configuration data exists and if it does read in
                 */
                int i = 0;
                while( i < urlTemplates.size() && !bFound ){
                    try{
                        // inform the user
                        setMessage(urlInfoStatements.get(i),true);

                        // to make sure
                        bParseFailed = false;
                        bForceManual = false;
                        bDoneSearching = false;

                        // preparing the urls
                        if( !domain.contains("%user%") ){ // else SHIT
                            tmpURL = urlTemplates.get(i).replaceAll("%domain%",domain);
                            tmpURL = tmpURL.replaceAll("%address%",mEmailAddress);
                        }

                        data = getXMLData(new URL(tmpURL));

                        // might be the user cancelled by now or the app was destroyed
                        if (mDestroyed) return;
                        if (mCanceled) { finish(); return; }

                        if( !data.isEmpty() ){
                            setMessage(R.string.account_setup_autoconfig_found,false);

                            // parse and finish
                            setMessage(R.string.account_setup_autoconfig_processing,true);
                            parse(data);
                            setMessage(R.string.account_setup_autoconfig_succesful,false);

                            // alert user these settings might be tampered with!!! ( no https )
                            if( i >= UNSAFE_URL_START ) bUnsafe = true;

                            bFound = true;
                            continue;
                        }

                    }catch (SocketTimeoutException ex){
                        Log.e(K9.LOG_TAG, "Error while attempting auto-configuration with url '"+
                                tmpURL+"' ( time-out is"+TIMEOUT+" )", ex);
                    }catch (MalformedURLException ex){
                        Log.e(K9.LOG_TAG, "Error while attempting auto-configuration with url '"+
                                tmpURL+"'", ex);
                    }catch (UnknownHostException ex){
                        Log.e(K9.LOG_TAG, "Error while attempting auto-configuration with url '"+
                                tmpURL+"'", ex);
                    }catch (SSLPeerUnverifiedException ex){
                        Log.e(K9.LOG_TAG, "Error while testing settings", ex);
                        acceptKeyDialog(R.string.account_setup_failed_dlg_certificate_message_fmt,i,ex);
                    }catch (SAXException e) {
                        setMessage(R.string.account_setup_autoconfig_fail,false);
                        bParseFailed = true;
                    }catch (ParserConfigurationException e) {
                        setMessage(R.string.account_setup_autoconfig_fail,false);
                        bParseFailed = true;
                    }catch (ErrorCodeException ex) {
                        Log.e(K9.LOG_TAG, "Error while attempting auto-configuration with url '" +
                                tmpURL + "' site didn't respond as expected. Got code: " + ex.getErrorCode(), ex);
                    }catch(IOException ex) {
                        Log.e(K9.LOG_TAG, "Error while attempting auto-configuration with url '"+
                                tmpURL+"'", ex);
                    } finally {
                        // might be the user cancelled by now or the app was destroyed
                        if (mDestroyed) return;
                        if (mCanceled) { finish(); return; }

                        // check next url
                        ++i;

                        // this was the last option..
                        if(i == urlTemplates.size() ){
                            bForceManual = true;
                            setMessage(R.string.account_setup_autoconfig_forcemanual, true);
                        }else{
                            if( bParseFailed )
                                setMessage(R.string.account_setup_autoconfig_trynext,true);
                        }
                    }

                    // no server-side config was found
                    closeUserInformationIfNeeded(i-1);
                }

                bDoneSearching = true;
                runOnUiThread(new Runnable() {
                    public void run() {
                        // TODO: set appropriate warning messages in here
                        // 1. All good, continue
                        // 2. Nothing came up, must manually config.. + help?
                        // 3. Data did not came over HTTPS this could be UNSAFE !!!!!!
                        mProgressCircle.setVisibility(View.INVISIBLE);
                        if( bUnsafe /*&& !bForceManual*/ ) mWarningMsg.setVisibility(View.VISIBLE);
                        mNextButton.setEnabled(true);
                        if( bForceManual )
                            mNextButton.setText(getString(R.string.account_setup_basics_manual_setup_action));
                        }
                 });

            }
        }
        .start();
    }


    /*
        Start parsing the xml
     */
    private void parse(String data) throws IOException, SAXException, ParserConfigurationException {
        ConfigurationXMLHandler parser = new ConfigurationXMLHandler();
        XMLReader xr = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
        xr.setContentHandler(parser);
        // TODO: see if this has performance consequences, otherwise change all so we pass around InputSource not string
        xr.parse(new InputSource(new StringReader(data)));
        mAutoConfigInfo = parser.getAutoconfigInfo();
    }

    /*
        Checks if an url is available / exists
     */
    private String getXMLData(URL url) throws IOException, ErrorCodeException, SocketTimeoutException {
        // think we should assume no redirects
        // TODO: use k9's own TrustManager so the right exception gets throwed and the user can choose to accept the certificate
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setInstanceFollowRedirects(false);

        // prepare request
        conn.setConnectTimeout(TIMEOUT);

        // get the data
        String tmp, line;
        BufferedReader reader;
        tmp = "";

        try{
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            while( (line = reader.readLine()) != null)
                tmp += line;

            if( conn.getResponseCode() != 200 )
                throw new ErrorCodeException("Server did not return as expected.",conn.getResponseCode());
       }
        catch (SocketTimeoutException ex)
            { throw ex; }
        finally
            { conn.disconnect(); }

        return tmp;
    }

    /*
        Adds messages to the view, provides the user with progress reports
     */
    private void setMessage(int resId, final boolean newLine){
        setMessage(getString(resId), newLine);
    }
    private synchronized void setMessage(final String msg, final boolean newline) {
        mHandler.post(new Runnable() {
            public void run() {
                // don't print if same as last message or when process should be destroyed
                if (mDestroyed || mLastMessage == msg ) { return; }

                // build the new content of the textview
                String current = mMessageView.getText().toString();
                if(newline) current+='\n'+msg;
                else current += msg;

                // set content & update last message
                mMessageView.setText(current);
                mLastMessage = msg;
            }
        });
    }

    private void closeUserInformationIfNeeded(int urlNumb) {
        if( bForceManual ) return;

        if( urlNumb == urlInfoStatements.size() - 1
                || (int)urlInfoStatements.get(urlNumb) != urlInfoStatements.get(urlNumb+1) ){
            mHandler.post(new Runnable() {
                public void run() {
                    mMessageView.setText(mMessageView.getText().toString()+
                    getString(R.string.account_setup_autoconfig_missing));
                }
            });
        }
    }
    /*
        We stop our thread
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        mDestroyed = true;
        mCanceled = true;
    }

    /*
        Alert user he canceled and stop the thread
     */
    private void onCancel() {
        if( bDoneSearching ){
            finish();
            return;
        }
        mCanceled = true;
        setMessage(R.string.account_setup_check_settings_canceling_msg, true);
    }

    /*
        Splits an email address in domain and username parts
     */
    private String[] splitEmail(String email) {
        String[] retParts = new String[2];
        String[] emailParts = email.split("@");
        retParts[0] = (emailParts.length > 0) ? emailParts[0] : "";
        retParts[1] = (emailParts.length > 1) ? emailParts[1] : "";
        return retParts;
    }

    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.autoconfig_button_cancel:
            onCancel();
            break;
        case R.id.autoconfig_button_next:
            // autoconfig failed, proceed by manual setup
            if( bForceManual ){
		// TODO: get boolean from user
                AccountSetupAccountType.actionStartManualConfiguration(this, mEmailAddress, mPassword, mMakeDefault);
                finish();
            // launch confirm activities
            }else{
                AccountSetupConfirmIncoming.actionConfirmIncoming
                        (this, null, mEmailAddress, mPassword, mAutoConfigInfo,mMakeDefault);
                finish();
            }
            break;
        default: return;
        }
    }

    /*
        Ask the user to accept ssl certificates if they are not trusted already.
        TODO: Rework this so it changes the url counter, not restart intent
        NOTE: It's called but doesn't work right now because for the connection the default sslfactory is yet used
     */
    private void acceptKeyDialog(final int msgResId, final int urlNumber, final Object... args) {
        mHandler.post(new Runnable() {
            public void run() {
                if (mDestroyed) {
                    return;
                }
                final X509Certificate[] chain = TrustManagerFactory.getLastCertChain();
                String exMessage = "Unknown Error";

                Exception ex = ((Exception) args[0]);
                if (ex != null) {
                    if (ex.getCause() != null) {
                        if (ex.getCause().getCause() != null) {
                            exMessage = ex.getCause().getCause().getMessage();

                        } else {
                            exMessage = ex.getCause().getMessage();
                        }
                    } else {
                        exMessage = ex.getMessage();
                    }
                }

                StringBuffer chainInfo = new StringBuffer(100);
                for (int i = 0; i < chain.length; i++) {
                    // display certificate chain information
                    chainInfo.append("Certificate chain[" + i + "]:\n");
                    chainInfo.append("Subject: " + chain[i].getSubjectDN().toString() + "\n");
                    chainInfo.append("Issuer: " + chain[i].getIssuerDN().toString() + "\n");
                }

                new AlertDialog.Builder(AccountSetupAutoConfiguration.this)
                        .setTitle(getString(R.string.account_setup_failed_dlg_invalid_certificate_title))
                                //.setMessage(getString(R.string.account_setup_failed_dlg_invalid_certificate)
                        .setMessage(getString(msgResId, exMessage) + " " + chainInfo.toString())
                        .setCancelable(true)
                        .setPositiveButton(getString(R.string.account_setup_failed_dlg_invalid_certificate_accept),

                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        try {
                                            // NOTE: the first argument is never used...
                                            TrustManagerFactory.addCertificateChain(mEmailAddress + urlTemplates.get(urlNumber), chain);
                                        } catch (CertificateException e) {
                                            setMessage(getString(R.string.account_setup_failed_dlg_certificate_message_fmt,
                                                    e.getMessage() == null ? "" : e.getMessage()), true);
                                        }
                                        // TODO: rework this so we just retry the last URL, DO NOT RESTART THE WHOLE ACTIVITY!!
                                        AccountSetupAutoConfiguration.actionAttemptConfiguration(AccountSetupAutoConfiguration.this, mEmailAddress, mPassword, mMakeDefault);
                                    }
                                })
                        .setNegativeButton(
                                getString(R.string.account_setup_failed_dlg_invalid_certificate_reject),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                    }
                                })
                        .show();
            }
        });
    }


    /*
        Small custom exception to pass http response codes around
     */
    private class ErrorCodeException extends Exception {
        private int errorCode;
        public ErrorCodeException(String msg, Integer code){
            super(msg);
            errorCode = code;
        }
        public int getErrorCode(){return errorCode;}
    }
}
