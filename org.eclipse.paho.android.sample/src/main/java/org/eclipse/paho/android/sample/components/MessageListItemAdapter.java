package org.eclipse.paho.android.sample.components;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.eclipse.paho.android.sample.R;
import org.eclipse.paho.android.sample.model.ReceivedMessage;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;


public class MessageListItemAdapter extends ArrayAdapter<ReceivedMessage>{

    private final Context context;
    private final ArrayList<ReceivedMessage> messages;

    public MessageListItemAdapter(Context context, ArrayList<ReceivedMessage> messages){
        super(context, R.layout.message_list_item, messages);
        this.context = context;
        this.messages = messages;

    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, @NonNull ViewGroup parent){
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.message_list_item, parent, false);
        TextView topicTextView = (TextView) rowView.findViewById(R.id.message_topic_text);
        TextView messageTextView = (TextView) rowView.findViewById(R.id.message_text);
        TextView dateTextView = (TextView) rowView.findViewById(R.id.message_date_text);
        messageTextView.setText(new String(messages.get(position).getMessage().getPayload()));
        topicTextView.setText(context.getString(R.string.topic_fmt, messages.get(position).getTopic()));
        DateFormat dateTimeFormatter = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        String shortDateStamp = dateTimeFormatter.format(messages.get(position).getTimestamp());
        dateTextView.setText(context.getString(R.string.message_time_fmt, shortDateStamp));
        return rowView;
    }
}
