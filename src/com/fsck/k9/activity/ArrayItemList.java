package com.fsck.k9.activity;

import java.util.ArrayList;

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

public class ArrayItemList extends K9ListActivity implements OnItemClickListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.item_list);

        Intent i = getIntent();
        ContactItem contact = (ContactItem) i.getSerializableExtra("contact");
        if (contact == null) {
            return;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, contact.getEmailAddresses());

        ListView listView = getListView();
        listView.setOnItemClickListener(this);
        listView.setAdapter(adapter);
        setTitle(contact.getDisplayName());
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String item = (String)parent.getItemAtPosition(position);

        Toast.makeText(ArrayItemList.this, item, Toast.LENGTH_LONG).show();

        Intent intent = new Intent();
        intent.putExtra("EMAIL_ADDRESS", item);
        setResult(RESULT_OK, intent);
        finish();
    }
}
