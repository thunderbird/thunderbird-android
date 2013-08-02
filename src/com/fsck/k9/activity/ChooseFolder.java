
package com.fsck.k9.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

public class ChooseFolder extends FolderList {
    public static final String EXTRA_ACCOUNT = FolderList.EXTRA_ACCOUNT;

    public static final String EXTRA_CUR_FOLDER = "com.fsck.k9.ChooseFolder_curfolder";
    public static final String EXTRA_SEL_FOLDER = "com.fsck.k9.ChooseFolder_selfolder";
    public static final String EXTRA_NEW_FOLDER = "com.fsck.k9.ChooseFolder_newfolder";
    public static final String EXTRA_MESSAGE = "com.fsck.k9.ChooseFolder_message";
    public static final String EXTRA_SHOW_CURRENT = "com.fsck.k9.ChooseFolder_showcurrent";
    public static final String EXTRA_SHOW_DISPLAYABLE_ONLY = "com.fsck.k9.ChooseFolder_showDisplayableOnly";

    String mFolder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        mFolder = intent.getStringExtra(EXTRA_CUR_FOLDER);
        mAdapter.getHierarchyFilter().filterParent(mFolder);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d("FOLDERS", "mListView.onItemClick(, , position = " + position + ")");
                Intent result = new Intent();
                result.putExtra(EXTRA_ACCOUNT, mAccount.getUuid());
                result.putExtra(EXTRA_CUR_FOLDER, mFolder);

                result.putExtra(EXTRA_NEW_FOLDER, ((FolderInfoHolder) mAdapter.getItem(position)).name);

                setResult(RESULT_OK, result);
                finish();
            }
        });
    }
};
