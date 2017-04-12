package com.fsck.k9.activity.license;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.fsck.k9.activity.K9Activity;

import java.io.IOException;

/**
 * Activity shows the list of third party software licenses.
 */
public class OssLicenseListActivity extends K9Activity
        implements AdapterView.OnItemClickListener {

    private String[] licenses;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
        ListView lv = new ListView(this);
        try {
            licenses = getAssets().list("license");
        } catch (IOException e) {
            finish();
            return;
        }
        ArrayAdapter<String> licenseAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, licenses);
        lv.setAdapter(licenseAdapter);
        lv.setOnItemClickListener(this);
        setContentView(lv);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return false;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        startActivity(new Intent(this, OssLicenseViewActivity.class)
                .putExtra(OssLicenseViewActivity.EXTRA_LICENSE_NAME, licenses[position]));
    }
}
