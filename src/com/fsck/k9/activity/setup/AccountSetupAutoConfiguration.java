package com.fsck.k9.activity.setup;

import android.app.Activity;
import android.content.Intent;
import android.os.*;
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

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    private String dnsMXLookupUrl = "https://live.mozillamessaging.com/dns/mx/";

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

        // inform user we start autoconfig
		setMessage(R.string.account_setup_autoconfig_info, true);

        // divide the address
        String[] emailParts = splitEmail(mEmailAddress);
        String user = emailParts[0];
        String domain = emailParts[1];

        // The real action, in a separate thread
        new AutoConfigurationThread(domain).start();
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
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        tmp = "";

        try{
            while( (line = reader.readLine()) != null)
                tmp += line;

            if( conn.getResponseCode() != 200 )
                throw new ErrorCodeException("Server did not return as expected.",conn.getResponseCode());
       }
        catch (SocketTimeoutException ex)
            { throw ex; }
        catch (ConnectException ex){
		// ignore this, it just means the url doesn't exist which happens often, we test for it!
        }
        finally{ 
        	reader.close();
        	conn.disconnect(); 
        }

        return tmp;
    }


    /*
     * Does an DNS MX lookup of the domain and returns a list of records.
     * This uses the Mozilla webservice to do so.
     */
    private List<String> doMXLookup(String domain) throws IOException{
        HttpClient httpclient = new DefaultHttpClient();
        HttpGet method = new HttpGet(dnsMXLookupUrl + domain);

        // do request
        HttpResponse response = httpclient.execute(method);
	String data = EntityUtils.toString(response.getEntity());
	return new ArrayList<String>(Arrays.asList(data.split("[\\r\\n]+")));
    }


    /*
     * Does two things:
     * 	1. Detect the isp-domain parts of the mxServer hostnames
     *  2. Filters these so we have a list of uniques also not containing the initial domainname used for mx lookup
     *
     *  TODO:
     *  	Speed this up! There are a lot of options...
     *  	Use a Set instead of a list, don't rebuild tmpStr every time,...
     */
    private List<String> getDomainPossibilities(List<String> mxServers, String origDomain){
	List<String> filteredDomains = new ArrayList<String>();
	String[] serverSplit;
	String tmpStr, prevAtom;
	int size = 0; // total atoms in server hostname
	// number of atoms in last found domain
	int parts = 2; // to begin with, minimum so never arrayoutofbound

	for( String server : mxServers )
	{
		serverSplit = server.toLowerCase().split("\\.");
		prevAtom = serverSplit[0];

		// speed things up a bit ( ugly )
		size = serverSplit.length;
		tmpStr = "";
		for( int k=size-1; k>(size-parts-1); --k) tmpStr += "."+serverSplit[k];
		tmpStr = tmpStr.substring(1);
		if( filteredDomains.contains(tmpStr)) continue;

		// determine right domainname
		for( int i = 1; i < serverSplit.length; ++i ){
			// build domainstring to test
			tmpStr = "";
			for( int j=i; j<serverSplit.length; ++j) tmpStr += "."+serverSplit[j];
			tmpStr = tmpStr.substring(1);

			// we matched a as-wide-as-possible tld
			if(com.fsck.k9.helper.Regex.TOP_LEVEL_DOMAIN_PATTERN.matcher(tmpStr).matches()){
				tmpStr = prevAtom + "." + tmpStr;
				size = 1 + (size - i);
				if( !tmpStr.equals(origDomain) && !filteredDomains.contains(tmpStr) )
					filteredDomains.add(tmpStr);
			}

			prevAtom = serverSplit[i];
		}
	}

	return filteredDomains;
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

    private synchronized void closeUserInformationIfNeeded(int urlNumb) {
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
   /* private void acceptKeyDialog(final int msgResId, final int urlNumber, final Object... args) {
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
    }*/


    /*
     * Thread class to do the autoconfiguration
     */
    private class AutoConfigurationThread extends Thread{

	private String mDomain;
		private List<String> mDomainAlternatives;

	public AutoConfigurationThread(String domain) {
		super();
		this.mDomain = domain;
	}

	@Override
	public void run() {
		//android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

		// declare some variables
		String data = "";    // used to store downloaded xml before parsing
		String tmpURL = "";

		int templateIndex = 0;
		while( templateIndex < urlTemplates.size() && !bFound ){
			try{
				// inform the user
				setMessage(urlInfoStatements.get(templateIndex),true);

				// to make sure
				bParseFailed = false;
				bForceManual = false;
				bDoneSearching = false;

				// preparing the urls
				if( !mDomain.contains("%user%") ){ // else SHIT
					tmpURL = urlTemplates.get(templateIndex).replaceAll("%domain%",mDomain);
					tmpURL = tmpURL.replaceAll("%address%",mEmailAddress);
				}

				// get the xml data
				data = getXMLData(new URL(tmpURL));

				// might be the user cancelled by now or the app was destroyed
				if (mDestroyed) return;
				if (mCanceled) { finish(); return; }

				// if we really have data
				if( !data.isEmpty() ){
					setMessage(R.string.account_setup_autoconfig_found,false);

					// parse and finish
					setMessage(R.string.account_setup_autoconfig_processing,true);
					parse(data);
					setMessage(R.string.account_setup_autoconfig_succesful,false);

					// alert user these settings might be tampered with!!! ( no https )
					if( templateIndex >= UNSAFE_URL_START ) bUnsafe = true;

					bFound = true;
					continue;
				}

			}catch (SocketTimeoutException ex){
				Log.v(K9.LOG_TAG, "Error while attempting auto-configuration with url '"+
						tmpURL+"' ( time-out is"+TIMEOUT+" )", ex);
			}catch (MalformedURLException ex){
				Log.v(K9.LOG_TAG, "Error while attempting auto-configuration with url '"+
						tmpURL+"'", ex);
			}catch (UnknownHostException ex){
				Log.v(K9.LOG_TAG, "Error while attempting auto-configuration with url '"+
						tmpURL+"'", ex);
			}catch (SSLPeerUnverifiedException ex){
				Log.v(K9.LOG_TAG, "Error while testing settings", ex);
				// TODO: use custom trust manager so this exception could get thrown
				//acceptKeyDialog(R.string.account_setup_failed_dlg_certificate_message_fmt,i,ex);
			}catch (SAXException e) {
				setMessage(R.string.account_setup_autoconfig_fail,false);
				bParseFailed = true;
			}catch (ParserConfigurationException e) {
				setMessage(R.string.account_setup_autoconfig_fail,false);
				bParseFailed = true;
			}catch (ErrorCodeException ex) {
				Log.v(K9.LOG_TAG, "Error while attempting auto-configuration with url '" +
						tmpURL + "' site didn't respond as expected. Got code: " + ex.getErrorCode(), ex);
			}catch(IOException ex) {
				Log.e(K9.LOG_TAG, "Error while attempting auto-configuration with url '"+
						tmpURL+"'", ex);
			} finally {
				// might be the user cancelled by now or the app was destroyed
				if (mDestroyed) return;
				if (mCanceled) { finish(); return; }

				// check next url
				++templateIndex;

				if( !bFound ) closeUserInformationIfNeeded(templateIndex-1);

				// did parsing fail? tell user
				if( bParseFailed )
					setMessage(R.string.account_setup_autoconfig_trynext,true);

				// this is the last domain to try in the list
				if( templateIndex == urlTemplates.size()){
					// we can still try DNS MX
					if( mDomainAlternatives == null ){
						try {
							setMessage(R.string.account_setup_autoconfig_trydns, true);
							mDomainAlternatives = getDomainPossibilities(doMXLookup(mDomain), mDomain);
							if( mDomainAlternatives.size() > 0 )
								setMessage(R.string.account_setup_autoconfig_found, false);
							else
								setMessage(R.string.account_setup_autoconfig_missing, false);
						} catch (IOException e) {
							mDomainAlternatives = new ArrayList<String>(); // setting empty list = no options left
							setMessage(R.string.account_setup_autoconfig_missing, false);
							Log.e(K9.LOG_TAG, "Error while getting DNS MX data in autoconfiguration", e);
						}
					}

					// still domains remaining to try, restart whole lookup with new domain
					if( mDomainAlternatives.size() > 0 ){
						mDomain = mDomainAlternatives.get(0);
						mDomainAlternatives.remove(0);
						templateIndex = 0;
						setMessage(R.string.account_setup_autoconfig_new_domain, true);
					// out of options... manual configuration
					}else{
						bForceManual = true;
						setMessage(R.string.account_setup_autoconfig_forcemanual, true);
					}

				}
			}
		}

		// remember we've searched already
		bDoneSearching = true;

		// update ui state
		runOnUiThread(new Runnable() {
			public void run() {
				// hide progress circle & enable button for any case
				mProgressCircle.setVisibility(View.INVISIBLE);
				mNextButton.setEnabled(true);

				// 1. All good, continue
				// all is fine

				// 2. Nothing came up, must manually config.. + help?
				if( bForceManual ) mNextButton.setText(getString(R.string.account_setup_basics_manual_setup_action));

				// 3. Data did not came over HTTPS this could be UNSAFE !!!!!!
				if( bUnsafe ) mWarningMsg.setVisibility(View.VISIBLE);
			}
		});
	}
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
