package org.eclipse.paho.android.sample.components;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.eclipse.paho.android.sample.R;
import org.eclipse.paho.android.sample.model.Subscription;

import java.util.ArrayList;

public class SubscriptionListItemAdapter extends ArrayAdapter<Subscription>{

    private final Context context;
    private final ArrayList<Subscription> topics;
    private final ArrayList<OnUnsubscribeListner> unsubscribeListners = new ArrayList<OnUnsubscribeListner>();
    //private final Map<String, String> topics;

    public SubscriptionListItemAdapter(Context context, ArrayList<Subscription> topics){
        super(context, R.layout.subscription_list_item, topics);
        this.context = context;
        this.topics = topics;

    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, @NonNull ViewGroup parent){
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.subscription_list_item, parent, false);
        TextView topicTextView = (TextView) rowView.findViewById(R.id.message_text);
        ImageView topicDeleteButton = (ImageView) rowView.findViewById(R.id.topic_delete_image);
        TextView  qosTextView = (TextView) rowView.findViewById(R.id.qos_label);
        topicTextView.setText(topics.get(position).getTopic());
        String qosString = context.getString(R.string.qos_text, topics.get(position).getQos());
        qosTextView.setText(qosString);
        TextView notifyTextView = (TextView) rowView.findViewById(R.id.show_notifications_label);
        String notifyString = context.getString(R.string.notify_text, (topics.get(position).isEnableNotifications() ? context.getString(R.string.enabled) : context.getString(R.string.disabled)));
        notifyTextView.setText(notifyString);

        topicDeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                for (OnUnsubscribeListner listner : unsubscribeListners) {
                    listner.onUnsubscribe(topics.get(position));
                }
                topics.remove(position);
                notifyDataSetChanged();
            }
        });

        return rowView;
    }

    public void addOnUnsubscribeListner(OnUnsubscribeListner listner){
        unsubscribeListners.add(listner);
    }

    public interface OnUnsubscribeListner{
        void onUnsubscribe(Subscription subscription);
    }



}
