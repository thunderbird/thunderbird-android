package com.fsck.k9.activity.setup;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.*;
import android.os.Process;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.activity.setup.configxmlparser.ConfigurationXMLHandler;
import com.fsck.k9.mail.store.TrustManagerFactory;
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

/**
 * User: dzan
 * Date: 30/06/11
 */

public class AccountSetupAutoConfiguration extends K9Activity implements View.OnClickListener {

    // constants for the intent/activity system
    // TODO: read about intents and make this one result right
    public static final int ACTIVITY_REQUEST_CODE = 1;
    private static final String EMAIL_ADDRESS = "account";
    private static final String PASSWORD = "password";

    // timeout for testing services availability ( in ms )
    private static final int TIMEOUT = 20000;

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
    private String mEmailAddress;
    private String mPassword;
    private String mLastMessage;

    /*
        Start the auto-configuration activity
     */
    public static void actionAttemptConfiguration(Activity context, String email, String password) {
        Intent i = new Intent(context, AccountSetupAutoConfiguration.class);
        i.putExtra(EMAIL_ADDRESS, email);
        i.putExtra(PASSWORD, password);
        context.startActivityForResult(i, ACTIVITY_REQUEST_CODE);
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
//        ((Button)findViewById(R.id.cancel)).setOnClickListener(this);

        // Getting our data to work with
        mEmailAddress = getIntent().getStringExtra(EMAIL_ADDRESS);
        mPassword = getIntent().getStringExtra(PASSWORD);

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
                    First part: check if serverside configuration data exists
                 */
                int i = 0;
                while( i < urlTemplates.size() ){
                    try{
                        // inform the user
                        setMessage(urlInfoStatements.get(i),true);

                        // preparing the urls
                        if( !domain.contains("%user%") ){ // else SHIT
                            tmpURL = urlTemplates.get(i).replaceAll("%domain%",domain);
                            tmpURL = tmpURL.replaceAll("%address%",mEmailAddress);
                        }

                        data = getXMLData(new URL(tmpURL));

                        if( !data.isEmpty() ){
                            setMessage(R.string.account_setup_autoconfig_found,false);

                            // parse and finish
                            // remember if i >= UNSAFE_URL_START => POSSIBLE UNSAFE DATA, alert user!!!
                            parse(data);
                            finish();
                            return;
                        }

                        // end the output if needed
                        closeUserInformationIfNeeded(i);

                    }catch (SocketTimeoutException ex){
                        Log.e(K9.LOG_TAG, "Error while attempting auto-configuration with url '"+
                                tmpURL+"' ( time-out is"+TIMEOUT+" )", ex);
                    }catch (MalformedURLException ex){
                        Log.e(K9.LOG_TAG, "Error while attempting auto-configuration with url '"+
                                tmpURL+"'", ex);
                    }catch (UnknownHostException ex){
                        Log.e(K9.LOG_TAG, "Error while attempting auto-configuration with url '"+
                                tmpURL+"'", ex);
                    }
                    catch (SSLPeerUnverifiedException ex){
                        Log.e(K9.LOG_TAG, "Error while testing settings", ex);
                        acceptKeyDialog(
                            R.string.account_setup_failed_dlg_certificate_message_fmt,i,ex);
                    }
                    catch(IOException ex) {
                        Log.e(K9.LOG_TAG, "Error while attempting auto-configuration with url '"+
                                tmpURL+"'", ex);
                    } catch (ErrorCodeException ex) {
                        Log.e(K9.LOG_TAG, "Error while attempting auto-configuration with url '" +
                                tmpURL + "' site didn't respond as expected. Got code: " + ex.getErrorCode(), ex);
                    } finally {
                        // might be the user cancelled by now or the app was destroyed
                        if (mDestroyed) return;
                        if (mCanceled) { finish(); return; }

                        // check next url
                        ++i;
                    }

                    // no server-side config was found
                    closeUserInformationIfNeeded(i-1);
                }
            }
        }
        .start();
    }

    /*
        Start parsing the xml
     */
    private void parse(String data) {
        try{
            XMLReader xr = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
            xr.setContentHandler(new ConfigurationXMLHandler());
            //xr.parse(data);

        // TODO: take care of these
        } catch (ParserConfigurationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (SAXException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    /*
        Checks if an url is available / exists
     */
    private String getXMLData(URL url) throws IOException, ErrorCodeException{
        // think we should assume no redirects
        // TODO: use k9's own TrustManager so the right exception gets throwed and the user can choose to accept the certificate
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setInstanceFollowRedirects(false);

        // prepare request
        conn.setConnectTimeout(TIMEOUT);

        // get the data
        String tmp, line;
        tmp = "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        while( (line = reader.readLine()) != null)
            tmp += line;

        if( conn.getResponseCode() != 200 )
            throw new ErrorCodeException("Server did not return as expected.",conn.getResponseCode());
        conn.disconnect();
        return tmp;
    }

    /*
        Adds messages to the view, provides the user with progress reports
     */
    private void setMessage(int resId, final boolean newLine){
        setMessage(getString(resId), newLine);
    }
    private void setMessage(final String msg, final boolean newline) {
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
        int a = urlInfoStatements.get(urlNumb);
        int b = urlInfoStatements.get(urlNumb+1);
        if( urlNumb == urlInfoStatements.size() - 1 || a != b ){
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

    @Override
    public void onActivityResult(int reqCode, int resCode, Intent data) {
        setResult(resCode);
        finish();
    }

    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.cancel:
            onCancel();
            break;
        }
    }

    /*
        Ask the user to accept ssl certificates if they are not trusted already.
        TODO: Rework this so it changes the url counter, not restart intent
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
                                        AccountSetupAutoConfiguration.actionAttemptConfiguration(AccountSetupAutoConfiguration.this, mEmailAddress, mPassword);
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
