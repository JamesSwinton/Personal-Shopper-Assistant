package com.ses.zebra.personalshopperassistant.Activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.ses.zebra.personalshopperassistant.App;
import com.ses.zebra.personalshopperassistant.R;
import com.ses.zebra.personalshopperassistant.Utilities.EnumToText;
import com.ses.zebra.personalshopperassistant.databinding.ActivityCallListenerBinding;
import com.slacorp.eptt.android.service.CallListener;
import com.slacorp.eptt.android.service.CoreBinder;
import com.slacorp.eptt.core.common.ErrorCode;
import com.slacorp.eptt.core.common.SessionState;

public class CallListenerActivity extends CoreActivity {

    // Debugging
    private static final String TAG = "ChatAssistantActivity";

    // Constants
    private static final String ACTIVATION_CODE_1 = "WCHDUZXKLV";

    private CoreListener mCoreListener = new CoreListener();

    // Variables
    private String mActivationCode;
    private ActivityCallListenerBinding mDataBinding;
    private boolean isCoreProvisioned; // True when Core is Provisioned && We've started an activity

    /**
     * LifeCycleMethods
     * onCreate() -> No Functionality
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_listener);

        // Init DataBinding
        mDataBinding = DataBindingUtil.setContentView(this, R.layout.activity_call_listener);

        // Get Activation Code
        mActivationCode = mSharedPreferences.getString(PREF_WFC_ACTIVATION_CODE,
                getString(R.string.default_wfc_activation_code));

        // Init Back Button
        mDataBinding.headerIcon.setOnClickListener(view -> {
            // Shutdown Calls
            shutdown(callsEnded -> {
                // Log Shutdown Progress
                Log.i(TAG, "Shutdown Complete - Calls Ended: " + callsEnded);

                // Return to Parent
                displayActivity(HomeActivity.class);
            });
        });
    }

    /**
     * Core Listener Class
     * Listens for changes in Core State
     * error()              -> Catches CoreBinder in Error State
     * sessionStateChange() -> Notifies when CoreBinder Session State has changed
     **/

    @SuppressLint("SetTextI18n")
    private class CoreListener extends com.slacorp.eptt.android.service.CoreListener {
        @Override
        public void error(final int error, String extra) {
            // Log Error
            Log.e(TAG, "CoreBinder Error - " + error + ", " + extra);
            // Handle Error
            mHandler.post(() -> {
                switch (error) {
                    case ErrorCode.AUTH_FAILURE :
                    case ErrorCode.CONFIG_UPDATE_FAILURE_NO_DATA :
                        Log.e(TAG, "Error Provisioning CoreBinder - Stopping Core");

                        // Update UI
                        mDataBinding.provisioningText.setText("This Activation code(" +
                                mActivationCode + ") has already been registered");
                        mDataBinding.provisioningProgressBar.setVisibility(View.GONE);

                        // Get Core binder -> Stop Core
                        CoreBinder core = getCoreBinder();
                        if (core != null) { core.stopCore(); }
                        break;
                    case ErrorCode.CONFIG_UPDATE_FAILURE_USER_NOT_FOUND:
                        mDataBinding.provisioningText.setText("We could not find an account with " +
                                "this Activation Code (" + mActivationCode + ")");
                        mDataBinding.provisioningProgressBar.setVisibility(View.GONE);
                        break;
                    case ErrorCode.FATAL_ERROR:
                        mDataBinding.provisioningText.setText("We encountered a error, please try again");
                        mDataBinding.provisioningProgressBar.setVisibility(View.GONE);
                        break;
                    case ErrorCode.NETWORK_FAILURE:
                    case ErrorCode.NETWORK_OFFLINE:
                    case ErrorCode.NETWORK_TIMEOUT:
                        mDataBinding.provisioningText.setText("There was an error due to the network, please try again");
                        mDataBinding.provisioningProgressBar.setVisibility(View.GONE);
                        break;
                    case ErrorCode.STARTUP_FAILURE:
                        break;
                    default:
                        mDataBinding.provisioningText.setText("We encountered an error (" + error +
                                " - " + (extra == null ? "" : extra)+ "), please try again");
                        mDataBinding.provisioningProgressBar.setVisibility(View.GONE);
                        break;
                }
            });
        }

        @Override
        public void sessionStateChange(final int state) {
            mHandler.post(() -> {
                // Log State
                Log.i(TAG, "Session state: " + EnumToText.getSessionStateAsString(state));
                switch (state) {
                    // IDLE State = CoreBinder is successfully Provisioned and we can use it
                    case SessionState.IDLE:
                        if (!isCoreProvisioned) { startPushToTalkActivity(); }
                        break;
                    default:
                        break;
                }
            });
        }
    }

    /**
     * Inherited Methods
     * getInheritedTag()  -> Returns this TAG to Parent class for debugging
     * coreServiceBound() -> Attaches mCoreListener to CoreBinder
     *                       Provisions CoreBinder with ACTIVATION_CODE
     */
    @Override
    protected String getInheritedTag() { return TAG; }

    @Override
    protected void coreServiceBound(CoreBinder coreBinder) {
        // Attach CoreListener to CoreBinder
        coreBinder.addCoreListener(mCoreListener);
        // Provision CoreBinder with ACTIVATION_CODE
        // provisionCore(coreBinder, ACTIVATION_CODE_1);
        provisionCore(coreBinder, mActivationCode);
    }

    /**
     * Start (Provision) the Core Binder.
     * This is different than starting and binding the core service, which is done by the
     * CoreActivity base class. If the core provisions successfully, our CoreListener will get a
     * notification that we've entered the IDLE state.  Otherwise it'll get an error notification.
     */
    private void provisionCore(CoreBinder core, String activationCode) {
        Log.i(TAG, "Provisioning CoreBinder with Activation Code: " + activationCode);
        core.startCore(activationCode);
    }

    /**
     * Helper method to start PushToTalkActivity
     **/
    private void startPushToTalkActivity() {
        // Log Step
        Log.i(TAG, "Core Provisioned -> Starting Push To Talk Activity");
        // Set Core Provisioned True
        isCoreProvisioned = true;
        // Start Push To Talk Activity
        Intent startPushToTalkActivity = new Intent(CallListenerActivity.this,
                PushToTalkActivity.class);
        startActivity(startPushToTalkActivity);
        // Finish && Exit this Activity
        finish();
    }
}
