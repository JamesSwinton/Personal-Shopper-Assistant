package com.ses.zebra.personalshopperassistant;

import android.app.Application;
import android.content.Context;
import android.os.Build;

public class App extends Application {

    // Debugging
    private static final String TAG = "App";

    // Constants

    // Variable
    public static Context mAppContext;
    public static String mDeviceSerialNumber;

    @Override
    public void onCreate() {
        super.onCreate();

        // Set App Context
        mAppContext = this;

        // Set Device Serial
        mDeviceSerialNumber = Build.SERIAL;
    }
}
