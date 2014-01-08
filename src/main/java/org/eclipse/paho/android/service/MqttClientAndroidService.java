/*
 * Copyright (c) 2009, 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.eclipse.paho.android.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.util.SparseArray;

/**
 * <p>
 * Implementation of the MQTT asynchronous client interface, using the MQTT
 * Android service to actually interface with MQTT.
 * </p>
 */
public class MqttClientAndroidService extends BroadcastReceiver implements IMqttAsyncClient {

	/**
	 * 
	 * The Acknowledgment mode for messages received from {@link MqttCallback#messageArrived(String, MqttMessage)}
	 *
	 */
	public enum Ack {
		/**
		 * As soon as the {@link MqttCallback#messageArrived(String, MqttMessage)} returns
		 * the message has been acknowledged as received .
		 */
		AUTO_ACK,
		/**
		 * When {@link MqttCallback#messageArrived(String, MqttMessage)} returns the message
		 * will not be acknowledged as received, the application will have to make an acknowledgment call
		 * to {@link MqttClientAndroidService} using {@link MqttClientAndroidService#acknowledgeMessage(MqttMessage)}
		 */
		MANUAL_ACK
	}

	private static final String SERVICE_NAME = "com.ibm.msg.android.service.MqttService";

	private static final int BIND_SERVICE_FLAG = 0;

	private static ExecutorService pool = Executors.newCachedThreadPool();

	/**
	 * ServiceConnection to process when we bind to our service
	 */
	private final class MyServiceConnection implements ServiceConnection {

		@Override
		public void onServiceConnected(ComponentName name, IBinder binder) {
			mqttService = ((MqttServiceBinder) binder).getService();
			// now that we have the service available, we can actually
			// connect...
			doConnect();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mqttService = null;
		}
	}

	// Listener for when the service is connected or disconnected
	private MyServiceConnection serviceConnection = new MyServiceConnection();

	// The Android Service which will process our mqtt calls
	private MqttService mqttService;

	// An identifier for the underlying client connection, which we can pass to
	// the service
	private String clientHandle;

	Context myContext;

	// We hold the various tokens in a collection and pass identifiers for them
	// to the service
	private SparseArray<IMqttToken> tokenMap = new SparseArray<IMqttToken>();
	private int tokenNumber = 0;

	// Connection data
	private String serverURI;
	private String clientId;
	private MqttClientPersistence persistence = null;
	private MqttConnectOptions connectOptions;
	private IMqttToken connectToken;

	// The MqttCallback provided by the application
	private MqttCallback callback;
	private MqttTraceHandler traceCallback;

	//The acknowledgment that a message has been processed by the application
	private Ack messageAck;
	private boolean traceEnabled = false;

	/**
	 * Constructor
	 * 
	 * @param context
	 * @param serverURI
	 *            specifies the protocol, host name and port to be used to
	 *            connect to an MQTT server
	 * @param clientId
	 *            specifies the name by which this connection should be
	 *            identified to the server
	 */
	public MqttClientAndroidService(Context context, String serverURI,
		String clientId) {
		this(context, serverURI, clientId, null, Ack.AUTO_ACK);
	}

	/**
	 *  Constructor
	 * @param ctx Application's context
	 * @param serverURI specifies the protocol, host name and port to be used to connect to an MQTT server
	 * @param clientId specifies the name by which this connection should be identified to the server
	 * @param ackType how the application wishes to acknowledge a message has been processed
	 */
	public MqttClientAndroidService(Context ctx, String serverURI, String clientId, Ack ackType) {
		this(ctx, serverURI, clientId, null, ackType);
	}

	/**
	 * Constructor
	 * @param ctx Application's context
	 * @param serverURI specifies the protocol, host name and port to be used to connect to an MQTT server
	 * @param clientId specifies the name by which this connection should be identified to the server
	 * @param persistence The object to use to store persisted data
	 */
	public MqttClientAndroidService(Context ctx, String serverURI, String clientId, MqttClientPersistence persistence) {
		this(ctx, serverURI, clientId, null, Ack.AUTO_ACK);
	}

	/**
	 * constructor
	 * 
	 * @param context
	 * @param serverURI
	 *            specifies the protocol, host name and port to be used to
	 *            connect to an MQTT server
	 * @param clientId
	 *            specifies the name by which this connection should be
	 *            identified to the server
	 * @param persistence
	 * @param ackType how the application wishes to acknowledge a message has been processed.
	 */
	public MqttClientAndroidService(Context context, String serverURI,
		String clientId, MqttClientPersistence persistence, Ack ackType) {
		myContext = context;
		this.serverURI = serverURI;
		this.clientId = clientId;
		this.persistence = persistence;
		messageAck = ackType;
	}

	/**
	 * @return whether or not we are connected
	 */
	@Override
	public boolean isConnected() {
		return mqttService.isConnected(clientHandle);
	}

	/**
	 * @return the clientId by which we identify ourself to the mqtt server
	 */
	@Override
	public String getClientId() {
		return clientId;
	}

	/**
	 * @return the mqtt server URI
	 */
	@Override
	public String getServerURI() {
		return serverURI;
	}

	/**
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#close()
	 */
	@Override
	public void close() {
		// Nothing to do TODO should pass this over to paho
	}

	/**
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#connect()
	 */
	@Override
	public IMqttToken connect() throws MqttException {
		return connect(null, null);
	}

	/**
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#connect(MqttConnectOptions)
	 */
	@Override
	public IMqttToken connect(MqttConnectOptions options) throws MqttException {
		return connect(options, null, null);
	}

	/**
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#connect(Object,
	 *      IMqttActionListener)
	 */
	@Override
	public IMqttToken connect(Object userContext, IMqttActionListener callback)
	throws MqttException {
		return connect(new MqttConnectOptions(), userContext, callback);
	}

	/**
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#connect(Object,
	 *      IMqttActionListener)
	 */
	/*
	 * The actual connection depends on the service, which we start and bind to
	 * here, but which we can't actually use until the serviceConnection
	 * onServiceConnected() method has run (asynchronously), so the connection
	 * itself takes place in the onServiceConnected() method
	 */
	@Override
	public IMqttToken connect(MqttConnectOptions options, Object userContext,
		IMqttActionListener callback) throws MqttException {

		//check to see if there is a network connection where we can send data before attempting the connect
		ConnectivityManager conManager = (ConnectivityManager) myContext.getSystemService(Context.CONNECTIVITY_SERVICE);

		NetworkInfo netInf = conManager.getActiveNetworkInfo();
		if ((netInf == null) || !netInf.isConnected()) {
			throw new MqttException(MqttException.REASON_CODE_BROKER_UNAVAILABLE);
		}

		IMqttToken token = new MqttTokenAndroidService(this, userContext,
			callback);

		connectOptions = options;
		connectToken = token;

		/*
		 * The actual connection depends on the service, which we start and bind
		 * to here, but which we can't actually use until the serviceConnection
		 * onServiceConnected() method has run (asynchronously), so the
		 * connection itself takes place in the onServiceConnected() method
		 */
		if (mqttService == null) { // First time - must bind to the service
			Intent serviceStartIntent = new Intent();
			serviceStartIntent.setClassName(myContext, SERVICE_NAME);
			Object service = myContext.startService(serviceStartIntent);
			if (service == null) {
				IMqttActionListener listener = token.getActionCallback();
				if (listener != null) {
					listener.onFailure(token, new RuntimeException(
						"cannot start service " + SERVICE_NAME));
				}
			}

			// We bind with BIND_SERVICE_FLAG (0), leaving us the manage the lifecycle
			// until the last time it is stopped by a call to stopService()
			myContext.bindService(serviceStartIntent, serviceConnection,
				BIND_SERVICE_FLAG);

			IntentFilter filter = new IntentFilter();
			filter.addAction(MqttServiceConstants.CALLBACK_TO_ACTIVITY);
			myContext.registerReceiver(this, filter);
		}
		else {
			pool.execute(new Runnable() {

				@Override
				public void run() {
					doConnect();

				}

			});
		}

		return token;
	}

	/**
	 * Actually do the mqtt connect operation
	 */
	private void doConnect() {
		if (clientHandle == null) {
			clientHandle = mqttService.getClient(serverURI, clientId,
				persistence);
		}
		mqttService.setTraceEnabled(traceEnabled);
		mqttService.setTraceCallbackId(clientHandle);

		String activityToken = storeToken(connectToken);
		try {
			mqttService.connect(clientHandle, connectOptions, null,
				activityToken);
		}
		catch (MqttException e) {
			IMqttActionListener listener = connectToken.getActionCallback();
			if (listener != null) {
				listener.onFailure(connectToken, e);
			}
		}
	}

	/**
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#disconnect()
	 */
	@Override
	public IMqttToken disconnect() throws MqttException {
		IMqttToken token = new MqttTokenAndroidService(this, null,
			(IMqttActionListener) null);
		String activityToken = storeToken(token);
		mqttService.disconnect(clientHandle, null, activityToken);
		return token;
	}

	/**
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#disconnect(long)
	 */
	@Override
	public IMqttToken disconnect(long quiesceTimeout) throws MqttException {
		IMqttToken token = new MqttTokenAndroidService(this, null,
			(IMqttActionListener) null);
		String activityToken = storeToken(token);
		mqttService.disconnect(clientHandle, quiesceTimeout, null,
			activityToken);
		return token;
	}

	/**
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#disconnect(Object,
	 *      IMqttActionListener)
	 */
	@Override
	public IMqttToken disconnect(Object userContext,
		IMqttActionListener callback) throws MqttException {
		IMqttToken token = new MqttTokenAndroidService(this, userContext,
			callback);
		String activityToken = storeToken(token);
		mqttService.disconnect(clientHandle, null, activityToken);
		return token;
	}

	/**
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#disconnect(long,
	 *      Object, IMqttActionListener)
	 */
	@Override
	public IMqttToken disconnect(long quiesceTimeout, Object userContext,
		IMqttActionListener callback) throws MqttException {
		IMqttToken token = new MqttTokenAndroidService(this, userContext,
			callback);
		String activityToken = storeToken(token);
		mqttService.disconnect(clientHandle, quiesceTimeout, null,
			activityToken);
		return token;
	}

	/**
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#publish(String,
	 *      byte[], int, boolean)
	 */
	@Override
	public IMqttDeliveryToken publish(String topic, byte[] payload, int qos,
		boolean retained) throws MqttException, MqttPersistenceException {
		return publish(topic, payload, qos, retained, null, null);
	}

	/**
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#publish(String,
	 *      MqttMessage)
	 */
	@Override
	public IMqttDeliveryToken publish(String topic, MqttMessage message)
	throws MqttException, MqttPersistenceException {
		return publish(topic, message, null, null);
	}

	/**
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#publish(String,
	 *      byte[], int, boolean, Object, IMqttActionListener)
	 */
	@Override
	public IMqttDeliveryToken publish(String topic, byte[] payload, int qos,
		boolean retained, Object userContext, IMqttActionListener callback)
	throws MqttException, MqttPersistenceException {

		MqttMessage message = new MqttMessage(payload);
		message.setQos(qos);
		message.setRetained(retained);
		MqttDeliveryTokenAndroidService token = new MqttDeliveryTokenAndroidService(
			this, userContext, callback, message);
		String activityToken = storeToken(token);
		IMqttDeliveryToken internalToken = mqttService.publish(clientHandle,
			topic, payload, qos, retained, null, activityToken);
		token.setDelegate(internalToken);
		return token;
	}

	/**
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#publish(String,
	 *      MqttMessage, Object, IMqttActionListener)
	 */
	@Override
	public IMqttDeliveryToken publish(String topic, MqttMessage message,
		Object userContext, IMqttActionListener callback)
	throws MqttException, MqttPersistenceException {
		MqttDeliveryTokenAndroidService token = new MqttDeliveryTokenAndroidService(
			this, userContext, callback, message);
		String activityToken = storeToken(token);
		IMqttDeliveryToken internalToken = mqttService.publish(clientHandle,
			topic, message, null, activityToken);
		token.setDelegate(internalToken);
		return token;
	}

	/**
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#subscribe(String,
	 *      int)
	 */
	@Override
	public IMqttToken subscribe(String topic, int qos) throws MqttException,
	MqttSecurityException {
		return subscribe(topic, qos, null, null);
	}

	/**
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#subscribe(String[],
	 *      int[])
	 */
	@Override
	public IMqttToken subscribe(String[] topic, int[] qos)
	throws MqttException, MqttSecurityException {
		return subscribe(topic, qos, null, null);
	}

	/**
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#subscribe(String,
	 *      int, Object, IMqttActionListener)
	 */
	@Override
	public IMqttToken subscribe(String topic, int qos, Object userContext,
		IMqttActionListener callback) throws MqttException {
		IMqttToken token = new MqttTokenAndroidService(this, userContext,
			callback, new String[]{topic});
		String activityToken = storeToken(token);
		mqttService.subscribe(clientHandle, topic, qos, null, activityToken);
		return token;
	}

	/**
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#subscribe(String[],
	 *      int[], Object, IMqttActionListener)
	 */
	@Override
	public IMqttToken subscribe(String[] topic, int[] qos, Object userContext,
		IMqttActionListener callback) throws MqttException {
		IMqttToken token = new MqttTokenAndroidService(this, userContext,
			callback, topic);
		String activityToken = storeToken(token);
		mqttService.subscribe(clientHandle, topic, qos, null, activityToken);
		return token;
	}

	/**
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#unsubscribe(String)
	 */
	@Override
	public IMqttToken unsubscribe(String topic) throws MqttException {
		return unsubscribe(topic, null, null);
	}

	/**
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#unsubscribe(String[])
	 */
	@Override
	public IMqttToken unsubscribe(String[] topic) throws MqttException {
		return unsubscribe(topic, null, null);
	}

	/**
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#unsubscribe(String,
	 *      Object, IMqttActionListener)
	 */
	@Override
	public IMqttToken unsubscribe(String topic, Object userContext,
		IMqttActionListener callback) throws MqttException {
		IMqttToken token = new MqttTokenAndroidService(this, userContext,
			callback);
		String activityToken = storeToken(token);
		mqttService.unsubscribe(clientHandle, topic, null, activityToken);
		return token;
	}

	/**
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#unsubscribe(String[],
	 *      Object, IMqttActionListener)
	 */
	@Override
	public IMqttToken unsubscribe(String[] topic, Object userContext,
		IMqttActionListener callback) throws MqttException {
		IMqttToken token = new MqttTokenAndroidService(this, userContext,
			callback);
		String activityToken = storeToken(token);
		mqttService.unsubscribe(clientHandle, topic, null, activityToken);
		return token;
	}

	/**
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#getPendingDeliveryTokens
	 *      ()
	 */
	@Override
	public IMqttDeliveryToken[] getPendingDeliveryTokens() {
		return mqttService.getPendingDeliveryTokens(clientHandle);
	}

	/**
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#setCallback(MqttCallback)
	 */
	@Override
	public void setCallback(MqttCallback callback) {
		this.callback = callback;

	}

	/**
	 * identify the callback to be invoked when making tracing calls back into
	 * the Activity
	 * 
	 * @param traceCallback handler
	 */
	public void setTraceCallback(MqttTraceHandler traceCallback) {
		this.traceCallback = traceCallback;
	 // mqttService.setTraceCallbackId(traceCallbackId);
	}

	/**
	 * turn tracing on and off
	 * 
	 * @param traceEnabled
	 */
	public void setTraceEnabled(boolean traceEnabled) {
		this.traceEnabled = traceEnabled;
		if (mqttService !=null)
			mqttService.setTraceEnabled(traceEnabled);
	}

	/**
	 * <p>
	 * Process incoming Intent objects representing the results of operations
	 * and asynchronous activities such as message received
	 * </p>
	 * <p>
	 * <strong>Note:</strong> This is only a public method because the Android
	 * APIs require such.<br>
	 * This method should not be explicitly invoked.
	 * </p>
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		Bundle data = intent.getExtras();

		String handleFromIntent = data
		.getString(MqttServiceConstants.CALLBACK_CLIENT_HANDLE);

		if ((handleFromIntent == null) || (!handleFromIntent.equals(clientHandle))) {
			return;
		}

		String action = data.getString(MqttServiceConstants.CALLBACK_ACTION);

		if (action.equals(MqttServiceConstants.CONNECT_ACTION)) {
			connectAction(data);
		}
		else if (action.equals(MqttServiceConstants.MESSAGE_ARRIVED_ACTION)) {
			messageArrivedAction(data);
		}
		else if (action.equals(MqttServiceConstants.SUBSCRIBE_ACTION)) {
			subscribeAction(data);
		}
		else if (action.equals(MqttServiceConstants.UNSUBSCRIBE_ACTION)) {
			unSubscribeAction(data);
		}
		else if (action.equals(MqttServiceConstants.SEND_ACTION)) {
			sendAction(data);
		}
		else if (action.equals(MqttServiceConstants.MESSAGE_DELIVERED_ACTION)) {
			messageDeliveredAction(data);
		}
		else if (action.equals(MqttServiceConstants.ON_CONNECTION_LOST_ACTION)) {
			connectionLostAction(data);
		}
		else if (action.equals(MqttServiceConstants.DISCONNECT_ACTION)) {
			disconnected(data);
		}
		else if (action.equals(MqttServiceConstants.TRACE_ACTION)) {
			traceAction(data);
		}
	}

	/**
	 * Acknowledges a message received on the {@link MqttCallback#messageArrived(String, MqttMessage)} 
	 * @param messageId the messageId received from the MqttMessage (To access this field you need to cast {@link MqttMessage} to {@link ParcelableMqttMessage}) 
	 * @return whether or not the message was successfully acknowledged
	 */
	public boolean acknowledgeMessage(String messageId) {
		if (messageAck == Ack.MANUAL_ACK) {
			Status status = mqttService.acknowledgeMessageArrival(clientHandle, messageId);
			return status == Status.OK;
		}
		return false;

	}

	/**
	 * Process the results of a connection
	 * 
	 * @param data
	 */
	private void connectAction(Bundle data) {
		IMqttToken token = removeMqttToken(data);
		simpleAction(token, data);
	}

	/**
	 * Process a notification that we have disconnected
	 * 
	 * @param data
	 */
	private void disconnected(Bundle data) {
		clientHandle = null; // avoid reuse!
		IMqttToken token = removeMqttToken(data);
		if (token != null) {
			((MqttTokenAndroidService) token).notifyComplete();
		}
		if (callback != null) {
			callback.connectionLost(null);
		}
	}

	/**
	 * Process a Connection Lost notification
	 * 
	 * @param data
	 */
	private void connectionLostAction(Bundle data) {
		if (callback != null) {
			Exception reason = (Exception) data
			.getSerializable(MqttServiceConstants.CALLBACK_EXCEPTION);
			callback.connectionLost(reason);
		}
	}

	/**
	 * Common processing for many notifications
	 * 
	 * @param token
	 *            the token associated with the action being undertake
	 * @param data
	 *            the result data
	 */
	private void simpleAction(IMqttToken token, Bundle data) {
		if (token != null) {
			Status status = (Status) data
			.getSerializable(MqttServiceConstants.CALLBACK_STATUS);
			if (status == Status.OK) {
				((MqttTokenAndroidService) token).notifyComplete();
			}
			else {

				Exception exceptionThrown = (Exception) data.getSerializable(MqttServiceConstants.CALLBACK_EXCEPTION);
				((MqttTokenAndroidService) token)
				.notifyFailure(exceptionThrown);
			}
		}
	}

	/**
	 * Process notification of a publish(send) operation
	 * 
	 * @param data
	 */
	private void sendAction(Bundle data) {
		IMqttToken token = getMqttToken(data); // get, don't remove - will
		// remove on delivery
		simpleAction(token, data);
	}

	/**
	 * Process notification of a subscribe operation
	 * 
	 * @param data
	 */
	private void subscribeAction(Bundle data) {
		IMqttToken token = removeMqttToken(data);
		simpleAction(token, data);
	}

	/**
	 * Process notification of an unsubscribe operation
	 * 
	 * @param data
	 */
	private void unSubscribeAction(Bundle data) {
		IMqttToken token = removeMqttToken(data);
		simpleAction(token, data);
	}

	/**
	 * Process notification of a published message having been delivered
	 * 
	 * @param data
	 */
	private void messageDeliveredAction(Bundle data) {
		IMqttToken token = removeMqttToken(data);
		if (token != null) {
			if (callback != null) {
				Status status = (Status) data
				.getSerializable(MqttServiceConstants.CALLBACK_STATUS);
				if (status == Status.OK) {
					callback.deliveryComplete((IMqttDeliveryToken) token);
				}
			}
		}
	}

	/**
	 * Process notification of a message's arrival
	 * 
	 * @param data
	 */
	private void messageArrivedAction(Bundle data) {
		if (callback != null) {
			String messageId = data
			.getString(MqttServiceConstants.CALLBACK_MESSAGE_ID);
			String destinationName = data
			.getString(MqttServiceConstants.CALLBACK_DESTINATION_NAME);

			ParcelableMqttMessage message = (ParcelableMqttMessage) data
			.getParcelable(MqttServiceConstants.CALLBACK_MESSAGE_PARCEL);
			try {
				if (messageAck == Ack.AUTO_ACK) {
					callback.messageArrived(destinationName, message);
					mqttService.acknowledgeMessageArrival(clientHandle, messageId);
				}
				else {
					message.messageId = messageId;
					callback.messageArrived(destinationName, message);
				}

				// let the service discard the saved message details
			}
			catch (Exception e) {
				// Swallow the exception
			}
		}
	}
	
	/**
	 * Process trace action - pass trace data back to the callback
	 * 
	 * @param data
	 */
	private void traceAction(Bundle data) {

		if (traceCallback != null) {
			String severity = data.getString(MqttServiceConstants.CALLBACK_TRACE_SEVERITY);
			String message =  data.getString(MqttServiceConstants.CALLBACK_ERROR_MESSAGE);
			String tag = data.getString(MqttServiceConstants.CALLBACK_TRACE_TAG);
			if (severity == MqttServiceConstants.TRACE_DEBUG) 
				traceCallback.traceDebug(tag, message);
			else if (severity == MqttServiceConstants.TRACE_ERROR) 
				traceCallback.traceError(tag, message);
			else
			{
				Exception e = (Exception) data.getSerializable(MqttServiceConstants.CALLBACK_EXCEPTION);
				traceCallback.traceException(tag, message, e);
			}
		}
	}

	/**
	 * @param token
	 *            identifying an operation
	 * @return an identifier for the token which can be passed to the Android
	 *         Service
	 */
	private synchronized String storeToken(IMqttToken token) {
		tokenMap.put(tokenNumber, token);
		return Integer.toString(tokenNumber++);
	}

	/**
	 * Get a token identified by a string, and remove it from our map
	 * 
	 * @param data
	 * @return the token
	 */
	private synchronized IMqttToken removeMqttToken(Bundle data) {
		String activityToken = data
		.getString(MqttServiceConstants.CALLBACK_ACTIVITY_TOKEN);
		int tokenNumber = Integer.parseInt(activityToken);
		IMqttToken token = tokenMap.get(tokenNumber);
		tokenMap.delete(tokenNumber);
		return token;
	}

	/**
	 * Get a token identified by a string, and remove it from our map
	 * 
	 * @param data
	 * @return the token
	 */
	private synchronized IMqttToken getMqttToken(Bundle data) {
		String activityToken = data
		.getString(MqttServiceConstants.CALLBACK_ACTIVITY_TOKEN);
		IMqttToken token = tokenMap.get(Integer.parseInt(activityToken));
		return token;
	}
}
