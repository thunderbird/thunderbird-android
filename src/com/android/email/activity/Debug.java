
package com.android.email.activity;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.android.email.Email;
import com.android.email.Preferences;
import com.android.email.R;

import java.lang.Integer;

public class Debug extends Activity implements OnCheckedChangeListener {
    private TextView mVersionView;
    private CheckBox mEnableDebugLoggingView;
    private CheckBox mEnableSensitiveLoggingView;

    private Preferences mPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.debug);

        mPreferences = Preferences.getPreferences(this);

        mVersionView = (TextView)findViewById(R.id.version);
        mEnableDebugLoggingView = (CheckBox)findViewById(R.id.debug_logging);
        mEnableSensitiveLoggingView = (CheckBox)findViewById(R.id.sensitive_logging);

        mEnableDebugLoggingView.setOnCheckedChangeListener(this);
        mEnableSensitiveLoggingView.setOnCheckedChangeListener(this);
	
        mVersionView.setText(String.format(getString(R.string.debug_version_fmt).toString(), getVersionNumber()));

        mEnableDebugLoggingView.setChecked(Email.DEBUG);
        mEnableSensitiveLoggingView.setChecked(Email.DEBUG_SENSITIVE);
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView.getId() == R.id.debug_logging) {
            Email.DEBUG = isChecked;
            mPreferences.setEnableDebugLogging(Email.DEBUG);
        } else if (buttonView.getId() == R.id.sensitive_logging) {
            Email.DEBUG_SENSITIVE = isChecked;
            mPreferences.setEnableSensitiveLogging(Email.DEBUG_SENSITIVE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.dump_settings) {
            Preferences.getPreferences(this).dump();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.debug_option, menu);
        return true;
    }

    private String getVersionNumber() {
	String version = "?";
	int vnum;

	try {
	    PackageInfo pi = 
		getPackageManager().getPackageInfo(getPackageName(), 0);
	    version = Integer.toString(pi.versionCode);
	    
	} catch (PackageManager.NameNotFoundException e){
	    Log.e(Email.LOG_TAG, "Package name not found: " + e.getMessage());
	};
	return version;
    }
}
