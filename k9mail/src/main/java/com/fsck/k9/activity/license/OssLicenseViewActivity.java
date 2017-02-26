package com.fsck.k9.activity.license;

import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.fsck.k9.activity.K9Activity;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Activity shows third party software license.
 */
public class OssLicenseViewActivity extends K9Activity {
    public static final String EXTRA_LICENSE_NAME = "com.fsck.k9.extra.LICENSE_NAME";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String licenseName;
        if (getIntent() == null ||
                (licenseName = getIntent().getStringExtra(EXTRA_LICENSE_NAME)) == null) {
            finish();
            return;
        }
        AssetManager am;
        String license;
        InputStreamReader isr = null;
        BufferedReader br = null;
        try {
            am = getAssets();
            isr = new InputStreamReader(am.open("license/" + licenseName));
            br = new BufferedReader(isr);
            String line;
            StringBuilder licenseBuilder = new StringBuilder();
            while ((line = br.readLine()) != null) {
                licenseBuilder.append(line).append("\n");
            }
            license = licenseBuilder.toString();
        } catch (IOException e) {
            finish();
            return;
        } finally {
            closeStream(br, isr);
        }
        setContentView(createView(license));
        setTitle(licenseName);
    }

    /**
     * Close {@link Closeable} without throwing exception.
     *
     * @param closeables Closeables wants to close.
     */
    private void closeStream(Closeable... closeables) {
        for (Closeable closeable : closeables) {
            IOUtils.closeQuietly(closeable);
        }
    }

    /**
     * Make a view which able to show as a activity content view.
     * Add {@link TextView} inside of {@link ScrollView} to make TextView scrollable.
     *
     * @param license The text of the license.
     * @return Created view.
     */
    private View createView(String license) {
        TextView licenseText = new TextView(this);
        licenseText.setTypeface(Typeface.MONOSPACE);
        licenseText.setTextSize(15.0f);
        licenseText.setPadding(16, 16, 16, 16);
        licenseText.setAutoLinkMask(Linkify.WEB_URLS);
        licenseText.setText(license);
        ScrollView base = new ScrollView(this);
        base.addView(licenseText);
        return base;
    }
}
