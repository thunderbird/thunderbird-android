package com.fsck.k9.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.fsck.k9.R;
import com.fsck.k9.helper.ContactItem;

public class EmailAddressList extends K9ListActivity implements OnItemClickListener {
    public static final String EXTRA_CONTACT_ITEM = "contact";
    public static final String EXTRA_EMAIL_ADDRESS = "emailAddress";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.email_address_list);

        Intent i = getIntent();
        ContactItem contact = (ContactItem) i.getSerializableExtra(EXTRA_CONTACT_ITEM);
        if (contact == null) {
            finish();
            return;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                R.layout.email_address_list_item, contact.emailAddresses);

        ListView listView = getListView();
        listView.setOnItemClickListener(this);
        listView.setAdapter(adapter);
        setTitle(contact.displayName);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String item = (String)parent.getItemAtPosition(position);

        Toast.makeText(EmailAddressList.this, item, Toast.LENGTH_LONG).show();

        Intent intent = new Intent();
        intent.putExtra(EXTRA_EMAIL_ADDRESS, item);
        setResult(RESULT_OK, intent);
        finish();
    }
}
