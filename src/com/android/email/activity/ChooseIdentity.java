
package com.android.email.activity;

import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.android.email.Account;

public class ChooseIdentity extends ListActivity
{
  Account mAccount;
  String	mUID;
  ArrayAdapter<String> adapter;
  private ChooseIdentityHandler mHandler = new ChooseIdentityHandler();
  
  public static final String EXTRA_ACCOUNT = "com.android.email.ChooseIdentity_account";
  public static final String EXTRA_IDENTITY = "com.android.email.ChooseIdentity_identity";

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    
    getListView().setTextFilterEnabled(true);
    getListView().setItemsCanFocus(false);
    getListView().setChoiceMode(ListView.CHOICE_MODE_NONE);
    Intent intent = getIntent();
    mAccount = (Account) intent.getSerializableExtra(EXTRA_ACCOUNT);
    
    adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
    
    List<Account.Identity> identities = mAccount.getIdentities();
    for (Account.Identity identity : identities)
    {
      adapter.add(identity.getName());
    }

    setListAdapter(adapter);

    this.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {  
      public void onItemClick(AdapterView adapterview, View view, int i, long l)
      {
        Intent intent = new Intent();
        
        intent.putExtra(EXTRA_IDENTITY, mAccount.getIdentity(i));
        setResult(RESULT_OK, intent);
        finish();
      }  
    });  

  }
  
  class ChooseIdentityHandler extends Handler
  {

    private static final int MSG_PROGRESS = 2;
    private static final int MSG_DATA_CHANGED = 3;

    public void handleMessage(android.os.Message msg)
    {
      switch (msg.what)
      {
        case MSG_PROGRESS:
          setProgressBarIndeterminateVisibility(msg.arg1 != 0);
          break;
        case MSG_DATA_CHANGED:
          adapter.notifyDataSetChanged();
          break;
      }
    }

    public void progress(boolean progress)
    {
      android.os.Message msg = new android.os.Message();
      msg.what = MSG_PROGRESS;
      msg.arg1 = progress ? 1 : 0;
      sendMessage(msg);
    }
    
    public void dataChanged()
    {
      sendEmptyMessage(MSG_DATA_CHANGED);
    }
  }
  
}
