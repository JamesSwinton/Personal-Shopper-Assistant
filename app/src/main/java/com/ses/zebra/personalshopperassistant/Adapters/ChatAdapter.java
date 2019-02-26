package com.ses.zebra.personalshopperassistant.Adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ses.zebra.personalshopperassistant.Debugging.Logger;
import com.ses.zebra.personalshopperassistant.POJOs.HelpMessage;
import com.ses.zebra.personalshopperassistant.R;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // Debugging
    private static final String TAG = "BasketListAdapter";

    // Constants
    private static final int NO_MESSAGE_VIEW_TYPE = 0;
    private static final int SENT_MESSAGE_VIEW_TYPE = 1;
    private static final int RECEIVED_MESSAGE_VIEW_TYPE = 2;

    // Variables
    private List<HelpMessage> mMessageList;

    public ChatAdapter(List<HelpMessage> messageList) {
        this.mMessageList = messageList;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case NO_MESSAGE_VIEW_TYPE:
                return new NoMessageHolder(LayoutInflater.from(
                        parent.getContext()).inflate(R.layout.adapter_chat_no_message, parent, false));
            case SENT_MESSAGE_VIEW_TYPE:
                return new SentMessageHolder(LayoutInflater.from(
                        parent.getContext()).inflate(R.layout.adapter_chat_message_sent, parent, false));
            case RECEIVED_MESSAGE_VIEW_TYPE:
                return new ReceivedMessageHolder(LayoutInflater.from(
                        parent.getContext()).inflate(R.layout.adapter_chat_message_received, parent, false));
            default:
                return new NoMessageHolder(null);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        switch(viewHolder.getItemViewType()) {
            case NO_MESSAGE_VIEW_TYPE:
                Logger.i(TAG, "Showing No Message ViewHolder");
                break;
            case SENT_MESSAGE_VIEW_TYPE:
                Logger.i(TAG, "Showing Sent Message ViewHolder");
                // Cast ViewHolder to SentMessageViewHolder
                SentMessageHolder sentViewHolder = (SentMessageHolder) viewHolder;
                // Update Values
                sentViewHolder.mMessageText.setText(mMessageList.get(position).getMessage());
                sentViewHolder.mMessageTime.setText(mMessageList.get(position).getSentTime());
                break;
            case RECEIVED_MESSAGE_VIEW_TYPE:
                Logger.i(TAG, "Showing Received Message ViewHolder");
                // Cast ViewHolder to SentMessageViewHolder
                ReceivedMessageHolder receivedMessageHolder = (ReceivedMessageHolder) viewHolder;
                // Update Values
                receivedMessageHolder.mMessageText.setText(mMessageList.get(position).getMessage());
                receivedMessageHolder.mMessageTime.setText(mMessageList.get(position).getSentTime());
        }
    }

    @Override
    public int getItemCount() {
        // Only 1 item = empty list = show empty view holder
        return mMessageList.isEmpty() ? 1 : mMessageList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mMessageList.size() == 0 ? NO_MESSAGE_VIEW_TYPE
                : mMessageList.get(position).getSender() == HelpMessage.MessageType.SENT
                ? SENT_MESSAGE_VIEW_TYPE : RECEIVED_MESSAGE_VIEW_TYPE;
    }

    public void updateMessageList(List<HelpMessage> messageList) {
        this.mMessageList = messageList;
        notifyDataSetChanged();
    }

    public static class NoMessageHolder extends RecyclerView.ViewHolder {
        NoMessageHolder(View view) {
            super(view);
        }
    }

    public static class SentMessageHolder extends RecyclerView.ViewHolder {

        // List Elements
        TextView mMessageText;
        TextView mMessageTime;

        SentMessageHolder(View view) {
            super(view);
            mMessageText = view.findViewById(R.id.messageText);
            mMessageTime = view.findViewById(R.id.messageSentTime);
        }
    }

    public static class ReceivedMessageHolder extends RecyclerView.ViewHolder {

        // List Elements
        TextView mMessageText;
        TextView mMessageTime;

        ReceivedMessageHolder(View view) {
            super(view);
            mMessageText = view.findViewById(R.id.messageText);
            mMessageTime = view.findViewById(R.id.messageSentTime);
        }
    }
}
