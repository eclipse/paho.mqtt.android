package org.eclipse.paho.android.sample.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;

import org.eclipse.paho.android.sample.R;
import org.eclipse.paho.android.sample.internal.Connections;

import java.util.Map;


public class PublishFragment extends Fragment {

    private Connection connection;

    private int selectedQos = 0;
    private boolean retainValue = false;
    private String topic = "/test";
    private String message = "Hello world";

    public PublishFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Map<String, Connection> connections = Connections.getInstance(this.getActivity())
                .getConnections();
        connection = connections.get(this.getArguments().getString(ActivityConstants.CONNECTION_KEY));

        System.out.println("FRAGMENT CONNECTION: " + this.getArguments().getString(ActivityConstants.CONNECTION_KEY));
        System.out.println("NAME:" + connection.getId());

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_publish, container, false);
        EditText topicText = (EditText) rootView.findViewById(R.id.topic);
        EditText messageText = (EditText) rootView.findViewById(R.id.message);
        Spinner qos = (Spinner) rootView.findViewById(R.id.qos_spinner);
        final Switch retain = (Switch) rootView.findViewById(R.id.retain_switch);
        topicText.setText(topic);

        topicText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                topic = s.toString();
            }
        });

        messageText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                message = s.toString();
            }
        });

        qos.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedQos = Integer.parseInt(getResources().getStringArray(R.array.qos_options)[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        retain.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                retainValue = isChecked;
            }
        });

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(), R.array.qos_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        qos.setAdapter(adapter);

        Button publishButton = (Button) rootView.findViewById(R.id.publish_button);
        publishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("Publising: [topic: " + topic + ", message: " + message + ", QoS: " + selectedQos + ", Retain: " + retainValue + "]");
                ((MainActivity) getActivity()).publish(connection, topic, message, selectedQos, retainValue);


            }
        });


        // Inflate the layout for this fragment
        return rootView;
    }


}