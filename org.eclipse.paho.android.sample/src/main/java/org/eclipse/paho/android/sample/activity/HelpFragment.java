package org.eclipse.paho.android.sample.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

import org.eclipse.paho.android.sample.R;
import org.eclipse.paho.android.sample.internal.Connections;

import java.util.Map;


public class HelpFragment extends Fragment {

    private static final String TAG = "HelpFragment";

    private static final String FEEDBACK_EMAIL = "paho-dev@eclipse.org";
    private static final String FEEDBACK_SUBJECT = "Eclipse Paho Android Sample Feedback";
    private static final String FEEDBACK_VERSION = "App Version: ";
    private static final String FEEDBACK_PHONE_MODEL = "Phone Model: ";
    private static final String FEEDBACK_ANDROID_VERSION = "Android SDK Version: ";
    private static final String FEEDBACK_UNKNOWN = "Unknown";
    private static final String FEEDBACK_NEW_LINE = "\r\n";

    private static final String PAHO_WEBSITE = "http://www.eclipse.org/paho/";


    public HelpFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        final View rootView = inflater.inflate(R.layout.fragment_help, container, false);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(rootView.getContext());


        Button websiteButton = (Button) rootView.findViewById(R.id.websiteButton);
        websiteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Opening Web Browser to Paho Website");
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(PAHO_WEBSITE));
                startActivity(browserIntent);
            }
        });

        Button feedbackButton = (Button) rootView.findViewById(R.id.feedbackButton);
        feedbackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Preparing Feedback Email.");
                Uri data = Uri.parse("mailto:" + FEEDBACK_EMAIL + "?subject=" + FEEDBACK_SUBJECT + "&body=" + getDebugInfoForEmail());
                Intent feedbackIntent = new Intent(Intent.ACTION_VIEW, data);
                startActivity(feedbackIntent);
            }
        });

        Switch enableLoggingSwitch = (Switch) rootView.findViewById(R.id.enable_logging_switch);
        enableLoggingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Map<String, Connection> connections = Connections.getInstance(rootView.getContext())
                        .getConnections();
                if(!connections.isEmpty()){
                    Map.Entry<String, Connection> entry = connections.entrySet().iterator().next();
                    Connection connection = entry.getValue();
                    connection.getClient().setTraceEnabled(isChecked);
                    if(isChecked){
                        connection.getClient().setTraceCallback(new MqttTraceCallback());
                    }
                    Log.i(TAG, "Trace was set to: " + isChecked);
                } else {
                    Log.i(TAG, "No Connection available to enable / disable trace on.");
                }
            }
        });



        // Inflate the layout for this fragment
        return rootView;
    }


    private String getDebugInfoForEmail(){
        StringBuilder sb = new StringBuilder();

        try {
            PackageInfo pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            sb.append(FEEDBACK_VERSION + pInfo.versionName + FEEDBACK_NEW_LINE);
        } catch(PackageManager.NameNotFoundException ex){
            sb.append(FEEDBACK_VERSION + FEEDBACK_UNKNOWN + FEEDBACK_NEW_LINE);
        }

        sb.append(FEEDBACK_PHONE_MODEL + Build.MANUFACTURER + " " +Build.MODEL + FEEDBACK_NEW_LINE);
        sb.append(FEEDBACK_ANDROID_VERSION + Build.VERSION.SDK_INT);



        return sb.toString();
    }
}