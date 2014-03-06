/*
============================================================================ 
Licensed Materials - Property of IBM

5747-SM3
 
(C) Copyright IBM Corp. 1999, 2012 All Rights Reserved.
 
US Government Users Restricted Rights - Use, duplication or
disclosure restricted by GSA ADP Schedule Contract with
IBM Corp.
============================================================================
 */
package org.eclipse.paho.android.service;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

/**
 * <p>
 * The android service which interfaces with an MQTT client implementation
 * </p>
 * <p>
 * The main API of MqttService is intended to pretty much mirror the
 * IMqttAsyncClient with appropriate adjustments for the Android environment.<br>
 * These adjustments usually consist of adding two parameters to each method :-
 * <ul>
 * <li>invocationContext - a string passed from the application to identify the
 * context of the operation (mainly included for support of the javascript API
 * implementation)</li>
 * <li>activityToken - a string passed from the Activity to relate back to a
 * callback method or other context-specific data</li>
 * </ul>
 * </p>
 * <p>
 * To support multiple client connections, the bulk of the MQTT work is
 * delegated to MqttServiceClient objects. These are identified by "client
 * handle" strings, which is how the Activity, and the higher-level APIs refer
 * to them.
 * </p>
 * <p>
 * Activities using this service are expected to start it and bind to it using
 * the BIND_AUTO_CREATE flag. The life cycle of this service is based on this
 * approach.
 * </p>
 * <p>
 * Operations are highly asynchronous - in most cases results are returned to
 * the Activity by broadcasting one (or occasionally more) appropriate Intents,
 * which the Activity is expected to register a listener for.<br>
 * The Intents have an Action of
 * {@link MqttServiceConstants#CALLBACK_TO_ACTIVITY
 * MqttServiceConstants.CALLBACK_TO_ACTIVITY} which allows the Activity to
 * register a listener with an appropriate IntentFilter.<br>
 * Further data is provided by "Extra Data" in the Intent, as follows :-
 * <table border="1">
 * <tr>
 * <th align="left">Name</th>
 * <th align="left">Data Type</th>
 * <th align="left">Value</th>
 * <th align="left">Operations used for</th>
 * </tr>
 * <tr>
 * <td align="left" valign="top">
 * {@link MqttServiceConstants#CALLBACK_CLIENT_HANDLE
 * MqttServiceConstants.CALLBACK_CLIENT_HANDLE}</td>
 * <td align="left" valign="top">String</td>
 * <td align="left" valign="top">The clientHandle identifying the client which
 * initiated this operation</td>
 * <td align="left" valign="top">All operations</td>
 * </tr>
 * <tr>
 * <td align="left" valign="top">{@link MqttServiceConstants#CALLBACK_STATUS
 * MqttServiceConstants.CALLBACK_STATUS}</td>
 * <td align="left" valign="top">Serializable</td>
 * <td align="left" valign="top">An {@link Status} value indicating success or
 * otherwise of the operation</td>;
 * <td align="left" valign="top">All operations</td>
 * </tr>
 * <tr>
 * <td align="left" valign="top">
 * {@link MqttServiceConstants#CALLBACK_ACTIVITY_TOKEN
 * MqttServiceConstants.CALLBACK_ACTIVITY_TOKEN}</td>
 * <td align="left" valign="top">String</td>
 * <td align="left" valign="top">the activityToken passed into the operation</td>
 * <td align="left" valign="top">All operations</td>
 * </tr>
 * <tr>
 * <td align="left" valign="top">
 * {@link MqttServiceConstants#CALLBACK_INVOCATION_CONTEXT
 * MqttServiceConstants.CALLBACK_INVOCATION_CONTEXT}</td>
 * <td align="left" valign="top">String</td>
 * <td align="left" valign="top">the invocationContext passed into the operation
 * </td>
 * <td align="left" valign="top">All operations</td>
 * </tr>
 * <tr>
 * <td align="left" valign="top">{@link MqttServiceConstants#CALLBACK_ACTION
 * MqttServiceConstants.CALLBACK_ACTION}</td>
 * <td align="left" valign="top">String</td>
 * <td align="left" valign="top">one of
 * <table>
 * <tr>
 * <td align="left" valign="top"> {@link MqttServiceConstants#SEND_ACTION
 * MqttServiceConstants.SEND_ACTION}</td>
 * </tr>
 * <tr>
 * <td align="left" valign="top">
 * {@link MqttServiceConstants#UNSUBSCRIBE_ACTION
 * MqttServiceConstants.UNSUBSCRIBE_ACTION}</td>
 * </tr>
 * <tr>
 * <td align="left" valign="top"> {@link MqttServiceConstants#SUBSCRIBE_ACTION
 * MqttServiceConstants.SUBSCRIBE_ACTION}</td>
 * </tr>
 * <tr>
 * <td align="left" valign="top"> {@link MqttServiceConstants#DISCONNECT_ACTION
 * MqttServiceConstants.DISCONNECT_ACTION}</td>
 * </tr>
 * <tr>
 * <td align="left" valign="top"> {@link MqttServiceConstants#CONNECT_ACTION
 * MqttServiceConstants.CONNECT_ACTION}</td>
 * </tr>
 * <tr>
 * <td align="left" valign="top">
 * {@link MqttServiceConstants#MESSAGE_ARRIVED_ACTION
 * MqttServiceConstants.MESSAGE_ARRIVED_ACTION}</td>
 * </tr>
 * <tr>
 * <td align="left" valign="top">
 * {@link MqttServiceConstants#MESSAGE_DELIVERED_ACTION
 * MqttServiceConstants.MESSAGE_DELIVERED_ACTION}</td>
 * </tr>
 * <tr>
 * <td align="left" valign="top">
 * {@link MqttServiceConstants#ON_CONNECTION_LOST_ACTION
 * MqttServiceConstants.ON_CONNECTION_LOST_ACTION}</td>
 * </tr>
 * </table>
 * </td>
 * <td align="left" valign="top">All operations</td>
 * </tr>
 * <tr>
 * <td align="left" valign="top">
 * {@link MqttServiceConstants#CALLBACK_ERROR_MESSAGE
 * MqttServiceConstants.CALLBACK_ERROR_MESSAGE}
 * <td align="left" valign="top">String</td>
 * <td align="left" valign="top">A suitable error message (taken from the
 * relevant exception where possible)</td>
 * <td align="left" valign="top">All failing operations</td>
 * </tr>
 * <td align="left" valign="top">
 * {@link MqttServiceConstants#CALLBACK_ERROR_NUMBER
 * MqttServiceConstants.CALLBACK_ERROR_NUMBER}
 * <td align="left" valign="top">int</td>
 * <td align="left" valign="top">A suitable error code (taken from the relevant
 * exception where possible)</td>
 * <td align="left" valign="top">All failing operations</td>
 * </tr>
 * <tr>
 * <td align="left" valign="top">
 * {@link MqttServiceConstants#CALLBACK_EXCEPTION_STACK
 * MqttServiceConstants.CALLBACK_EXCEPTION_STACK}</td>
 * <td align="left" valign="top">String</td>
 * <td align="left" valign="top">The stacktrace of the failing call</td>
 * <td align="left" valign="top">The Connection Lost event</td>
 * </tr>
 * <tr>
 * <td align="left" valign="top">
 * {@link MqttServiceConstants#CALLBACK_MESSAGE_ID
 * MqttServiceConstants.CALLBACK_MESSAGE_ID}</td>
 * <td align="left" valign="top">String</td>
 * <td align="left" valign="top">The identifier for the message in the message
 * store, used by the Activity to acknowledge the arrival of the message, so
 * that the service may remove it from the store</td>
 * <td align="left" valign="top">The Message Arrived event</td>
 * </tr>
 * <tr>
 * <td align="left" valign="top">
 * {@link MqttServiceConstants#CALLBACK_DESTINATION_NAME
 * MqttServiceConstants.CALLBACK_DESTINATION_NAME}
 * <td align="left" valign="top">String</td>
 * <td align="left" valign="top">The topic on which the message was received</td>
 * <td align="left" valign="top">The Message Arrived event</td>
 * </tr>
 * <tr>
 * <td align="left" valign="top">
 * {@link MqttServiceConstants#CALLBACK_MESSAGE_PARCEL
 * MqttServiceConstants.CALLBACK_MESSAGE_PARCEL}</td>
 * <td align="left" valign="top">Parcelable</td>
 * <td align="left" valign="top">The new message encapsulated in Android
 * Parcelable format as a {@link ParcelableMqttMessage}</td>
 * <td align="left" valign="top">The Message Arrived event</td>
 * </tr>
 * </table >
 * </p>
 */
public class MqttService extends Service implements MqttTraceHandler {

  // Identifier for Intents, log messages, etc..
  static final String TAG = "MqttService";

  // callback id for making trace callbacks to the Activity
  // needs to be set by the activity as appropriate
  private String traceCallbackId;
  // state of tracing
  private boolean traceEnabled = false;

  // somewhere to persist received messages until we're sure
  // that they've reached the application
  MessageStore messageStore;

  // a way to pass ourself back to the activity
  private MqttServiceBinder mqttServiceBinder;

  // mapping from client handle strings to actual clients
  private Map<String/* clientHandle */, MqttServiceClient/* client */> clients = new HashMap<String, MqttServiceClient>();

  /**
   * constructor - very simple!
   * 
   */
  public MqttService() {
    super();
  }

  /**
   * pass data back to the Activity, by building a suitable Intent object and
   * broadcasting it
   * 
   * @param clientHandle
   *            source of the data
   * @param status
   *            OK or Error
   * @param dataBundle
   *            the data to be passed
   */
  void callbackToActivity(String clientHandle, Status status,
      Bundle dataBundle) {
    // Don't call traceDebug, as it will try to callbackToActivity leading
    // to recursion.
    Intent callbackIntent = new Intent(
        MqttServiceConstants.CALLBACK_TO_ACTIVITY);
    if (clientHandle != null) {
      callbackIntent.putExtra(
          MqttServiceConstants.CALLBACK_CLIENT_HANDLE, clientHandle);
    }
    callbackIntent.putExtra(MqttServiceConstants.CALLBACK_STATUS, status);
    if (dataBundle != null) {
      callbackIntent.putExtras(dataBundle);
    }
    sendBroadcast(callbackIntent);
  }

  // The major API implementation follows :-

  /**
   * get an MqttServiceClient object to represent a connection to a server
   * 
   * @param serverURI
   * @param clientId
   * @return a string to be used by the Activity as a "handle" for this
   *         MqttServiceClient
   */
  public String getClient(String serverURI, String clientId, MqttClientPersistence persistence) {
    String clientHandle = serverURI + ":" + clientId;
    if (!clients.containsKey(clientHandle)) {
      MqttServiceClient client = new MqttServiceClient(this, serverURI,
          clientId, persistence, clientHandle);
      clients.put(clientHandle, client);
    }
    return clientHandle;
  }

  /**
   * Connect to the MQTT server specified by a particular client
   * 
   * @param clientHandle
   *            identifies the MqttServiceClient to use
   * @param connectOptions
   *            the MQTT connection options to be used
   * @param invocationContext
   *            arbitrary data to be passed back to the application
   * @param activityToken
   *            arbitrary identifier to be passed back to the Activity
   * @throws MqttSecurityException
   * @throws MqttException
   */
  public void connect(String clientHandle, MqttConnectOptions connectOptions,
      String invocationContext, String activityToken)
      throws MqttSecurityException, MqttException {

    MqttServiceClient client = clientFromHandle(clientHandle);

    client.connect(connectOptions, invocationContext, activityToken);
  }

  /**
   * disconnect from the server
   * 
   * @param clientHandle
   *            identifies the MqttServiceClient to use
   * @param invocationContext
   *            arbitrary data to be passed back to the application
   * @param activityToken
   *            arbitrary identifier to be passed back to the Activity
   */
  public void disconnect(String clientHandle, String invocationContext,
      String activityToken) {
    MqttServiceClient client = clientFromHandle(clientHandle);
    client.disconnect(invocationContext, activityToken);
    clients.remove(clientHandle);

    // the activity has finished using us, so we can stop the service
    // the activities are bound with BIND_AUTO_CREATE, so the service will
    // remain around until the last activity disconnects
    stopSelf();
  }

  /**
   * disconnect from the server
   * 
   * @param clientHandle
   *            identifies the MqttServiceClient to use
   * @param quiesceTimeout
   *            in milliseconds
   * @param invocationContext
   *            arbitrary data to be passed back to the application
   * @param activityToken
   *            arbitrary identifier to be passed back to the Activity
   */
  public void disconnect(String clientHandle, long quiesceTimeout,
      String invocationContext, String activityToken) {
    MqttServiceClient client = clientFromHandle(clientHandle);
    client.disconnect(quiesceTimeout, invocationContext, activityToken);
    clients.remove(clientHandle);

    // the activity has finished using us, so we can stop the service
    // the activities are bound with BIND_AUTO_CREATE, so the service will
    // remain around until the last activity disconnects
    stopSelf();
  }

  /**
   * @param clientHandle
   *            identifies the MqttServiceClient to use
   * @return true if the specified client is connected to an MQTT server
   */
  public boolean isConnected(String clientHandle) {
    MqttServiceClient client = clientFromHandle(clientHandle);
    return client.isConnected();
  }

  /**
   * Publish a message to a topic
   * 
   * @param clientHandle
   *            identifies the MqttServiceClient to use
   * @param topic
   *            the topic to which to publish
   * @param payload
   *            the content of the message to publish
   * @param qos
   *            the quality of service requested
   * @param retained
   *            whether the MQTT server should retain this message
   * @param invocationContext
   *            arbitrary data to be passed back to the application
   * @param activityToken
   *            arbitrary identifier to be passed back to the Activity
   * @throws MqttPersistenceException
   * @throws MqttException
   * @return token for tracking the operation
   */
  public IMqttDeliveryToken publish(String clientHandle, String topic,
      byte[] payload, int qos, boolean retained,
      String invocationContext, String activityToken)
      throws MqttPersistenceException, MqttException {
    MqttServiceClient client = clientFromHandle(clientHandle);
    return client.publish(topic, payload, qos, retained, invocationContext,
        activityToken);
  }

  /**
   * Publish a message to a topic
   * 
   * @param clientHandle
   *            identifies the MqttServiceClient to use
   * @param topic
   *            the topic to which to publish
   * @param message
   *            the message to publish
   * @param invocationContext
   *            arbitrary data to be passed back to the application
   * @param activityToken
   *            arbitrary identifier to be passed back to the Activity
   * @throws MqttPersistenceException
   * @throws MqttException
   * @return token for tracking the operation
   */
  public IMqttDeliveryToken publish(String clientHandle, String topic,
      MqttMessage message, String invocationContext, String activityToken)
      throws MqttPersistenceException, MqttException {
    MqttServiceClient client = clientFromHandle(clientHandle);
    return client.publish(topic, message, invocationContext, activityToken);
  }

  /**
   * subscribe to a topic
   * 
   * @param clientHandle
   *            identifies the MqttServiceClient to use
   * @param topic
   *            a possibly wildcarded topic name
   * @param qos
   *            requested quality of service for the topic
   * @param invocationContext
   *            arbitrary data to be passed back to the application
   * @param activityToken
   *            arbitrary identifier to be passed back to the Activity
   */
  public void subscribe(String clientHandle, String topic, int qos,
      String invocationContext, String activityToken) {
    MqttServiceClient client = clientFromHandle(clientHandle);
    client.subscribe(topic, qos, invocationContext, activityToken);
  }

  /**
   * subscribe to one or more topics
   * 
   * @param clientHandle
   *            identifies the MqttServiceClient to use
   * @param topic
   *            a list of possibly wildcarded topic names
   * @param qos
   *            requested quality of service for each topic
   * @param invocationContext
   *            arbitrary data to be passed back to the application
   * @param activityToken
   *            arbitrary identifier to be passed back to the Activity
   */
  public void subscribe(String clientHandle, String[] topic, int[] qos,
      String invocationContext, String activityToken) {
    MqttServiceClient client = clientFromHandle(clientHandle);
    client.subscribe(topic, qos, invocationContext, activityToken);
  }

  /**
   * unsubscribe from a topic
   * 
   * @param clientHandle
   *            identifies the MqttServiceClient
   * @param topic
   *            a possibly wildcarded topic name
   * @param invocationContext
   *            arbitrary data to be passed back to the application
   * @param activityToken
   *            arbitrary identifier to be passed back to the Activity
   */
  public void unsubscribe(String clientHandle, final String topic,
      String invocationContext, String activityToken) {
    MqttServiceClient client = clientFromHandle(clientHandle);
    client.unsubscribe(topic, invocationContext, activityToken);
  }

  /**
   * unsubscribe from one or more topics
   * 
   * @param clientHandle
   *            identifies the MqttServiceClient
   * @param topic
   *            a list of possibly wildcarded topic names
   * @param invocationContext
   *            arbitrary data to be passed back to the application
   * @param activityToken
   *            arbitrary identifier to be passed back to the Activity
   */
  public void unsubscribe(String clientHandle, final String[] topic,
      String invocationContext, String activityToken) {
    MqttServiceClient client = clientFromHandle(clientHandle);
    client.unsubscribe(topic, invocationContext, activityToken);
  }

  /**
   * get tokens for all outstanding deliveries for a client
   * 
   * @param clientHandle
   *            identifies the MqttServiceClient
   * @return an array (possibly empty) of tokens
   */
  public IMqttDeliveryToken[] getPendingDeliveryTokens(String clientHandle) {
    MqttServiceClient client = clientFromHandle(clientHandle);
    return client.getPendingDeliveryTokens();
  }

  /**
   * @param clientHandle
   * @return the MqttServiceClient identified by this handle
   */
  private MqttServiceClient clientFromHandle(String clientHandle) {
    MqttServiceClient client = clients.get(clientHandle);
    if (client == null) {
      throw new IllegalArgumentException("Invalid ClientHandle");
    }
    return client;
  }

  /**
   * Called by the Activity when a message has been passed back to the
   * application
   * 
   * @param clientHandle
   * @param id
   */
  public Status acknowledgeMessageArrival(String clientHandle, String id) {
    if (messageStore.discardArrived(clientHandle, id)) {
      return Status.OK;
    }
    else {
      return Status.ERROR;
    }
  }

  // Extend Service

  /**
   * @see android.app.Service#onCreate()
   */
  @Override
  public void onCreate() {
    super.onCreate();

    // create a binder that will let the Activity UI send
    // commands to the Service
    mqttServiceBinder = new MqttServiceBinder(this);

    // create somewhere to buffer received messages until
    // we know that they have been passed to the application
    messageStore = new DatabaseMessageStore(this, this);

  }

  /**
   * @see android.app.Service#onDestroy()
   */
  @Override
  public void onDestroy() {
    // disconnect immediately
    for (MqttServiceClient client : clients.values()) {
      client.disconnect(null, null);
    }

    // clear down
    if (mqttServiceBinder != null) {
      mqttServiceBinder = null;
    }

    super.onDestroy();
  }

  /**
   * @see android.app.Service#onBind(Intent)
   */
  @Override
  public IBinder onBind(Intent intent) {
    // What we pass back to the Activity on binding -
    // a reference to ourself, and the activityToken
    // we were given when started
    String activityToken = intent
        .getStringExtra(MqttServiceConstants.CALLBACK_ACTIVITY_TOKEN);
    mqttServiceBinder.setActivityToken(activityToken);
    return mqttServiceBinder;
  }

  /**
   * @see android.app.Service#onStartCommand(Intent,int,int)
   */
  @Override
  public int onStartCommand(final Intent intent, int flags, final int startId) {
    // run till explicitly stopped, restart when
    // process restarted
    return START_STICKY;
  }

  /**
   * identify the callbackId to be passed when making tracing calls back into
   * the Activity
   * 
   * @param traceCallbackId
   */
  public void setTraceCallbackId(String traceCallbackId) {
    this.traceCallbackId = traceCallbackId;
  }

  /**
   * turn tracing on and off
   * 
   * @param traceEnabled
   */
  public void setTraceEnabled(boolean traceEnabled) {
    this.traceEnabled = traceEnabled;
  }

  /**
   * trace debugging information
   * 
   * @param tag
   *            identifier for the source of the trace
   * @param message
   *            the text to be traced
   */
  @Override
  public void traceDebug(String tag, String message) {
    traceCallback("debug", message);
  }

  /**
   * trace error information
   * 
   * @param tag
   *            identifier for the source of the trace
   * @param message
   *            the text to be traced
   */
  @Override
  public void traceError(String tag, String message) {
    traceCallback("error", message);
  }

  private void traceCallback(String severity, String message) {
    if ((traceCallbackId != null) && (traceEnabled)) {
      Bundle dataBundle = new Bundle();
      dataBundle.putString(MqttServiceConstants.CALLBACK_ACTION,
          MqttServiceConstants.TRACE_ACTION);
      dataBundle.putString(MqttServiceConstants.CALLBACK_TRACE_SEVERITY,
          severity);
      dataBundle.putString(MqttServiceConstants.CALLBACK_ERROR_MESSAGE,
          message);
      callbackToActivity(null, Status.ERROR, dataBundle);
    }
  }

  /**
   * trace exceptions
   * 
   * @param tag
   *            identifier for the source of the trace
   * @param message
   *            the text to be traced
   * @param e
   *            the exception
   */
  @Override
  public void traceException(String tag, String message, Exception e) {
    if (traceCallbackId != null) {
      Bundle dataBundle = new Bundle();
      dataBundle.putString(MqttServiceConstants.CALLBACK_ACTION,
          MqttServiceConstants.TRACE_ACTION);
      dataBundle.putString(MqttServiceConstants.CALLBACK_ERROR_MESSAGE,
          message);
      dataBundle.putString(MqttServiceConstants.CALLBACK_EXCEPTION_STACK,
          Log.getStackTraceString(e));
      callbackToActivity(null, Status.ERROR, dataBundle);
    }
  }

}
