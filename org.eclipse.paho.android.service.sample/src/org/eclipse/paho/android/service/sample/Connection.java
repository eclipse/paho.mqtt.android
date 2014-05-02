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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import org.eclipse.paho.android.service.sample.R;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import android.content.Context;
import android.text.Html;
import android.text.Spanned;
import org.eclipse.paho.android.service.MqttAndroidClient;

/**
 * 
 * Represents a {@link MqttAndroidClient} and the actions it has performed
 *
 */
public class Connection {

  /*
   * Basic Information about the client
   */
  /** ClientHandle for this Connection Object**/
  private String clientHandle = null;
  /** The clientId of the client associated with this <code>Connection</code> object **/
  private String clientId = null;
  /** The host that the {@link MqttAndroidClient} represented by this <code>Connection</code> is represented by **/
  private String host = null;
  /** The port on the server this client is connecting to **/
  private int port = 0;
  /** {@link ConnectionStatus} of the {@link MqttAndroidClient} represented by this <code>Connection</code> object. Default value is {@link ConnectionStatus#NONE} **/
  private ConnectionStatus status = ConnectionStatus.NONE;
  /** The history of the {@link MqttAndroidClient} represented by this <code>Connection</code> object **/
  private ArrayList<String> history = null;
  /** The {@link MqttAndroidClient} instance this class represents**/
  private MqttAndroidClient client = null;

  /** Collection of {@link PropertyChangeListener} **/
  private ArrayList<PropertyChangeListener> listeners = new ArrayList<PropertyChangeListener>();

  /** The {@link Context} of the application this object is part of**/
  private Context context = null;

  /** The {@link MqttConnectOptions} that were used to connect this client**/
  private MqttConnectOptions conOpt;

  /** True if this connection is secured using SSL **/
  private boolean sslConnection = false;

  /** Persistence id, used by {@link Persistence} **/
  private long persistenceId = -1;

  /**
   * Connections status for  a connection
   */
  enum ConnectionStatus {

    /** Client is Connecting **/
    CONNECTING,
    /** Client is Connected **/
    CONNECTED,
    /** Client is Disconnecting **/
    DISCONNECTING,
    /** Client is Disconnected **/
    DISCONNECTED,
    /** Client has encountered an Error **/
    ERROR,
    /** Status is unknown **/
    NONE
  }

  /**
   * Creates a connection from persisted information in the database store, attempting
   * to create a {@link MqttAndroidClient} and the client handle.
   * @param clientId The id of the client
   * @param host the server which the client is connecting to
   * @param port the port on the server which the client will attempt to connect to
   * @param context the application context
   * @param sslConnection true if the connection is secured by SSL
   * @return a new instance of <code>Connection</code>
   */
  public static Connection createConnection(String clientId, String host,
      int port, Context context, boolean sslConnection) {
    String handle = null;
    String uri = null;
    if (sslConnection) {
      uri = "ssl://" + host + ":" + port;
      handle = uri + clientId;
    }
    else {
      uri = "tcp://" + host + ":" + port;
      handle = uri + clientId;
    }
    MqttAndroidClient client = new MqttAndroidClient(context, uri, clientId);
    return new Connection(handle, clientId, host, port, context, client, sslConnection);

  }

  /**
   * Creates a connection object with the server information and the client
   * hand which is the reference used to pass the client around activities
   * @param clientHandle The handle to this <code>Connection</code> object
   * @param clientId The Id of the client
   * @param host The server which the client is connecting to
   * @param port The port on the server which the client will attempt to connect to
   * @param context The application context
   * @param client The MqttAndroidClient which communicates with the service for this connection
   * @param sslConnection true if the connection is secured by SSL
   */
  public Connection(String clientHandle, String clientId, String host,
      int port, Context context, MqttAndroidClient client, boolean sslConnection) {
    //generate the client handle from its hash code
    this.clientHandle = clientHandle;
    this.clientId = clientId;
    this.host = host;
    this.port = port;
    this.context = context;
    this.client = client;
    this.sslConnection = sslConnection;
    history = new ArrayList<String>();
    StringBuffer sb = new StringBuffer();
    sb.append("Client: ");
    sb.append(clientId);
    sb.append(" created");
    addAction(sb.toString());
  }

  /**
   * Add an action to the history of the client
   * @param action the history item to add
   */
  public void addAction(String action) {

    Object[] args = new String[1];
    SimpleDateFormat sdf = new SimpleDateFormat(context.getString(R.string.dateFormat));
    args[0] = sdf.format(new Date());

    String timestamp = context.getString(R.string.timestamp, args);
    history.add(action + timestamp);

    notifyListeners(new PropertyChangeEvent(this, ActivityConstants.historyProperty, null, null));
  }

  /**
   * Generate an array of Spanned items representing the history of this
   * connection. 
   * 
   * @return an array of history entries
   */
  public Spanned[] history() {

    int i = 0;
    Spanned[] array = new Spanned[history.size()];

    for (String s : history) {
      if (s != null) {
        array[i] = Html.fromHtml(s);
        i++;
      }
    }

    return array;

  }

  /**
   * Gets the client handle for this connection
   * @return client Handle for this connection
   */
  public String handle() {
    return clientHandle;
  }

  /**
   * Determines if the client is connected
   * @return is the client connected
   */
  public boolean isConnected() {
    return status == ConnectionStatus.CONNECTED;
  }

  /**
   * Changes the connection status of the client
   * @param connectionStatus The connection status of this connection
   */
  public void changeConnectionStatus(ConnectionStatus connectionStatus) {
    status = connectionStatus;
    notifyListeners((new PropertyChangeEvent(this, ActivityConstants.ConnectionStatusProperty, null, null)));
  }

  /**
   * A string representing the state of the client this connection
   * object represents
   * 
   * 
   * @return A string representing the state of the client
   */
  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(clientId);
    sb.append("\n ");

    switch (status) {

      case CONNECTED :
        sb.append(context.getString(R.string.connectedto));
        break;
      case DISCONNECTED :
        sb.append(context.getString(R.string.disconnected));
        break;
      case NONE :
        sb.append(context.getString(R.string.no_status));
        break;
      case CONNECTING :
        sb.append(context.getString(R.string.connecting));
        break;
      case DISCONNECTING :
        sb.append(context.getString(R.string.disconnecting));
        break;
      case ERROR :
        sb.append(context.getString(R.string.connectionError));
    }
    sb.append(" ");
    sb.append(host);

    return sb.toString();
  }

  /**
   * Determines if a given handle refers to this client
   * @param handle The handle to compare with this clients handle
   * @return true if the handles match
   */
  public boolean isHandle(String handle) {
    return clientHandle.equals(handle);
  }

  /**
   * Compares two connection objects for equality
   * this only takes account of the client handle
   * @param o The object to compare to
   * @return true if the client handles match
   */
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Connection)) {
      return false;
    }

    Connection c = (Connection) o;

    return clientHandle.equals(c.clientHandle);

  }

  /**
   * Get the client Id for the client this object represents
   * @return the client id for the client this object represents
   */
  public String getId() {
    return clientId;
  }

  /**
   * Get the host name of the server that this connection object is associated with
   * @return the host name of the server this connection object is associated with
   */
  public String getHostName() {

    return host;
  }

  /**
   * Determines if the client is in a state of connecting or connected.
   * @return if the client is connecting or connected
   */
  public boolean isConnectedOrConnecting() {
    return (status == ConnectionStatus.CONNECTED) || (status == ConnectionStatus.CONNECTING);
  }

  /**
   * Client is currently not in an error state
   * @return true if the client is in not an error state
   */
  public boolean noError() {
    return status != ConnectionStatus.ERROR;
  }

  /**
   * Gets the client which communicates with the android service.
   * @return the client which communicates with the android service
   */
  public MqttAndroidClient getClient() {
    return client;
  }

  /**
   * Add the connectOptions used to connect the client to the server
   * @param connectOptions the connectOptions used to connect to the server
   */
  public void addConnectionOptions(MqttConnectOptions connectOptions) {
    conOpt = connectOptions;

  }

  /**
   * Get the connectOptions used to connect this client to the server
   * @return The connectOptions used to connect the client to the server
   */
  public MqttConnectOptions getConnectionOptions()
  {
    return conOpt;
  }

  /**
   * Register a {@link PropertyChangeListener} to this object
   * @param listener the listener to register
   */
  public void registerChangeListener(PropertyChangeListener listener)
  {
    listeners.add(listener);
  }

  /**
   * Remove a registered {@link PropertyChangeListener}
   * @param listener A reference to the listener to remove
   */
  public void removeChangeListener(PropertyChangeListener listener)
  {
    if (listener != null) {
      listeners.remove(listener);
    }
  }

  /**
   * Notify {@link PropertyChangeListener} objects that the object has been updated
   * @param propertyChangeEvent 
   */
  private void notifyListeners(PropertyChangeEvent propertyChangeEvent)
  {
    for (PropertyChangeListener listener : listeners)
    {
      listener.propertyChange(propertyChangeEvent);
    }
  }

  /**
   * Gets the port that this connection connects to.
   * @return port that this connection connects to
   */
  public int getPort() {
    return port;
  }

  /**
   * Determines if the connection is secured using SSL, returning a C style integer value
   * @return 1 if SSL secured 0 if plain text
   */
  public int isSSL() {
    return sslConnection ? 1 : 0;
  }

  /**
   * Assign a persistence ID to this object
   * @param id the persistence id to assign
   */
  public void assignPersistenceId(long id) {
    persistenceId = id;
  }

  /**
   * Returns the persistence ID assigned to this object
   * @return the persistence ID assigned to this object
   */
  public long persistenceId() {
    return persistenceId;
  }
}
