package com.ses.zebra.personalshopperassistant.Activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import com.ses.zebra.personalshopperassistant.Interfaces.ShutdownCallback;
import com.slacorp.eptt.android.service.CoreBinder;
import com.slacorp.eptt.android.service.CoreCall;
import com.slacorp.eptt.android.service.CoreCallManager;
import com.slacorp.eptt.android.service.CoreService;
import com.slacorp.eptt.core.common.CallEndReason;
import com.slacorp.eptt.core.common.ErrorCode;
import com.slacorp.eptt.jcommon.Debugger;

import static com.slacorp.eptt.core.common.SessionState.IDLE;
import static com.slacorp.eptt.core.common.SessionState.UNREGISTERED;

public abstract class BaseActivity extends AppCompatActivity {

    // Debugging (Inherited tag implemented by all sub classes)
    private static final String MAIN_TAG = "CoreActivity";

    //
    public static final String PREF_EDIT_STOCK_LIST = "edit_stock_list";
    public static final String PREF_EDIT_OFFER_LIST = "edit_offer_list";
    public static final String PREF_EDIT_SHOPPING_LIST = "edit_shopping_list";
    public static final String PREF_ENABLE_MQTT = "mqtt_enabled";
    public static final String PREF_CUSTOM_MQTT_SERVER = "mqtt_use_custom_broker";
    public static final String PREF_MQTT_BROKER = "mqtt_broker";
    public static final String PREF_MQTT_USER = "mqtt_broker_user";
    public static final String PREF_MQTT_PASS = "mqtt_broker_pass";
    public static final String PREF_CURRENT_TOPIC = "mqtt_topic";
    public static final String PREF_TOPIC_IDENTIFIER = "mqtt_topic_identifier";
    public static final String PREF_SUB_TOPIC_IDENTIFIER = "mqtt_sub_topic_identifier";
    public static final String PREF_ENABLE_WFC = "wfc_enabled";
    public static final String PREF_WFC_ACTIVATION_CODE = "wfc_provisioning_code";
    public static final String PREF_ENABLE_VLC = "vlc_enabled";
    public static final String PREF_VLC_CONFIG_STRING = "vlc_config_string";
    public static final String PREF_VLC_SELECT_MAP = "vlc_map";
    public static final String PREF_ENABLE_CONTEXTUAL_VOICE = "contextual_voice_enabled";
    public static final String PREF_AI_CONFIG_STRING = "contextual_voice_config_string";

    // Variables
    public static SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Init shared Pref
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        setupUI();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Setup UI
        setupUI();
    }

    public void displayActivity(Class<?> Activity) {
        Intent i = new Intent(this, Activity);
        startActivity(i);
        finish();
        overridePendingTransition(0, 0);
    }

    @Override
    protected void onPause() {
        super.onPause();
        setupUI();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            setupUI();
        }
    }

    private void setupUI() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        View decorView = setSystemUiVisibilityMode();
        decorView.setOnSystemUiVisibilityChangeListener(visibility -> {
            setSystemUiVisibilityMode(); // Needed to avoid exiting immersive_sticky when keyboard is displayed
        });
    }

    private View setSystemUiVisibilityMode() {
        View decorView = getWindow().getDecorView();
        int options = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                | View.SYSTEM_UI_FLAG_IMMERSIVE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(options);
        return decorView;
    }
}
