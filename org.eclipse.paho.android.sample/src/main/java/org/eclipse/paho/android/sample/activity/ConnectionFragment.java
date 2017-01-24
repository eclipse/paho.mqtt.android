package org.eclipse.paho.android.sample.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTabHost;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import org.eclipse.paho.android.sample.R;
import org.eclipse.paho.android.sample.internal.Connections;
import java.util.Map;


public class ConnectionFragment extends Fragment {
    private Connection connection;
    private FragmentTabHost mTabHost;
    private Switch connectSwitch;

    public ConnectionFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Map<String, Connection> connections = Connections.getInstance(this.getActivity())
                .getConnections();
        connection = connections.get(this.getArguments().getString(ActivityConstants.CONNECTION_KEY));
        boolean connected = this.getArguments().getBoolean(ActivityConstants.CONNECTED, false);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_connection, container, false);




        Bundle bundle = new Bundle();
        bundle.putString(ActivityConstants.CONNECTION_KEY, connection.handle());

        // Initialise the tab-host
        mTabHost = (FragmentTabHost) rootView.findViewById(android.R.id.tabhost);
        mTabHost.setup(getActivity(), getChildFragmentManager(), android.R.id.tabcontent);
        // Add a tab to the tabHost
        mTabHost.addTab(mTabHost.newTabSpec("History").setIndicator("History"), HistoryFragment.class, bundle);
        mTabHost.addTab(mTabHost.newTabSpec("Publish").setIndicator("Publish"), PublishFragment.class, bundle);
        mTabHost.addTab(mTabHost.newTabSpec("Subscribe").setIndicator("Subscribe"), SubscriptionFragment.class, bundle);
        return rootView;

    }

    private void changeConnectedState(boolean state){
        mTabHost.getTabWidget().getChildTabViewAt(1).setEnabled(state);
        mTabHost.getTabWidget().getChildTabViewAt(2).setEnabled(state);
        connectSwitch.setChecked(state);
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, MenuInflater inflater){
        inflater.inflate(R.menu.menu_connection, menu);


        connectSwitch = (Switch)  menu.findItem(R.id.connect_switch).getActionView().findViewById(R.id.switchForActionBar);

        connectSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    ((MainActivity) getActivity()).connect(connection);
                    changeConnectedState(true);
                } else {
                    ((MainActivity) getActivity()).disconnect(connection);
                    changeConnectedState(false);
                }
            }
        });
        changeConnectedState(connection.isConnected());
        super.onCreateOptionsMenu(menu, inflater);
    }

}

