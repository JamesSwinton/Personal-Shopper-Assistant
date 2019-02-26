package com.ses.zebra.personalshopperassistant.Activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.google.gson.Gson;
import com.ses.zebra.personalshopperassistant.Adapters.ChatAdapter;
import com.ses.zebra.personalshopperassistant.App;
import com.ses.zebra.personalshopperassistant.Debugging.Logger;
import com.ses.zebra.personalshopperassistant.MQTT;
import com.ses.zebra.personalshopperassistant.POJOs.HelpMessage;
import com.ses.zebra.personalshopperassistant.R;
import com.ses.zebra.personalshopperassistant.databinding.ActivityMessageBinding;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MessageActivity extends BaseActivity {

    // Debugging
    private static final String TAG = "MessageActivity";

    // Constants
    private static final Handler mHandler = new Handler(Looper.getMainLooper());
    public static final String mAssociateSerial = Build.SERIAL;
    private static final String INITIAL_MESSAGE = "initial-message";
    private static final String[] mAssociateNames = {
            "John",
            "Lesly",
            "Michael",
            "James",
            "Sarah"
    };
    private static final Gson mGson = new Gson();

    private static final String DEFAULT_TOPIC = "PSSDEMO/MQTT/DEFAULT-ASSISTANT/DEFAULT-ASSISTANT/";
    private static final String HELP_REQUEST_BROADTCAST_TOPIC = "HELP_REQUEST_BROADCAST";
    private static final String HELP_REQUEST_BROADCAST_ACCEPTED_TOPIC = "HELP_REQUEST_BROADCAST_ACCEPTED";
    private static final String HELP_REQUEST_BROADCAST_ACCEPTED_REPLY_TOPIC = "HELP_REQUEST_BROADCAST_ACCEPTED_REPLY/";
    private static final String HELP_REQUEST_PRIVATE_CHAT_TOPIC = "CHAT_ASSIST/";
    private static final String HELP_REQUEST_PRIVATE_CHAT_ENDED_TOPIC = "CHAT_ASSIST_ENDED/";

    // Variables
    private ActivityMessageBinding mDataBinding;

    private static ChatAdapter mChatAdapter;
    public static String mCustomerSerial = "";
    private static List<HelpMessage> mHelpMessages;
    private static Activity mActivity;


    private static String mCurrentTopic;
    private static InputMethodManager mInputManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        // Init Variables
        mActivity = this;
        mHelpMessages = new ArrayList<>();
        mCurrentTopic = mSharedPreferences.getString(PREF_CURRENT_TOPIC, DEFAULT_TOPIC);
        mDataBinding = DataBindingUtil.setContentView(this, R.layout.activity_message);
        mInputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        /*
          * Get initial request message
          * Including Customer Serial required for private chat channel topic
          *
          * If intent was null, we cannot init the chat. Inform associate.
          */
        if (getInitialRequestMessage()) {
            // Unsubscribe from Broadcast && Subscribe to Private Chat Topic
            updateSubscriptions();
            // Broadcast to associates that request was accepted
            // Send message to All Associates && Customer
            notifyRequestAcceptance();
        } else {
            Logger.i(TAG, "HomeActivity delivered null intent. " +
                                  "Cannot initialise private chat topic.");

            Toast.makeText(this, "Error initialising private chat topic", Toast.LENGTH_LONG).show();
        }

        // Init Chat Recycler View
        mChatAdapter = new ChatAdapter(mHelpMessages);
        mDataBinding.chatLogRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mDataBinding.chatLogRecyclerView.setAdapter(mChatAdapter);

        // Init Send Message Listeners
        initSendMessageListeners();

        // Init End Chat Listener
        mDataBinding.headerIcon.setOnClickListener(view ->
            new AlertDialog.Builder(this)
                    .setTitle("Confirm Exit")
                    .setMessage("Are you sure you wish to exit this chat? This action cannot be " +
                                "undone and you will lose connection with the customer")
                    .setPositiveButton("EXIT", (dialog, i) -> {
                        // Notify Customer
                        notifyChatEnding();
                        // End Chat and Reset Subscriptions in onDestroy()
                        finish();
                    })
                    .setNegativeButton("CANCEL", (dialog, i) -> dialog.dismiss())
                    .show());
    }

    private void notifyChatEnding() {
        // Notify Chat Ending
        try {
            MQTT.publish(mCurrentTopic + HELP_REQUEST_PRIVATE_CHAT_ENDED_TOPIC
                    + mAssociateSerial + "/" + mCustomerSerial, new byte[0]);
        } catch (MqttException e) {
            Logger.e(TAG, "MQTT Exception :" + e.getMessage(), e);
        }
    }

    @Override
    protected void onDestroy() {
        // Destroy Activity Reference
        mActivity = null;

        // Unsubscribe from Chat Topics
        MQTT.unsubscribeFromTopic(mCurrentTopic + HELP_REQUEST_PRIVATE_CHAT_TOPIC
                + mCustomerSerial + "/" + mAssociateSerial);
        MQTT.unsubscribeFromTopic(mCurrentTopic + HELP_REQUEST_PRIVATE_CHAT_ENDED_TOPIC
                + mCustomerSerial + "/" + mAssociateSerial);

        // Re-Subscribe to Broadcast Topic
        MQTT.subscribeToTopic(mCurrentTopic + HELP_REQUEST_BROADTCAST_TOPIC);
        MQTT.subscribeToTopic(mCurrentTopic + HELP_REQUEST_BROADCAST_ACCEPTED_TOPIC);

        // Pass to Super Class
        super.onDestroy();
    }

    private void updateSubscriptions() {
        // Unsubscribe from Broadcast Topic
        MQTT.unsubscribeFromTopic(mCurrentTopic + HELP_REQUEST_BROADTCAST_TOPIC);
        MQTT.unsubscribeFromTopic(mCurrentTopic + HELP_REQUEST_BROADCAST_ACCEPTED_TOPIC);

        // Subscribe to Chat Topic
        MQTT.subscribeToTopic(mCurrentTopic + HELP_REQUEST_PRIVATE_CHAT_TOPIC
                + mCustomerSerial + "/" + mAssociateSerial);
        MQTT.subscribeToTopic(mCurrentTopic + HELP_REQUEST_PRIVATE_CHAT_ENDED_TOPIC
                + mCustomerSerial + "/" + mAssociateSerial);
    }

    private void notifyRequestAcceptance() {
        // Get Random Associate Name
        String associateName = mAssociateNames[new Random().nextInt(mAssociateNames.length)];

        // Create Accepted Help Message
        HelpMessage requestAccepted = new HelpMessage(HelpMessage.MessageType.SYSTEM,
                "We've connected you with " + associateName, System.currentTimeMillis(),
                mAssociateSerial);

        try {
            MQTT.publish(mCurrentTopic + HELP_REQUEST_BROADCAST_ACCEPTED_TOPIC, new byte[0]);
            MQTT.publish(mCurrentTopic + HELP_REQUEST_BROADCAST_ACCEPTED_REPLY_TOPIC + mCustomerSerial,
                    mGson.toJson(requestAccepted).getBytes());
        } catch (MqttException e) {
            Logger.e(TAG, "MQTT Exception: " + e.getMessage(), e);
        }

    }

    private boolean getInitialRequestMessage() {
        Intent intent = getIntent();
        if (intent.getExtras() != null && intent.getStringExtra(INITIAL_MESSAGE) != null) {
            // Get Help Message
            HelpMessage initialMessage = mGson.fromJson(intent.getStringExtra(INITIAL_MESSAGE), HelpMessage.class);

            // Extract Customer Device Serial Number from Message
            mCustomerSerial = initialMessage.getDeviceSerialNumber();

            // Update Sent / Received Parameter
            initialMessage.setSender(HelpMessage.MessageType.RECEIVED);

            // Update List
            mHelpMessages.add(initialMessage);

            return true;
        } else {
            Logger.i(TAG, "Error getting initial message from intent");

            return false;
        }
    }

    public static void newMessageReceived(String message) {
        // Get Help Message
        HelpMessage initialMessage = mGson.fromJson(message, HelpMessage.class);

        // Update Sent / Received Parameter
        initialMessage.setSender(HelpMessage.MessageType.RECEIVED);

        // Update List
        mHelpMessages.add(initialMessage);

        // Notify Adapter
        mHandler.post(() -> mChatAdapter.updateMessageList(mHelpMessages));
    }

    private void initSendMessageListeners() {
        // Set "Send Request" Button Listener
        mDataBinding.sendRequestButton.setOnClickListener(view -> {
            String message = mDataBinding.assistanceMessage.getText().toString().trim();
            if (messageValidated(message)) { buildAndSendHelpMessage(message); }
        });

        // Set imeOptions Send Button Listener
        mDataBinding.sendRequestButton.setOnEditorActionListener((textView, i, keyEvent) -> {
            // If triggered by an enter key, this is the event; otherwise, this is null.
            if (keyEvent != null) {
                String message = mDataBinding.assistanceMessage.getText().toString().trim();
                if (messageValidated(message)) {
                    buildAndSendHelpMessage(message);
                    return true;
                }
            } return false;
        });
    }

    private boolean messageValidated(String message) {
        // Check message was entered
        if (TextUtils.isEmpty(message)) {
            Toast.makeText(this, "Please enter a message...", Toast.LENGTH_LONG).show();
            return false;
        } return true;
    }

    private void buildAndSendHelpMessage(String message) {
        // Create HelpMessage
        HelpMessage helpMessage = new HelpMessage(HelpMessage.MessageType.RECEIVED, message,
                System.currentTimeMillis(), mAssociateSerial);
        // Remove Text From Send Box
        mDataBinding.assistanceMessage.setText("");
        // Close Soft Keyboard
        mInputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);
        // MQTT Connected? -> Publish Message -> Init ChatLog Adapter
        try {
            // Publish Message
            MQTT.publish(mCurrentTopic + HELP_REQUEST_PRIVATE_CHAT_TOPIC
                    + mAssociateSerial + "/" + mCustomerSerial, mGson.toJson(helpMessage).getBytes());

            // Update Chat Log
            helpMessage.setSender(HelpMessage.MessageType.SENT);
            updateChatLog(helpMessage);
        } catch (MqttException e) {
            Logger.e(TAG, "MQTT Exception: " + e.getMessage(), e);
        }
    }

    private static void updateChatLog(HelpMessage helpMessage) {
        mHandler.post(() -> {
            mHelpMessages.add(helpMessage);
            mChatAdapter.updateMessageList(mHelpMessages);
        });
    }

    public static void chatEndedByCustomer(){
        mHandler.post(() ->
                new AlertDialog.Builder(mActivity)
                        .setTitle("Chat Ended")
                        .setMessage("Chat was closed by the Customer")
                        .setPositiveButton("OK", (dialog, i) -> {
                            mActivity.finish();
                            mActivity = null;
                        })
                        .show());
    }
}
