package com.fsck.k9.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.widget.Toast;

import com.fsck.k9.activity.K9ActivityCommon.K9ActivityMagic;
import com.fsck.k9.activity.misc.SwipeGestureDetector.OnSwipeGestureListener;
import com.fsck.k9.ui.R;
import timber.log.Timber;


public abstract class K9Activity extends AppCompatActivity implements K9ActivityMagic {

    private K9ActivityCommon mBase;

    public static final int PERMISSIONS_REQUEST_READ_CONTACTS  = 1;
    public static final int PERMISSIONS_REQUEST_WRITE_CONTACTS = 2;

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
                // If request is cancelled, the result arrays are empty.
                boolean permissionWasGranted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
                if (!permissionWasGranted) {
                    Toast.makeText(this, R.string.contact_permission_request_denied,
                            Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public boolean hasPermission(Permission permission) {
        return ContextCompat.checkSelfPermission(this, permission.permission) == PackageManager.PERMISSION_GRANTED;
    }

    public void requestPermission(Permission permission) {
        Timber.i("Requesting permission: " + permission.permission);
        ActivityCompat.requestPermissions(this, new String[] { permission.permission }, permission.requestCode);
    }


    public enum Permission {
        READ_CONTACTS(Manifest.permission.READ_CONTACTS, PERMISSIONS_REQUEST_READ_CONTACTS),
        WRITE_CONTACTS(Manifest.permission.WRITE_CONTACTS, PERMISSIONS_REQUEST_WRITE_CONTACTS);


        final String permission;
        final int requestCode;

        Permission(String permission, int requestCode) {
            this.permission = permission;
            this.requestCode = requestCode;
        }
    }
}
