package org.eclipse.paho.android.sample.activity;



import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import org.eclipse.paho.android.sample.R;
import org.eclipse.paho.android.sample.internal.Connections;
import org.eclipse.paho.android.sample.model.ConnectionModel;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements FragmentDrawer.FragmentDrawerListener{

    private FragmentDrawer drawerFragment;

    private static final String TAG = "MainActivity";

    private final ChangeListener changeListener = new ChangeListener();

    private final MainActivity mainActivity = this;

    private ArrayList<String> connectionMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        drawerFragment = (FragmentDrawer) getSupportFragmentManager().findFragmentById(R.id.fragment_navigation_drawer);
        drawerFragment.setUp(R.id.fragment_navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), mToolbar);
        drawerFragment.setDrawerListener(this);

        populateConnectionList();
    }

    public void removeConnectionRow(Connection connection){
        drawerFragment.removeConnection(connection);
        populateConnectionList();
    }


    private void populateConnectionList(){
        // Clear drawerFragment
        drawerFragment.clearConnections();

        // get all the available connections
        Map<String, Connection> connections = Connections.getInstance(this)
                .getConnections();
        int connectionIndex = 0;
        connectionMap = new ArrayList<String>();

        Iterator connectionIterator = connections.entrySet().iterator();
        while (connectionIterator.hasNext()){
            Map.Entry pair = (Map.Entry) connectionIterator.next();
            drawerFragment.addConnection((Connection) pair.getValue());
            connectionMap.add((String) pair.getKey());
            ++connectionIndex;
        }

        if(connectionMap.size() == 0){
            displayView(-1);
        } else {
            displayView(0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }



    @Override
    public void onDrawerItemSelected(int position){
        displayView(position);
    }

    @Override
    public void onDrawerItemLongSelected(int position){
        displayDeleteView(position);
    }

    @Override
    public void onAddConnectionSelected() {
        Fragment editConnectionFragment =  new EditConnectionFragment();
        String title = "Edit Connection";
        displayFragment(editConnectionFragment, title);
    }

    @Override
    public void onHelpSelected() {
        Fragment helpFragment = new HelpFragment();
        displayFragment(helpFragment, getString(R.string.help_and_feedback));

    }

    private void displayDeleteView(int position){
        if(position == -1){
            displayFragment(new HomeFragment(), "Home");
        } else {
            Fragment fragment  = new ManageConnectionFragment();
            Bundle bundle = new Bundle();
            bundle.putString(ActivityConstants.CONNECTION_KEY, connectionMap.get(position));
            fragment.setArguments(bundle);
            Map<String, Connection> connections = Connections.getInstance(this)
                    .getConnections();
            Connection connection = connections.get(connectionMap.get(position));
            displayFragment(fragment, "");
        }
    }

    private void displayView(int position){
        if(position == -1){
            displayFragment(new HomeFragment(), "Home");
        } else {
            Fragment fragment  = new ConnectionFragment();
            Bundle bundle = new Bundle();
            bundle.putString(ActivityConstants.CONNECTION_KEY, connectionMap.get(position));
            fragment.setArguments(bundle);
            Map<String, Connection> connections = Connections.getInstance(this)
                    .getConnections();
            Connection connection = connections.get(connectionMap.get(position));
            String title = connection.getId();
            displayFragment(fragment, title);
        }
    }

    private void displayFragment(Fragment fragment, String title){
        if (fragment != null){
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.container_body, fragment);
            fragmentTransaction.commit();

            // Set Toolbar Title
            getSupportActionBar().setTitle(title);


        }
    }

    public void updateAndConnect(ConnectionModel model){
        Map<String, Connection> connections = Connections.getInstance(this)
                .getConnections();

        Log.i(TAG, "Updating connection: " + connections.keySet().toString());

        Connection connection = connections.get(model.getClientHandle());
        // First disconnect the current instance of this connection
        if(connection.isConnected()){
            connection.changeConnectionStatus(Connection.ConnectionStatus.DISCONNECTING);
            connection.getClient().disconnect();
        }
        // Update the connection.
        connection.updateConnection(model.getClientId(), model.getServerHostName(), model.getServerPort(), model.isTlsConnection());
        connection.changeConnectionStatus(Connection.ConnectionStatus.CONNECTING);

        String[] actionArgs = new String[1];
        actionArgs[0] = model.getClientId();
        final ActionListener callback = new ActionListener(this,
                ActionListener.Action.CONNECT, connection, actionArgs);
        connection.getClient().setCallback(new MqttCallbackHandler(this, model.getClientHandle()));

        connection.getClient().setTraceCallback(new MqttTraceCallback());
        MqttConnectOptions connOpts = optionsFromModel(model);
        connection.addConnectionOptions(connOpts);
        Connections.getInstance(this).updateConnection(connection);
        drawerFragment.updateConnection(connection);

        connection.getClient().connect(connOpts, null, callback);
        Fragment fragment  = new ConnectionFragment();
        Bundle bundle = new Bundle();
        bundle.putString(ActivityConstants.CONNECTION_KEY, connection.handle());
        fragment.setArguments(bundle);
        String title = connection.getId();
        displayFragment(fragment, title);
    }


    /**
     * Takes a {@link ConnectionModel} and uses it to connect
     * and then persist.
     * @param model - The connection Model
     */
    public void persistAndConnect(ConnectionModel model){
        Log.i(TAG, "Persisting new connection:" + model.getClientHandle());
        Connection connection = Connection.createConnection(model.getClientHandle(),model.getClientId(),model.getServerHostName(),model.getServerPort(),this,model.isTlsConnection());
        connection.registerChangeListener(changeListener);
        connection.changeConnectionStatus(Connection.ConnectionStatus.CONNECTING);


        String[] actionArgs = new String[1];
        actionArgs[0] = model.getClientId();
        final ActionListener callback = new ActionListener(this,
                ActionListener.Action.CONNECT, connection, actionArgs);
        connection.getClient().setCallback(new MqttCallbackHandler(this, model.getClientHandle()));



        connection.getClient().setTraceCallback(new MqttTraceCallback());

        MqttConnectOptions connOpts = optionsFromModel(model);

        connection.addConnectionOptions(connOpts);
        Connections.getInstance(this).addConnection(connection);
        connectionMap.add(model.getClientHandle());
        drawerFragment.addConnection(connection);

        connection.getClient().connect(connOpts, null, callback);
        Fragment fragment  = new ConnectionFragment();
        Bundle bundle = new Bundle();
        bundle.putString(ActivityConstants.CONNECTION_KEY, connection.handle());
        bundle.putBoolean(ActivityConstants.CONNECTED, true);
        fragment.setArguments(bundle);
        String title = connection.getId();
        displayFragment(fragment, title);
    }





    private MqttConnectOptions optionsFromModel(ConnectionModel model){

        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(model.isCleanSession());
        connOpts.setConnectionTimeout(model.getTimeout());
        connOpts.setKeepAliveInterval(model.getKeepAlive());
        if(!model.getUsername().equals(ActivityConstants.empty)){
            connOpts.setUserName(model.getUsername());
        }

        if(!model.getPassword().equals(ActivityConstants.empty)){
            connOpts.setPassword(model.getPassword().toCharArray());
        }
        if(!model.getLwtTopic().equals(ActivityConstants.empty) && !model.getLwtMessage().equals(ActivityConstants.empty)){
            connOpts.setWill(model.getLwtTopic(), model.getLwtMessage().getBytes(), model.getLwtQos(), model.isLwtRetain());
        }
        //   if(tlsConnection){
        //       // TODO Add Keys to conOpts here
        //       //connOpts.setSocketFactory();
        //   }
        return connOpts;
    }




    public void connect(Connection connection) {
        String[] actionArgs = new String[1];
        actionArgs[0] = connection.getId();
        final ActionListener callback = new ActionListener(this,
                ActionListener.Action.CONNECT, connection, actionArgs);
        connection.getClient().setCallback(new MqttCallbackHandler(this, connection.handle()));
        connection.getClient().connect(connection.getConnectionOptions(), null, callback);
    }

    public void disconnect(Connection connection){
        connection.getClient().disconnect();
    }

    public void publish(Connection connection, String topic, String message, int qos, boolean retain){
        String[] actionArgs = new String[2];
        actionArgs[0] = message;
        actionArgs[1] = topic;
        final ActionListener callback = new ActionListener(this,
                ActionListener.Action.PUBLISH, connection, actionArgs);
        connection.getClient().publish(topic, message.getBytes(), qos, retain, null, callback);
    }

    /**
     * This class ensures that the user interface is updated as the Connection objects change their states
     *
     *
     */
    private class ChangeListener implements PropertyChangeListener {

        /**
         * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
         */
        @Override
        public void propertyChange(PropertyChangeEvent event) {

            if (!event.getPropertyName().equals(ActivityConstants.ConnectionStatusProperty)) {
                return;
            }
            mainActivity.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    mainActivity.drawerFragment.notifyDataSetChanged();
                }

            });

        }

    }
}
