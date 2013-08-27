package com.fsck.k9.helper;

import android.os.Environment;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.preferences.SettingsExporter;
import com.fsck.k9.preferences.SettingsImportExportException;

import org.acra.ReportField;
import org.acra.collector.CrashReportData;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/***
 * Customer Report sender which writes ACRA reports into files on the SD card (default:
 * sdcard/com.fsck.k9/crash-yyyy-MM-dd_HHmmss)
 */
public class AcraCustomFileSender implements ReportSender{

    /**
     * folder where to store the log reports (each exception creates on file)
     * folder MUST be user accessable to enable to user to retrieve the logfiles
     */
    private File logFolder;
    
    
    public AcraCustomFileSender(File folder) {
        super();
        this.logFolder = folder;
    }


    @Override
    public void send(CrashReportData data) throws ReportSenderException {

        //See: https://github.com/ACRA/acra/wiki/ReportContent
        ReportField[] includeFields = {
            ReportField.APP_VERSION_NAME,
            ReportField.ANDROID_VERSION,
            ReportField.AVAILABLE_MEM_SIZE,
            ReportField.TOTAL_MEM_SIZE,
            ReportField.PHONE_MODEL,
            ReportField.BRAND,
            ReportField.PRODUCT,
            ReportField.THREAD_DETAILS,
            ReportField.USER_APP_START_DATE,
            ReportField.USER_CRASH_DATE,
            ReportField.ENVIRONMENT,
            ReportField.SETTINGS_SECURE,
            ReportField.CRASH_CONFIGURATION
        };
        
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // TODO we can not write our log
            Log.w(K9.LOG_TAG, "can't write error log -> sd path "+ logFolder + " readonly ");
            return;
        }
        
        logFolder.mkdirs();
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US);
        String filename = "crash-"+sdf.format(new Date());
        File file = new File(logFolder, filename);
        
        try {
            FileOutputStream f = new FileOutputStream(file);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(f));
            
            bw.write("#### k9 generated LOGFILE");
            bw.newLine();
            bw.write("generated:" + filename);
            bw.newLine();
            for(ReportField field : includeFields) {
                bw.write(field.toString()+":"+data.getProperty(field));
                bw.newLine();
            }
            
            bw.newLine();
            bw.write("#### AccountSettings"); 
            bw.newLine();
            try {
                bw.write(getAccountSettings(listAccountUUids()));
                bw.newLine();
            } catch (SettingsImportExportException e) {
                bw.write("can not get account settings for crash log");
                Log.e(K9.LOG_TAG,"can not get account settings for crash log",e);
            }
            
            bw.newLine();
            bw.write("#### Exception");
            bw.newLine();
            bw.write(data.getProperty(ReportField.STACK_TRACE));
            
            bw.newLine();
            bw.write("#### eventlog");
            bw.newLine();
            bw.write(""+data.getProperty(ReportField.EVENTSLOG));
            
            
            bw.newLine();
            bw.write("#### Logcat");
            bw.newLine();
            bw.write(""+data.getProperty(ReportField.LOGCAT));
            
            bw.close();
            Log.i(K9.LOG_TAG, "wrote file" + logFolder+ "/"+filename);
        } catch (IOException e) {
            // TODO can not write log file
            Log.e(K9.LOG_TAG, "can not write log file",e);
        } 
    }
    
    private Set<String> listAccountUUids() {
        Account[] accounts = Preferences.getPreferences(K9.app.getApplicationContext()).getAccounts();
        Set<String> uuids = new HashSet<String>();
        for(Account a : accounts) {
            uuids.add(a.getUuid());
        }
        return uuids;
    }
    
    /***
     * get the account settings and removes all hostnames and usernames and pws
     * the anonymization is done by exportPreferences function
     * @param uuids the list of all accounts
     * @return the string representation (xml) of the settings
     * @throws SettingsImportExportException
     */
    private String getAccountSettings(Set<String> uuids) throws SettingsImportExportException{
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SettingsExporter.exportPreferences(K9.app.getApplicationContext(), baos, true, uuids, true);
        
        return new String(baos.toByteArray(), Charset.defaultCharset());
    }

}
