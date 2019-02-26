package com.ses.zebra.personalshopperassistant.Activities;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.os.Environment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.ses.zebra.personalshopperassistant.Debugging.Logger;
import com.ses.zebra.personalshopperassistant.R;
import com.ses.zebra.personalshopperassistant.databinding.ActivityHomeBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeActivity extends BaseActivity {

    // Debugging
    private static final String TAG = "HomeActivity";

    // Constants
    private static final int ALL_PERMISSIONS = 1;
    private static final String SETTINGS = "Settings";
    private static final String mPssDemoDirectory = Environment.getExternalStorageDirectory()
            + File.separator + "PSSDemo";
    private static final String mConfigFilePath = Environment.getExternalStorageDirectory()
            + File.separator + "PSSDemo" + File.separator + "config.ini";

    // Variables
    private ActivityHomeBinding mDataBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Init Variables
        mDataBinding = DataBindingUtil.setContentView(this, R.layout.activity_home);

        // Request Permissions
        checkAndRequestPermissions();

        // Init Click Listeners
        mDataBinding.settingsIcon.setOnClickListener(view -> {
            Intent settingsActivity = new Intent(HomeActivity.this, SettingsActivity.class);
            startActivity(settingsActivity);
        });

        // Set Message Listener
        if (mSharedPreferences.getBoolean(PREF_ENABLE_MQTT, false)) {
            mDataBinding.messageListenerContainer.setOnClickListener(view -> {
                Intent msgActivity = new Intent(HomeActivity.this, MessageListenerActivity.class);
                startActivity(msgActivity);
            });
        } else {
            disableEnableChildViews(false, mDataBinding.messageListenerContainer);
        }

        // Set Call Listener
        if (mSharedPreferences.getBoolean(PREF_ENABLE_WFC, false)) {
            mDataBinding.callListenerContainer.setOnClickListener(view -> {
                Intent chatActivity = new Intent(HomeActivity.this, CallListenerActivity.class);
                startActivity(chatActivity);
            });
        } else {
            disableEnableChildViews(false, mDataBinding.callListenerContainer);
        }

    }
    /*
     * Check Permissions at Runtime
     */
    private  boolean checkAndRequestPermissions() {
        // Define Permissions
        int readContactsPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS);
        int writeContactsPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_CONTACTS);
        int phoneStatePermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_PHONE_STATE);
        int microphonePermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO);
        int locationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int readStoragePermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE);
        int writeStoragePermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        // Add non-granted permissions to Array for Requesting
        List<String> listPermissionsNeeded = new ArrayList<>();
        if (readContactsPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_CONTACTS);
        }
        if (writeContactsPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_CONTACTS);
        }
        if (phoneStatePermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (microphonePermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
        }
        if (locationPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (readStoragePermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (writeStoragePermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        // Request permissions, if required
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),
                    ALL_PERMISSIONS);
            return false;
        } return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case ALL_PERMISSIONS: {
                Map<String, Integer> perms = new HashMap<>();

                // Initialize the map with both permissions
                perms.put(Manifest.permission.CAMERA, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.RECORD_AUDIO, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.READ_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);

                // Fill with actual results from user
                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++)
                        perms.put(permissions[i], grantResults[i]);
                    // Check for both permissions
                    if (perms.get(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                            && perms.get(Manifest.permission.RECORD_AUDIO) !=
                                    PackageManager.PERMISSION_GRANTED
                            && perms.get(Manifest.permission.ACCESS_FINE_LOCATION) !=
                                    PackageManager.PERMISSION_GRANTED
                            && perms.get(Manifest.permission.READ_EXTERNAL_STORAGE) !=
                                    PackageManager.PERMISSION_GRANTED
                            && perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                                    PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "1 Or More Permissions Not Granted");
                        // Show dialog to re-request permissions
                        showDialogOK((dialog, which) -> {
                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    checkAndRequestPermissions();
                                    break;
                                case DialogInterface.BUTTON_NEGATIVE:
                                    Log.i(TAG, "Permissions Not Granted - Go to settings to enable");
                                    Log.i(TAG, "Permissions Not Granted - Application Exiting");
                                    finish();
                                    break;
                            }
                        });
                    }
                }
            }
        }
    }

    private void showDialogOK(DialogInterface.OnClickListener okListener) {
        new android.support.v7.app.AlertDialog.Builder(this)
                .setMessage("All Permissions are required to run this app")
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", okListener)
                .create()
                .show();
    }

    private void disableEnableChildViews(boolean enable, ViewGroup vg){
        for (int i = 0; i < vg.getChildCount(); i++){
            View child = vg.getChildAt(i);
            child.setEnabled(enable);
            if (child instanceof ViewGroup){
                disableEnableChildViews(enable, (ViewGroup)child);
            }
        }
    }
}
