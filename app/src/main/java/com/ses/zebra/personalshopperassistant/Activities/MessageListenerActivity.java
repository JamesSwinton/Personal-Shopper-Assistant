package com.ses.zebra.personalshopperassistant.Activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;
import com.ses.zebra.personalshopperassistant.App;
import com.ses.zebra.personalshopperassistant.Interfaces.MqttConnectionCallback;
import com.ses.zebra.personalshopperassistant.MQTT;
import com.ses.zebra.personalshopperassistant.POJOs.HelpMessage;
import com.ses.zebra.personalshopperassistant.R;
import com.ses.zebra.personalshopperassistant.databinding.ActivityMessageListenerBinding;

public class MessageListenerActivity extends BaseActivity {

    // Debugging
    private static final String TAG = "MessageListenerActivity";

    // Cosntants
    private static final Gson mGson = new Gson();
    private static final String INITIAL_MESSAGE = "initial-message";
    private static final Handler mHandler = new Handler(Looper.getMainLooper());
    private static final String STATUS_ACTIVE = "<b>Status: <font color='green'><i>Active</i></font></b>";
    private static final String STATUS_PROVISIONING_ERROR = "<b>Status: <font color='red'><i>PROVISIONING-ERROR</i></font></b>";

    // Variables
    private static Context mContext;
    private static boolean isMqttConnected = false;
    private ActivityMessageListenerBinding mDataBinding;

    // Variables
    private static LayoutInflater mLayoutInflater;
    private static AlertDialog mIncomingRequestDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_listener);

        // Init DataBinding
        mLayoutInflater = getLayoutInflater();
        mDataBinding = DataBindingUtil.setContentView(this, R.layout.activity_message_listener);

        // Init Back
        mDataBinding.headerIcon.setOnClickListener(view -> {
            // Disconnect MQTT
            MQTT.disconnect();
            // Return Home
            displayActivity(HomeActivity.class);
        });

        // Check MQTT Connected
        if (!isMqttConnected) {
            MQTT.init(new MqttConnectionCallback() {
                @Override
                public void onConnected() {
                    updateUIOnConnectedMqtt(true);
                }

                @Override
                public void onConnectionLost() {
                    updateUIOnConnectedMqtt(false);
                }
            });
        }
    }

    @Override
    protected void onResume() {
        mContext = this;
        super.onResume();
    }

    public void updateUIOnConnectedMqtt(boolean connected) {
        // Log Step
        Log.i(TAG, "Updating UI - MQTT Connected: " + connected);
        // Update UI Elements
        mHandler.post(() -> {
            if (connected) {
                mDataBinding.messageListenerStatusBar.setVisibility(View.GONE);
                mDataBinding.messageListenerStatusIcon.setVisibility(View.VISIBLE);
                mDataBinding.messageListenerStatusIcon.setColorFilter(ContextCompat.getColor(
                        this, R.color.zebraGreen), android.graphics.PorterDuff.Mode.SRC_IN);
                mDataBinding.messageListenerStatusIcon.setImageDrawable(getDrawable(R.drawable.ic_tick));
                mDataBinding.messageListenerStatusText.setText(Html.fromHtml(STATUS_ACTIVE));
            } else {
                mDataBinding.messageListenerStatusBar.setVisibility(View.GONE);
                mDataBinding.messageListenerStatusIcon.setVisibility(View.VISIBLE);
                mDataBinding.messageListenerStatusIcon.setColorFilter(ContextCompat.getColor(
                        this, R.color.zebraRed), android.graphics.PorterDuff.Mode.SRC_IN);
                mDataBinding.messageListenerStatusIcon.setImageDrawable(getDrawable(R.drawable.ic_error));
                mDataBinding.messageListenerStatusText.setText(Html.fromHtml(STATUS_PROVISIONING_ERROR));
            }
        });
    }

    public static void incomingAssistanceRequest(String assistanceMessage) {
        // Convert JSON Message to POJO
        HelpMessage message = mGson.fromJson(assistanceMessage, HelpMessage.class);

        // Inflate and create custom Dialog View
        View customLayout = mLayoutInflater.inflate(R.layout.dialog_incoming_assistance_request, null);
        TextView assistanceMessageTextView = customLayout.findViewById(R.id.assistanceMessageText);
        assistanceMessageTextView.setText(message.getMessage());

        // Display Alert Dialog
        mHandler.post(() -> {
            // Dismiss Existing Dialog
            if (mIncomingRequestDialog != null && mIncomingRequestDialog.isShowing()) {
                mIncomingRequestDialog.dismiss();
            }

            // Show new Request Dialog
            mIncomingRequestDialog = new AlertDialog.Builder(mContext)
                    .setView(customLayout)
                    .setPositiveButton("ACCEPT", (dialogInterface, i) -> startChatActivity(assistanceMessage))
                    .setNegativeButton("REJECT", (dialogInterface, i) -> dialogInterface.dismiss())
                    .show();
        });
    }

    public static void cancelAssistanceRequest() {
        if (mIncomingRequestDialog != null && mIncomingRequestDialog.isShowing()) {
            mHandler.post(() -> {
                mIncomingRequestDialog.dismiss();
                mIncomingRequestDialog = new AlertDialog.Builder(App.mAppContext)
                        .setTitle("Request Expired")
                        .setMessage("Another associate has already handled this request.")
                        .setPositiveButton("OK", (dialog, i) -> dialog.dismiss())
                        .show();
            });
        }
    }

    private static void startChatActivity(String assistanceMessage) {
        Intent startChatActivity = new Intent(mContext, MessageActivity.class);
        startChatActivity.putExtra(INITIAL_MESSAGE, assistanceMessage);
        mContext.startActivity(startChatActivity);
    }

    @Override
    protected void onDestroy() {
        // Remove Context
        mContext = null;
        super.onDestroy();
    }
}
