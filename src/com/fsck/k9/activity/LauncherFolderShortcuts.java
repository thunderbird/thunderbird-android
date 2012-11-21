package com.fsck.k9.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

import com.fsck.k9.Account;
import com.fsck.k9.BaseAccount;
import com.fsck.k9.R;
import com.fsck.k9.search.SearchSpecification;
import com.fsck.k9.activity.ChooseFolder;

public class LauncherFolderShortcuts extends AccountList {
    // Written by Av (www.avbrand.com), first attempt at modifying an existing android app.
    // Please bear with me!
    
    private static final int SELECT_ACCOUNT_FOLDER = 1;
    private Account chosenAccount; // Store the account they used.
    
    @Override
    public void onCreate(Bundle icicle) {
        // finish() immediately if we aren't supposed to be here
        if (!Intent.ACTION_CREATE_SHORTCUT.equals(getIntent().getAction())) {
            finish();
            return;
        }

        super.onCreate(icicle);
    }

    @Override
    protected boolean displaySpecialAccounts() {
        return true;
    }

    @Override
    protected void onAccountSelected(BaseAccount account) {
        


				// Allow the user to choose the folder for this account.
				
				// Don't allow these accounts, since they probably won't work.
				if (account instanceof SearchSpecification) {
					finish();
				}
				else
				{
					// Store the account.
					chosenAccount = (Account)account;
					
					// Show the folder chooser.
					
					Intent selectIntent = new Intent(this, ChooseFolder.class);
		      selectIntent.putExtra(ChooseFolder.EXTRA_ACCOUNT, ((Account)account).getUuid());
		
		      //selectIntent.putExtra(ChooseFolder.EXTRA_CUR_FOLDER, mAutoExpandFolder.getSummary());
		      //selectIntent.putExtra(ChooseFolder.EXTRA_SHOW_CURRENT, "yes");
		      //selectIntent.putExtra(ChooseFolder.EXTRA_SHOW_FOLDER_NONE, "yes");
		      selectIntent.putExtra(ChooseFolder.EXTRA_SHOW_DISPLAYABLE_ONLY, "yes");
		      startActivityForResult(selectIntent, SELECT_ACCOUNT_FOLDER);

				}
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Intent shortcutIntent = null;
        
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
            case SELECT_ACCOUNT_FOLDER: // They selected the folder in the account.
                
                // Create the new shortcut, and specify the folder that was selected.
                String folder = translateFolder(data.getStringExtra(ChooseFolder.EXTRA_NEW_FOLDER));
				        shortcutIntent = FolderList.actionHandleAccountIntent(this, chosenAccount, folder, true);
				        
				        Intent intent = new Intent();
				        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
				        String description = folder;

				        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, description);
				        Parcelable iconResource = Intent.ShortcutIconResource.fromContext(this, R.drawable.icon);
				        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
				
				        setResult(RESULT_OK, intent);
				        finish();
                
                break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }    
    
    private String translateFolder(String in) {
        if (chosenAccount.getInboxFolderName().equalsIgnoreCase(in)) {
            return getString(R.string.special_mailbox_name_inbox);
        } else {
            return in;
        }
    }    
}
