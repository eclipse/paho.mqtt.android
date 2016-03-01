package org.eclipse.paho.android.sample.components;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.eclipse.paho.android.sample.R;
import org.eclipse.paho.android.sample.internal.Persistence;
import org.eclipse.paho.android.sample.model.ReceivedMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;


public class MessageListItemAdapter extends ArrayAdapter<ReceivedMessage>{

    private final Context context;
    private final ArrayList<ReceivedMessage> messages;
    TextView messageTextView;
    TextView  topicTextView;
    TextView dateTextView;

    public MessageListItemAdapter(Context context, ArrayList<ReceivedMessage> messages){
        super(context, R.layout.message_list_item, messages);
        this.context = context;
        this.messages = messages;

    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent){
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.message_list_item, parent, false);
        topicTextView = (TextView) rowView.findViewById(R.id.message_topic_text);
        messageTextView = (TextView) rowView.findViewById(R.id.message_text);
        dateTextView = (TextView) rowView.findViewById(R.id.message_date_text);
        messageTextView.setText(new String(messages.get(position).getMessage().getPayload()));
        topicTextView.setText(new String("Topic: " + messages.get(position).getTopic()));
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        String shortDateStamp = format.format(messages.get(position).getTimestamp());
        dateTextView.setText("Time: " + shortDateStamp);
        return rowView;
    }
}
