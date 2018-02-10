package com.fsck.k9.activity;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.Toast;

import com.fsck.k9.R;
import com.fsck.k9.activity.K9ActivityCommon.K9ActivityMagic;
import com.fsck.k9.activity.misc.SwipeGestureDetector.OnSwipeGestureListener;


public abstract class K9Activity extends Activity implements K9ActivityMagic {

    private K9ActivityCommon mBase;

    public static final int PERMISSIONS_REQUEST_READ_CONTACTS = 1;
    public static final int PERMISSIONS_REQUEST_WRITE_CONTACTS = 2;
    public static final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 3;
    public static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 4;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        mBase = K9ActivityCommon.newInstance(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        mBase.preDispatchTouchEvent(event);
        return super.dispatchTouchEvent(event);
    }

    @Override
    public void setupGestureDetector(OnSwipeGestureListener listener) {
        mBase.setupGestureDetector(listener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_READ_CONTACTS:
            case PERMISSIONS_REQUEST_WRITE_CONTACTS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Toast.makeText(this, R.string.contact_permission_thanks,
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.permission_request,
                            Toast.LENGTH_LONG).show();
                }
                break;
            }
            case PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Toast.makeText(this, R.string.storage_permission_thanks,
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.permission_request,
                            Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }
}
