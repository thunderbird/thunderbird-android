package com.fsck.k9.helper;

import android.os.Environment;
import android.util.Log;

import com.fsck.k9.K9;
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
        String versionCode = data.getProperty(ReportField.APP_VERSION_NAME);
        String logcat = data.getProperty(ReportField.LOGCAT);
        Log.e(K9.LOG_TAG, "XXXX sending report "+logFolder + " " + versionCode,new RuntimeException("XXXXXXXXXXX"));

        
        String state = Environment.getExternalStorageState();
        
        
        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // TODO we can not write our log
            Log.w(K9.LOG_TAG, "can't write error log -> sd path "+ logFolder + " readonly ");
            return;
        }
        
        File dir = new File(logFolder.getAbsolutePath() + "/k9LOG/");
        dir.mkdirs();
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US);
        String filename = sdf.format(new Date());
        File file = new File(dir, filename);

        Set<String> uuids = new HashSet<String>();
        try {
            FileOutputStream f = new FileOutputStream(file);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(f));
            
            bw.write("#### k9 generated LOGFILE");
            bw.newLine();
            bw.write("gen:" + filename);
            bw.newLine();
            bw.write("Version:" + versionCode);
            bw.newLine();
            
            bw.write("#### AccountSettings"); 
            bw.newLine();
            try {
                getAccountSettings(uuids);
            } catch (SettingsImportExportException e) {
                bw.write("can not get account settings for crash log");
                Log.e(K9.LOG_TAG,"can not get account settings for crash log",e);
            }
            bw.newLine();
            
            bw.write("#### Logcat");
            bw.newLine();
            bw.write(logcat);
            
            bw.close();
            
            Log.i(K9.LOG_TAG, "wrote file" + dir+ "/"+filename);
        } catch (IOException e) {
            // TODO can not write log file
            Log.e(K9.LOG_TAG, "can not write log file",e);
        } 
        
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
