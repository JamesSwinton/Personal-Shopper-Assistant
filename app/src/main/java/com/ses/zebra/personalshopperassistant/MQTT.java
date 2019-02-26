package com.ses.zebra.personalshopperassistant;

import android.os.Build;
import android.util.Log;

import com.google.gson.Gson;
import com.ses.zebra.personalshopperassistant.Activities.MessageActivity;
import com.ses.zebra.personalshopperassistant.Activities.MessageListenerActivity;
import com.ses.zebra.personalshopperassistant.Debugging.Logger;
import com.ses.zebra.personalshopperassistant.Interfaces.MqttConnectionCallback;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import static com.ses.zebra.personalshopperassistant.Activities.BaseActivity.PREF_CURRENT_TOPIC;
import static com.ses.zebra.personalshopperassistant.Activities.BaseActivity.PREF_MQTT_BROKER;
import static com.ses.zebra.personalshopperassistant.Activities.BaseActivity.PREF_MQTT_PASS;
import static com.ses.zebra.personalshopperassistant.Activities.BaseActivity.PREF_MQTT_USER;
import static com.ses.zebra.personalshopperassistant.Activities.BaseActivity.mSharedPreferences;

public class MQTT {

    // Debugging
    private static final String TAG = "MQTT";

    // Constants
    private static final int QOS_LEVEL_0 = 0;
    private static final int QOS_LEVEL_1 = 1;
    private static final int QOS_LEVEL_2 = 2;

    private static final String DEFAULT_BROKER = "ssl://52.42.13.85";
    private static final String DEFAULT_USER = "ZebraMQTTGeneralUser";
    private static final String DEFAULT_PASS = "Zebra3278!7$!*hdh737$";

    private static final String DEFAULT_TOPIC = "PSSDEMO/MQTT/DEFAULT/DEFAULT/";
    private static final String HELP_REQUEST_BROADCAST_TOPIC = "HELP_REQUEST_BROADCAST";
    private static final String HELP_REQUEST_BROADCAST_ACCEPTED_TOPIC = "HELP_REQUEST_BROADCAST_ACCEPTED";
    private static final String HELP_REQUEST_BROADCAST_ACCEPTED_REPLY_TOPIC = "HELP_REQUEST_BROADCAST_ACCEPTED_REPLY/";
    private static final String HELP_REQUEST_PRIVATE_CHAT_TOPIC = "CHAT_ASSIST/";
    private static final String HELP_REQUEST_PRIVATE_CHAT_ENDED_TOPIC = "CHAT_ASSIST_ENDED/";

    // Variables
    private static Gson mGson;
    private static String mTopic;
    private static String mMqttClientId;
    private static List<String> mMqttTopics;
    private static MqttAsyncClient mMqttAsyncClient;
    private static MqttConnectionCallback mqttConnectionCallback;

    private static String mCurrentTopic;
    private static String mMqttBroker;
    private static String mMqttUser;
    private static String mMqttPass;

    public static void init(MqttConnectionCallback callback) {
        // Set Callback ot Member Variable
        mqttConnectionCallback = callback;

        // Init GSON
        mGson = new Gson();

        // Get Current Topic
        mCurrentTopic = mSharedPreferences.getString(PREF_CURRENT_TOPIC, DEFAULT_TOPIC);

        // Get MQTT Credentials
        mMqttBroker = mSharedPreferences.getString(PREF_MQTT_BROKER, DEFAULT_BROKER);
        mMqttUser = mSharedPreferences.getString(PREF_MQTT_USER, DEFAULT_USER);
        mMqttPass = mSharedPreferences.getString(PREF_MQTT_PASS, DEFAULT_PASS);

        // Set Client ID
        mMqttClientId = MqttClient.generateClientId();

        // Build Topic List
        mMqttTopics = new ArrayList<>();
        mMqttTopics.add(mCurrentTopic + HELP_REQUEST_BROADCAST_TOPIC);
        mMqttTopics.add(mCurrentTopic + HELP_REQUEST_BROADCAST_ACCEPTED_TOPIC);

        // Create MQTT Async Client
        try {
            mMqttAsyncClient = new MqttAsyncClient(mMqttBroker, mMqttClientId, null);
        } catch (MqttException e) {
            e.printStackTrace();
        }

        // Set Callback Handler
        mMqttAsyncClient.setCallback(mqttCallbackHandler());

        // Set Connection Options
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setCleanSession(true);
        mqttConnectOptions.setSocketFactory(getSocketFactory());
        mqttConnectOptions.setUserName(mMqttUser);
        mqttConnectOptions.setPassword(mMqttPass.toCharArray());

        // Init Connection
        try {
            IMqttToken connectionToken = mMqttAsyncClient.connect(mqttConnectOptions);
        } catch (MqttException e) {
            Logger.e(TAG, "MQTT Client Connection Error: " + e.getMessage(), e);
        }
    }

    public static void disconnect() {
        try {
            mMqttAsyncClient.disconnect();
        } catch (MqttException e) {
            Logger.e(TAG, "MQTT Exception: " + e.getMessage(), e);
        }
    }

    public static void publish(String topic, byte[] payload) throws MqttException {
        Log.i(TAG, "Publishing MQTT Message | Topic: " + topic + " | Payload: " + payload);
        // Publish Message
        mMqttAsyncClient.publish(topic, payload, QOS_LEVEL_0, false);
    }

    public static void subscribeToTopic(String topic) {
        try {
            Logger.i(TAG, "Subscribing to topic: " + topic);
            mMqttAsyncClient.subscribe(topic, 0, null, mqttPublishCallbackHandler());
        } catch (MqttException e) {
            Logger.e(TAG, "MQTT Topic Subscription Error: " + e.getMessage(), e);
        }
    }

    public static void unsubscribeFromTopic(String topic) {
        try {
            Logger.i(TAG, "Subscribing to topic: " + topic);
            mMqttAsyncClient.unsubscribe(topic);
        } catch (MqttException e) {
            Logger.e(TAG, "MQTT Topic Subscription Error: " + e.getMessage(), e);
        }
    }

    private static void subscribeToAllTopics() {
        try {
            for (String topic : mMqttTopics) {
                Logger.i(TAG, "Subscribing to topic: " + topic);
                mMqttAsyncClient.subscribe(topic, 0, null, mqttPublishCallbackHandler());
            }
        } catch (MqttException e) {
            Logger.e(TAG, "MQTT Topic Subscription Error: " + e.getMessage(), e);
        }
    }

    private static IMqttActionListener mqttPublishCallbackHandler() {
        return new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Logger.i(TAG,"Successfully Subscribed!");
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable e) {
                Logger.e(TAG, "Failed to Subscribe: " + e.getMessage(), e);
            }
        };
    }

    private static MqttCallbackExtended mqttCallbackHandler() {
        return new MqttCallbackExtended() {
            /*
             * Called when the connection to the server is completed successfully.
             */
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                // Log connection Details
                Logger.i(TAG, "MQTT Connection Complete | Reconnect: " + reconnect
                        + " | URI: " + serverURI);

                //
                mqttConnectionCallback.onConnected();

                // Subscribe to Topics
                subscribeToAllTopics();
            }

            /*
             * This method is called when the connection to the server is lost.
             */
            @Override
            public void connectionLost(Throwable cause) {
                Logger.e(TAG, "MQTT Client Disconnected - " + cause.getMessage(), cause);

                mqttConnectionCallback.onConnectionLost();
            }

            /*
             * This method is called when a message arrives from the server.
             */
            @Override
            public void messageArrived(String topic, MqttMessage message) {
                Logger.i(TAG, "MQTT Message Arrived | Topic: " + topic
                        + " | Payload: " + new String(message.getPayload()));

                /**
                 * Three Types of messages:
                 * 1) Initial Request Message
                 *      Gets Sent to Listener Activity to allow user to accept / reject
                 * 2) Request Accepted Elsewhere
                 *      Gets sent to Listener Activity to update UI if previous message hasn't yet been rejected
                 * 3) Chat Message
                 *      Chat topic has been created & subscribed to -> all messages get updated in chatlog
                 *
                 * All messages are ignored if Listener Activity isn't 'online'
                 */

                if (topic.equals(mCurrentTopic + HELP_REQUEST_BROADCAST_TOPIC)) {
                    MessageListenerActivity.incomingAssistanceRequest(new String(message.getPayload()));
                }

                if (topic.equals(mCurrentTopic + HELP_REQUEST_BROADCAST_ACCEPTED_TOPIC)) {
                    MessageListenerActivity.cancelAssistanceRequest();
                }

                if (topic.equals(mCurrentTopic + HELP_REQUEST_PRIVATE_CHAT_TOPIC
                        + MessageActivity.mCustomerSerial + "/" + MessageActivity.mAssociateSerial)) {
                    Log.i(TAG, "Chat Message Received: " + new String(message.getPayload()));
                    MessageActivity.newMessageReceived(new String(message.getPayload()));
                }

                if (topic.equals(mCurrentTopic + HELP_REQUEST_PRIVATE_CHAT_ENDED_TOPIC
                        + MessageActivity.mCustomerSerial + "/" + Build.SERIAL)) {
                    Log.i(TAG, "Chat Ended");
                    MessageActivity.chatEndedByCustomer();
                }
            }

            /*
             * Called when delivery for a message has been completed,
             * and all acknowledgments have been received.
             */
            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Logger.i(TAG, "MQTT Message Delivery Complete - " + token.toString());
            }
        };
    }

    private static SSLSocketFactory getSocketFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, new TrustManager[]{ getVeryTrustingTrustManager() },
                    new java.security.SecureRandom());
            return sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException e) {
            Logger.e(TAG, "NoSuchAlgorithmException: " + e.getMessage(), e);
        } catch (KeyManagementException e) {
            Logger.e(TAG, "KeyManagementException: " + e.getMessage(), e);
        } return null;
    }

    private static TrustManager getVeryTrustingTrustManager() {
        return new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                Logger.i(TAG, "Returning Accepted Issuers");
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
                Logger.i(TAG, "Checking Clients Are Trusted");
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
                Logger.i(TAG, "Checking Server Is Trusted");
            }
        };
    }
}
