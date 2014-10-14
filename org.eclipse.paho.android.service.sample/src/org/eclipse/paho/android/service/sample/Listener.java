/*******************************************************************************
 * Copyright (c) 1999, 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.paho.android.service.sample;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.LogManager;

import org.eclipse.paho.android.service.sample.R;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;

import org.eclipse.paho.android.service.sample.ActionListener.Action;
import org.eclipse.paho.android.service.sample.Connection.ConnectionStatus;
import org.eclipse.paho.android.service.MqttAndroidClient;

/**
 * Deals with actions performed in the {@link ClientConnections} activity
 * and the {@link ConnectionDetails} activity and associated fragments
 *
 */
public class Listener implements OnMenuItemClickListener {

  /** The handle to a {@link Connection} object which contains the {@link MqttAndroidClient} associated with this object **/
  private String clientHandle = null;

  /** {@link ConnectionDetails} reference used to perform some actions**/
  private ConnectionDetails connectionDetails = null;
  /** {@link ClientConnections} reference used to perform some actions**/
  private ClientConnections clientConnections = null;
  /** {@link Context} used to load and format strings **/
  private Context context = null;

  /** Whether Paho is logging is enabled**/
  static boolean logging = false;

  /**
   * Constructs a listener object for use with {@link ConnectionDetails} activity and
   * associated fragments.
   * @param connectionDetails The instance of {@link ConnectionDetails}
   * @param clientHandle The handle to the client that the actions are to be performed on
   */
  public Listener(ConnectionDetails connectionDetails, String clientHandle)
  {
    this.connectionDetails = connectionDetails;
    this.clientHandle = clientHandle;
    context = connectionDetails;

  }

  /**
   * Constructs a listener object for use with {@link ClientConnections} activity.
   * @param clientConnections The instance of {@link ClientConnections}
   */
  public Listener(ClientConnections clientConnections) {
    this.clientConnections = clientConnections;
    context = clientConnections;
  }

  /**
   * Perform the needed action required based on the button that
   * the user has clicked.
   * 
   * @param item The menu item that was clicked
   * @return If there is anymore processing to be done
   * 
   */
  @Override
  public boolean onMenuItemClick(MenuItem item) {

    int id = item.getItemId();

    switch (id)
    {
      case R.id.publish :
        publish();
        break;
      case R.id.subscribe :
        subscribe();
        break;
      case R.id.newConnection :
        createAndConnect();
        break;
      case R.id.disconnect :
        disconnect();
        break;
      case R.id.connectMenuOption :
        reconnect();
        break;
      case R.id.startLogging :
        enablePahoLogging();
        break;
      case R.id.endLogging :
        disablePahoLogging();
        break;
    }

    return false;
  }

  /**
   * Reconnect the selected client
   */
  private void reconnect() {

    Connections.getInstance(context).getConnection(clientHandle).changeConnectionStatus(ConnectionStatus.CONNECTING);

    Connection c = Connections.getInstance(context).getConnection(clientHandle);
    try {
      c.getClient().connect(c.getConnectionOptions(), null, new ActionListener(context, Action.CONNECT, clientHandle, null));
    }
    catch (MqttSecurityException e) {
      Log.e(this.getClass().getCanonicalName(), "Failed to reconnect the client with the handle " + clientHandle, e);
      c.addAction("Client failed to connect");
    }
    catch (MqttException e) {
      Log.e(this.getClass().getCanonicalName(), "Failed to reconnect the client with the handle " + clientHandle, e);
      c.addAction("Client failed to connect");
    }

  }

  /**
   * Disconnect the client
   */
  private void disconnect() {

    Connection c = Connections.getInstance(context).getConnection(clientHandle);

    //if the client is not connected, process the disconnect
    if (!c.isConnected()) {
      return;
    }

    try {
      c.getClient().disconnect(null, new ActionListener(context, Action.DISCONNECT, clientHandle, null));
      c.changeConnectionStatus(ConnectionStatus.DISCONNECTING);
    }
    catch (MqttException e) {
      Log.e(this.getClass().getCanonicalName(), "Failed to disconnect the client with the handle " + clientHandle, e);
      c.addAction("Client failed to disconnect");
    }

  }

  /**
   * Subscribe to a topic that the user has specified
   */
  private void subscribe()
  {
    String topic = ((EditText) connectionDetails.findViewById(R.id.topic)).getText().toString();
    ((EditText) connectionDetails.findViewById(R.id.topic)).getText().clear();

    RadioGroup radio = (RadioGroup) connectionDetails.findViewById(R.id.qosSubRadio);
    int checked = radio.getCheckedRadioButtonId();
    int qos = ActivityConstants.defaultQos;

    switch (checked) {
      case R.id.qos0 :
        qos = 0;
        break;
      case R.id.qos1 :
        qos = 1;
        break;
      case R.id.qos2 :
        qos = 2;
        break;
    }

    try {
      String[] topics = new String[1];
      topics[0] = topic;
      Connections.getInstance(context).getConnection(clientHandle).getClient()
          .subscribe(topic, qos, null, new ActionListener(context, Action.SUBSCRIBE, clientHandle, topics));
    }
    catch (MqttSecurityException e) {
      Log.e(this.getClass().getCanonicalName(), "Failed to subscribe to" + topic + " the client with the handle " + clientHandle, e);
    }
    catch (MqttException e) {
      Log.e(this.getClass().getCanonicalName(), "Failed to subscribe to" + topic + " the client with the handle " + clientHandle, e);
    }
  }

  /**
   * Publish the message the user has specified
   */
  private void publish()
  {
    String topic = ((EditText) connectionDetails.findViewById(R.id.lastWillTopic))
        .getText().toString();

    ((EditText) connectionDetails.findViewById(R.id.lastWillTopic)).getText().clear();

    String message = ((EditText) connectionDetails.findViewById(R.id.lastWill)).getText()
        .toString();

    ((EditText) connectionDetails.findViewById(R.id.lastWill)).getText().clear();

    RadioGroup radio = (RadioGroup) connectionDetails.findViewById(R.id.qosRadio);
    int checked = radio.getCheckedRadioButtonId();
    int qos = ActivityConstants.defaultQos;

    switch (checked) {
      case R.id.qos0 :
        qos = 0;
        break;
      case R.id.qos1 :
        qos = 1;
        break;
      case R.id.qos2 :
        qos = 2;
        break;
    }

    boolean retained = ((CheckBox) connectionDetails.findViewById(R.id.retained))
        .isChecked();

    String[] args = new String[2];
    args[0] = message;
    args[1] = topic+";qos:"+qos+";retained:"+retained;

    try {
      Connections.getInstance(context).getConnection(clientHandle).getClient()
          .publish(topic, message.getBytes(), qos, retained, null, new ActionListener(context, Action.PUBLISH, clientHandle, args));
    }
    catch (MqttSecurityException e) {
      Log.e(this.getClass().getCanonicalName(), "Failed to publish a messged from the client with the handle " + clientHandle, e);
    }
    catch (MqttException e) {
      Log.e(this.getClass().getCanonicalName(), "Failed to publish a messged from the client with the handle " + clientHandle, e);
    }

  }

  /**
   * Create a new client and connect
   */
  private void createAndConnect()
  {
    Intent createConnection;

    //start a new activity to gather information for a new connection
    createConnection = new Intent();
    createConnection.setClassName(
        clientConnections.getApplicationContext(),
        "org.eclipse.paho.android.service.sample.NewConnection");

    clientConnections.startActivityForResult(createConnection,
        ActivityConstants.connect);
  }

  /**
   * Enables logging in the Paho MQTT client
   */
  private void enablePahoLogging() {

    try {
      InputStream logPropStream = context.getResources().openRawResource(R.raw.jsr47android);
      LogManager.getLogManager().readConfiguration(logPropStream);
      logging = true;
         
      HashMap<String, Connection> connections = (HashMap<String,Connection>)Connections.getInstance(context).getConnections();
      if(!connections.isEmpty()){
    	  Entry<String, Connection> entry = connections.entrySet().iterator().next();
    	  Connection connection = (Connection)entry.getValue();
    	  connection.getClient().setTraceEnabled(true);
    	  //change menu state.
    	  clientConnections.invalidateOptionsMenu();
    	  //Connections.getInstance(context).getConnection(clientHandle).getClient().setTraceEnabled(true);
      }else{
    	  Log.i("SampleListener","No connection to enable log in service");
      }
    }
    catch (IOException e) {
      Log.e("MqttAndroidClient",
          "Error reading logging parameters", e);
    }

  }

  /**
   * Disables logging in the Paho MQTT client
   */
  private void disablePahoLogging() {
    LogManager.getLogManager().reset();
    logging = false;
    
    HashMap<String, Connection> connections = (HashMap<String,Connection>)Connections.getInstance(context).getConnections();
    if(!connections.isEmpty()){
  	  Entry<String, Connection> entry = connections.entrySet().iterator().next();
  	  Connection connection = (Connection)entry.getValue();
  	  connection.getClient().setTraceEnabled(false);
  	  //change menu state.
  	  clientConnections.invalidateOptionsMenu();
    }else{
  	  Log.i("SampleListener","No connection to disable log in service");
    }
    clientConnections.invalidateOptionsMenu();
  }

}
