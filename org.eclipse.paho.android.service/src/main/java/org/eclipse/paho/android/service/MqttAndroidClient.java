/*******************************************************************************
 * Copyright (c) 1999, 2016 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 *   
 *   Ian Craggs - Per subscription message handlers bug 466579
 *   Ian Craggs - ack control (bug 472172)
 *   
 */
package org.eclipse.paho.android.service;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.MqttToken;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.SparseArray;

/**
 * Enables an android application to communicate with an MQTT server using non-blocking methods.
 * <p>
 * Implementation of the MQTT asynchronous client interface {@link IMqttAsyncClient} , using the MQTT
 * android service to actually interface with MQTT server. It provides android applications a simple programming interface to all features of the MQTT version 3.1
 * specification including:
 * </p>
 * <ul>
 * <li>connect
 * <li>publish
 * <li>subscribe
 * <li>unsubscribe
 * <li>disconnect
 * </ul>
 */
public class MqttAndroidClient extends BroadcastReceiver implements
		IMqttAsyncClient {

	/**
	 * 
	 * The Acknowledgment mode for messages received from {@link MqttCallback#messageArrived(String, MqttMessage)}
	 *
	 */
	public enum Ack {
		/**
		 * As soon as the {@link MqttCallback#messageArrived(String, MqttMessage)} returns,
		 * the message has been acknowledged as received .
		 */
		AUTO_ACK,
		/**
		 * When {@link MqttCallback#messageArrived(String, MqttMessage)} returns, the message
		 * will not be acknowledged as received, the application will have to make an acknowledgment call
		 * to {@link MqttAndroidClient} using {@link MqttAndroidClient#acknowledgeMessage(String)}
		 */
		MANUAL_ACK
	}

	private static final String SERVICE_NAME = "org.eclipse.paho.android.service.MqttService";

	private static final int BIND_SERVICE_FLAG = 0;

	private static final ExecutorService pool = Executors.newCachedThreadPool();

	/**
	 * ServiceConnection to process when we bind to our service
	 */
	private final class MyServiceConnection implements ServiceConnection {

		@Override
		public void onServiceConnected(ComponentName name, IBinder binder) {
			mqttService = ((MqttServiceBinder) binder).getService();
			bindedService = true;
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
	private final MyServiceConnection serviceConnection = new MyServiceConnection();

	// The Android Service which will process our mqtt calls
	private MqttService mqttService;

	// An identifier for the underlying client connection, which we can pass to
	// the service
	private String clientHandle;

	private Context myContext;

	// We hold the various tokens in a collection and pass identifiers for them
	// to the service
	private final SparseArray<IMqttToken> tokenMap = new SparseArray<>();
	private int tokenNumber = 0;

	// Connection data
	private final String serverURI;
	private final String clientId;
	private MqttClientPersistence persistence = null;
	private MqttConnectOptions connectOptions;
	private IMqttToken connectToken;

	// The MqttCallback provided by the application
	private MqttCallback callback;
	private MqttTraceHandler traceCallback;

	//The acknowledgment that a message has been processed by the application
	private final Ack messageAck;
	private boolean traceEnabled = false;
	
	private volatile boolean receiverRegistered = false;
	private volatile boolean bindedService = false;

	/**
	 * Constructor - create an MqttAndroidClient that can be used to communicate with an MQTT server on android
	 * 
	 * @param context 
	 *            object used to pass context to the callback. 
	 * @param serverURI
	 *            specifies the protocol, host name and port to be used to
	 *            connect to an MQTT server
	 * @param clientId
	 *            specifies the name by which this connection should be
	 *            identified to the server
	 */
	public MqttAndroidClient(Context context, String serverURI,
			String clientId) {
		this(context, serverURI, clientId, null, Ack.AUTO_ACK);
	}

	/**
	 * Constructor - create an MqttAndroidClient that can be used to communicate
	 * with an MQTT server on android
	 * 
	 * @param ctx
	 *            Application's context
	 * @param serverURI
	 *            specifies the protocol, host name and port to be used to
	 *            connect to an MQTT server
	 * @param clientId
	 *            specifies the name by which this connection should be
	 *            identified to the server
	 * @param ackType
	 *            how the application wishes to acknowledge a message has been
	 *            processed
	 */
	public MqttAndroidClient(Context ctx, String serverURI, String clientId, Ack ackType) {
		this(ctx, serverURI, clientId, null, ackType);
	}

	/**
	 * Constructor - create an MqttAndroidClient that can be used to communicate
	 * with an MQTT server on android
	 * 
	 * @param ctx
	 *            Application's context
	 * @param serverURI
	 *            specifies the protocol, host name and port to be used to
	 *            connect to an MQTT server
	 * @param clientId
	 *            specifies the name by which this connection should be
	 *            identified to the server
	 * @param persistence
	 *            The object to use to store persisted data
	 */
	public MqttAndroidClient(Context ctx, String serverURI, String clientId, MqttClientPersistence persistence) {
		this(ctx, serverURI, clientId, persistence, Ack.AUTO_ACK);
	}

	/**
	 * Constructor- create an MqttAndroidClient that can be used to communicate
	 * with an MQTT server on android
	 * 
	 * @param context
	 *            used to pass context to the callback.
	 * @param serverURI
	 *            specifies the protocol, host name and port to be used to
	 *            connect to an MQTT server
	 * @param clientId
	 *            specifies the name by which this connection should be
	 *            identified to the server
	 * @param persistence
	 *            the persistence class to use to store in-flight message. If
	 *            null then the default persistence mechanism is used
	 * @param ackType
	 *            how the application wishes to acknowledge a message has been
	 *            processed.
	 */
	public MqttAndroidClient(Context context, String serverURI,
			String clientId, MqttClientPersistence persistence, Ack ackType) {
		myContext = context;
		this.serverURI = serverURI;
		this.clientId = clientId;
		this.persistence = persistence;
		messageAck = ackType;
	}

	 /**
	 * Determines if this client is currently connected to the server.
	 *
	 * @return <code>true</code> if connected, <code>false</code> otherwise.
	 */
	@Override
	public boolean isConnected() {

		return clientHandle != null && mqttService != null && mqttService.isConnected(clientHandle);
	}

	/**
	 * Returns the client ID used by this client.
	 * <p>
	 * All clients connected to the same server or server farm must have a
	 * unique ID.
	 * </p>
	 * 
	 * @return the client ID used by this client.
	 */
	@Override
	public String getClientId() {
		return clientId;
	}

	/**
	 * Returns the URI address of the server used by this client.
	 * <p>
	 * The format of the returned String is the same as that used on the
	 * constructor.
	 * </p>
	 * 
	 * @return the server's address, as a URI String.
	 */
	@Override
	public String getServerURI() {
		return serverURI;
	}

	/**
	 * Close the client. Releases all resource associated with the client. After
	 * the client has been closed it cannot be reused. For instance attempts to
	 * connect will fail.
	 */
	@Override
	public void close() {
		if(mqttService != null){
			if (clientHandle == null) {
				clientHandle = mqttService.getClient(serverURI, clientId, myContext.getApplicationInfo().packageName,persistence);
			}
			mqttService.close(clientHandle);
		}
	}
	
	/**
	 * Connects to an MQTT server using the default options.
	 * <p>
	 * The default options are specified in {@link MqttConnectOptions} class.
	 * </p>
	 * 
	 * @return token used to track and wait for the connect to complete. The
	 *         token will be passed to the callback methods if a callback is
	 *         set.
	 * @see #connect(MqttConnectOptions, Object, IMqttActionListener)
	 */
	@Override
	public IMqttToken connect() {
		return connect(null, null);
	}

	
	/**
	 * Connects to an MQTT server using the provided connect options.
	 * <p>
	 * The connection will be established using the options specified in the
	 * {@link MqttConnectOptions} parameter.
	 * </p>
	 * 
	 * @param options
	 *            a set of connection parameters that override the defaults.
	 * @return token used to track and wait for the connect to complete. The
	 *         token will be passed to any callback that has been set.
	 * @see #connect(MqttConnectOptions, Object, IMqttActionListener)
	 */
	@Override
	public IMqttToken connect(MqttConnectOptions options) {
		return connect(options, null, null);
	}

	/**
	 * Connects to an MQTT server using the default options.
	 * <p>
	 * The default options are specified in {@link MqttConnectOptions} class.
	 * </p>
	 * 
	 * @param userContext
	 *            optional object used to pass context to the callback. Use null
	 *            if not required.
	 * @param callback
	 *            optional listener that will be notified when the connect
	 *            completes. Use null if not required.
	 * @return token used to track and wait for the connect to complete. The
	 *         token will be passed to any callback that has been set.
	 * @see #connect(MqttConnectOptions, Object, IMqttActionListener)
	 */
	@Override
	public IMqttToken connect(Object userContext, IMqttActionListener callback) {
		return connect(new MqttConnectOptions(), userContext, callback);
	}

	/**
	 * Connects to an MQTT server using the specified options.
	 * <p>
	 * The server to connect to is specified on the constructor. It is
	 * recommended to call {@link #setCallback(MqttCallback)} prior to
	 * connecting in order that messages destined for the client can be accepted
	 * as soon as the client is connected.
	 * </p>
	 *
	 * <p>
	 * The method returns control before the connect completes. Completion can
	 * be tracked by:
     * </p>
	 * <ul>
	 * <li>Waiting on the returned token {@link IMqttToken#waitForCompletion()}
	 * or</li>
	 * <li>Passing in a callback {@link IMqttActionListener}</li>
	 * </ul>
	 *
	 * 
	 * @param options
	 *            a set of connection parameters that override the defaults.
	 * @param userContext
	 *            optional object for used to pass context to the callback. Use
	 *            null if not required.
	 * @param callback
	 *            optional listener that will be notified when the connect
	 *            completes. Use null if not required.
	 * @return token used to track and wait for the connect to complete. The
	 *         token will be passed to any callback that has been set.
	 */

	@Override
	public IMqttToken connect(MqttConnectOptions options, Object userContext,
			IMqttActionListener callback) {

		IMqttToken token = new MqttTokenAndroid(this, userContext,
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
					Context.BIND_AUTO_CREATE);

			if (!receiverRegistered) registerReceiver(this);
		}
		else {
			pool.execute(new Runnable() {

				@Override
				public void run() {
					doConnect();
					
					//Register receiver to show shoulder tap.
					if (!receiverRegistered) registerReceiver(MqttAndroidClient.this);
				}

			});
		}

		return token;
	}

	private void registerReceiver(BroadcastReceiver receiver) {
		IntentFilter filter = new IntentFilter();
				filter.addAction(MqttServiceConstants.CALLBACK_TO_ACTIVITY);
				LocalBroadcastManager.getInstance(myContext).registerReceiver(receiver, filter);
				receiverRegistered = true;
	}

	/**
	 * Actually do the mqtt connect operation
	 */
	private void doConnect() {
		if (clientHandle == null) {
			clientHandle = mqttService.getClient(serverURI, clientId,myContext.getApplicationInfo().packageName,
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
	 * Disconnects from the server.
	 * <p>
	 * An attempt is made to quiesce the client allowing outstanding work to
	 * complete before disconnecting. It will wait for a maximum of 30 seconds
	 * for work to quiesce before disconnecting. This method must not be called
	 * from inside {@link MqttCallback} methods.
	 * </p>
	 * 
	 * @return token used to track and wait for disconnect to complete. The
	 *         token will be passed to any callback that has been set.
	 * @see #disconnect(long, Object, IMqttActionListener)
	 */
	@Override
	public IMqttToken disconnect() {
		IMqttToken token = new MqttTokenAndroid(this, null,
				null);
		String activityToken = storeToken(token);
		mqttService.disconnect(clientHandle, null, activityToken);
		return token;
	}

	/**
	 * Disconnects from the server.
	 * <p>
	 * An attempt is made to quiesce the client allowing outstanding work to
	 * complete before disconnecting. It will wait for a maximum of the
	 * specified quiesce time for work to complete before disconnecting. This
	 * method must not be called from inside {@link MqttCallback} methods.
	 * </p>
	 * 
	 * @param quiesceTimeout
	 *            the amount of time in milliseconds to allow for existing work
	 *            to finish before disconnecting. A value of zero or less means
	 *            the client will not quiesce.
	 * @return token used to track and wait for disconnect to complete. The
	 *         token will be passed to the callback methods if a callback is
	 *         set.
	 * @see #disconnect(long, Object, IMqttActionListener)
	 */
	@Override
	public IMqttToken disconnect(long quiesceTimeout) {
		IMqttToken token = new MqttTokenAndroid(this, null,
				null);
		String activityToken = storeToken(token);
		mqttService.disconnect(clientHandle, quiesceTimeout, null,
				activityToken);
		return token;
	}

	/**
	 * Disconnects from the server.
	 * <p>
	 * An attempt is made to quiesce the client allowing outstanding work to
	 * complete before disconnecting. It will wait for a maximum of 30 seconds
	 * for work to quiesce before disconnecting. This method must not be called
	 * from inside {@link MqttCallback} methods.
	 * </p>
	 * 
	 * @param userContext
	 *            optional object used to pass context to the callback. Use null
	 *            if not required.
	 * @param callback
	 *            optional listener that will be notified when the disconnect
	 *            completes. Use null if not required.
	 * @return token used to track and wait for the disconnect to complete. The
	 *         token will be passed to any callback that has been set.
	 * @see #disconnect(long, Object, IMqttActionListener)
	 */
	@Override
	public IMqttToken disconnect(Object userContext,
			IMqttActionListener callback) {
		IMqttToken token = new MqttTokenAndroid(this, userContext,
				callback);
		String activityToken = storeToken(token);
		mqttService.disconnect(clientHandle, null, activityToken);
		return token;
	}

	/**
	 * Disconnects from the server.
	 * <p>
	 * The client will wait for {@link MqttCallback} methods to complete. It
	 * will then wait for up to the quiesce timeout to allow for work which has
	 * already been initiated to complete. For instance when a QoS 2 message has
	 * started flowing to the server but the QoS 2 flow has not completed.It
	 * prevents new messages being accepted and does not send any messages that
	 * have been accepted but not yet started delivery across the network to the
	 * server. When work has completed or after the quiesce timeout, the client
	 * will disconnect from the server. If the cleanSession flag was set to
	 * false and next time it is also set to false in the connection, the
	 * messages made in QoS 1 or 2 which were not previously delivered will be
	 * delivered this time.
	 * </p>
	 * <p>
	 * This method must not be called from inside {@link MqttCallback} methods.
	 * </p>
	 * <p>
	 * The method returns control before the disconnect completes. Completion
	 * can be tracked by:
     * </p>
	 * <ul>
	 * <li>Waiting on the returned token {@link IMqttToken#waitForCompletion()}
	 * or</li>
	 * <li>Passing in a callback {@link IMqttActionListener}</li>
	 * </ul>
	 * 
	 * @param quiesceTimeout
	 *            the amount of time in milliseconds to allow for existing work
	 *            to finish before disconnecting. A value of zero or less means
	 *            the client will not quiesce.
	 * @param userContext
	 *            optional object used to pass context to the callback. Use null
	 *            if not required.
	 * @param callback
	 *            optional listener that will be notified when the disconnect
	 *            completes. Use null if not required.
	 * @return token used to track and wait for the disconnect to complete. The
	 *         token will be passed to any callback that has been set.
	 */
	@Override
	public IMqttToken disconnect(long quiesceTimeout, Object userContext,
			IMqttActionListener callback) {
		IMqttToken token = new MqttTokenAndroid(this, userContext,
				callback);
		String activityToken = storeToken(token);
		mqttService.disconnect(clientHandle, quiesceTimeout, null,
				activityToken);
		return token;
	}

	/**
	 * Publishes a message to a topic on the server.
	 * <p>
	 * A convenience method, which will create a new {@link MqttMessage} object
	 * with a byte array payload and the specified QoS, and then publish it.
	 * </p>
	 * 
	 * @param topic
	 *            to deliver the message to, for example "finance/stock/ibm".
	 * @param payload
	 *            the byte array to use as the payload
	 * @param qos
	 *            the Quality of Service to deliver the message at. Valid values
	 *            are 0, 1 or 2.
	 * @param retained
	 *            whether or not this message should be retained by the server.
	 * @return token used to track and wait for the publish to complete. The
	 *         token will be passed to any callback that has been set.
	 * @throws IllegalArgumentException
	 *             if value of QoS is not 0, 1 or 2.
	 * @see #publish(String, MqttMessage, Object, IMqttActionListener)
	 */
	@Override
	public IMqttDeliveryToken publish(String topic, byte[] payload, int qos,
			boolean retained) {
		return publish(topic, payload, qos, retained, null, null);
	}

	/**
	 * Publishes a message to a topic on the server. Takes an
	 * {@link MqttMessage} message and delivers it to the server at the
	 * requested quality of service.
	 * 
	 * @param topic
	 *            to deliver the message to, for example "finance/stock/ibm".
	 * @param message
	 *            to deliver to the server
	 * @return token used to track and wait for the publish to complete. The
	 *         token will be passed to any callback that has been set.
	 * @throws IllegalArgumentException
	 *             if value of QoS is not 0, 1 or 2.
	 * @see #publish(String, MqttMessage, Object, IMqttActionListener)
	 */
	@Override
	public IMqttDeliveryToken publish(String topic, MqttMessage message) {
		return publish(topic, message, null, null);
	}

	/**
	 * Publishes a message to a topic on the server.
	 * <p>
	 * A convenience method, which will create a new {@link MqttMessage} object
	 * with a byte array payload, the specified QoS and retained, then publish it.
	 * </p>
	 * 
	 * @param topic
	 *            to deliver the message to, for example "finance/stock/ibm".
	 * @param payload
	 *            the byte array to use as the payload
	 * @param qos
	 *            the Quality of Service to deliver the message at. Valid values
	 *            are 0, 1 or 2.
	 * @param retained
	 *            whether or not this message should be retained by the server.
	 * @param userContext
	 *            optional object used to pass context to the callback. Use null
	 *            if not required.
	 * @param callback
	 *            optional listener that will be notified when message delivery
	 *            has completed to the requested quality of service
	 * @return token used to track and wait for the publish to complete. The
	 *         token will be passed to any callback that has been set.
	 * @throws IllegalArgumentException
	 *             if value of QoS is not 0, 1 or 2.
	 * @see #publish(String, MqttMessage, Object, IMqttActionListener)
	 */
	@Override
	public IMqttDeliveryToken publish(String topic, byte[] payload, int qos,
			boolean retained, Object userContext, IMqttActionListener callback) {

		MqttMessage message = new MqttMessage(payload);
		message.setQos(qos);
		message.setRetained(retained);
		MqttDeliveryTokenAndroid token = new MqttDeliveryTokenAndroid(
				this, userContext, callback, message);
		String activityToken = storeToken(token);
		IMqttDeliveryToken internalToken = mqttService.publish(clientHandle,
				topic, payload, qos, retained, null, activityToken);
		token.setDelegate(internalToken);
		return token;
	}

	/**
	 * Publishes a message to a topic on the server.
	 * <p>
	 * Once this method has returned cleanly, the message has been accepted for
	 * publication by the client and will be delivered on a background thread.
	 * In the event the connection fails or the client stops, Messages will be
	 * delivered to the requested quality of service once the connection is
	 * re-established to the server on condition that:
     * </p>
	 * <ul>
	 * <li>The connection is re-established with the same clientID
	 * <li>The original connection was made with (@link
	 * MqttConnectOptions#setCleanSession(boolean)} set to false
	 * <li>The connection is re-established with (@link
	 * MqttConnectOptions#setCleanSession(boolean)} set to false
	 * <li>Depending when the failure occurs QoS 0 messages may not be
	 * delivered.
	 * </ul>
	 * 
	 * <p>
	 * When building an application, the design of the topic tree should take
	 * into account the following principles of topic name syntax and semantics:
	 * </p>
	 * 
	 * <ul>
	 * <li>A topic must be at least one character long.</li>
	 * <li>Topic names are case sensitive. For example, <em>ACCOUNTS</em> and
	 * <em>Accounts</em> are two different topics.</li>
	 * <li>Topic names can include the space character. For example,
	 * <em>Accounts
	 * 	payable</em> is a valid topic.</li>
	 * <li>A leading "/" creates a distinct topic. For example,
	 * <em>/finance</em> is different from <em>finance</em>. <em>/finance</em>
	 * matches "+/+" and "/+", but not "+".</li>
	 * <li>Do not include the null character (Unicode <em>\x0000</em>) in any topic.</li>
	 * </ul>
	 * 
	 * <p>
	 * The following principles apply to the construction and content of a topic
	 * tree:
	 * </p>
	 * 
	 * <ul>
	 * <li>The length is limited to 64k but within that there are no limits to
	 * the number of levels in a topic tree.</li>
	 * <li>There can be any number of root nodes; that is, there can be any
	 * number of topic trees.</li>
	 * </ul>
	 * <p>
	 * The method returns control before the publish completes. Completion can
	 * be tracked by:
     * </p>
	 * <ul>
	 * <li>Setting an {@link IMqttAsyncClient#setCallback(MqttCallback)} where
	 * the {@link MqttCallback#deliveryComplete(IMqttDeliveryToken)} method will
	 * be called.</li>
	 * <li>Waiting on the returned token {@link MqttToken#waitForCompletion()}
	 * or</li>
	 * <li>Passing in a callback {@link IMqttActionListener} to this method</li>
	 * </ul>
	 * 
	 * @param topic
	 *            to deliver the message to, for example "finance/stock/ibm".
	 * @param message
	 *            to deliver to the server
	 * @param userContext
	 *            optional object used to pass context to the callback. Use null
	 *            if not required.
	 * @param callback
	 *            optional listener that will be notified when message delivery
	 *            has completed to the requested quality of service
	 * @return token used to track and wait for the publish to complete. The
	 *         token will be passed to callback methods if set.
	 * @throws IllegalArgumentException
	 *             if value of QoS is not 0, 1 or 2.
	 * @see MqttMessage
	 */
	@Override
	public IMqttDeliveryToken publish(String topic, MqttMessage message,
			Object userContext, IMqttActionListener callback) {
		MqttDeliveryTokenAndroid token = new MqttDeliveryTokenAndroid(
				this, userContext, callback, message);
		String activityToken = storeToken(token);
		IMqttDeliveryToken internalToken = mqttService.publish(clientHandle,
				topic, message, null, activityToken);
		token.setDelegate(internalToken);
		return token;
	}

	/**
	 * Subscribe to a topic, which may include wildcards.
	 * 
	 * @param topic
	 *            the topic to subscribe to, which can include wildcards.
	 * @param qos
	 *            the maximum quality of service at which to subscribe. Messages
	 *            published at a lower quality of service will be received at
	 *            the published QoS. Messages published at a higher quality of
	 *            service will be received using the QoS specified on the
	 *            subscription.
	 * @return token used to track and wait for the subscribe to complete. The
	 *         token will be passed to callback methods if set.
	 *
	 * @see #subscribe(String[], int[], Object, IMqttActionListener)
	 */
	@Override
	public IMqttToken subscribe(String topic, int qos) {
		return subscribe(topic, qos, null, null);
	}

	/**
	 * Subscribe to multiple topics, each topic may include wildcards.
	 * 
	 * <p>
	 * Provides an optimized way to subscribe to multiple topics compared to
	 * subscribing to each one individually.
	 * </p>
	 * 
	 * @param topic
	 *            one or more topics to subscribe to, which can include
	 *            wildcards
	 * @param qos
	 *            the maximum quality of service at which to subscribe. Messages
	 *            published at a lower quality of service will be received at
	 *            the published QoS. Messages published at a higher quality of
	 *            service will be received using the QoS specified on the
	 *            subscription.
	 * @return token used to track and wait for the subscription to complete. The
	 *         token will be passed to callback methods if set.
	 *
	 * @see #subscribe(String[], int[], Object, IMqttActionListener)
	 */
	@Override
	public IMqttToken subscribe(String[] topic, int[] qos) {
		return subscribe(topic, qos, null, null);
	}

	/**
	 * Subscribe to a topic, which may include wildcards.
	 * 
	 * @param topic
	 *            the topic to subscribe to, which can include wildcards.
	 * @param qos
	 *            the maximum quality of service at which to subscribe. Messages
	 *            published at a lower quality of service will be received at
	 *            the published QoS. Messages published at a higher quality of
	 *            service will be received using the QoS specified on the
	 *            subscription.
	 * @param userContext
	 *            optional object used to pass context to the callback. Use null
	 *            if not required.
	 * @param callback
	 *            optional listener that will be notified when subscribe has
	 *            completed
	 * @return token used to track and wait for the subscribe to complete. The
	 *         token will be passed to callback methods if set.
	 *
	 * @see #subscribe(String[], int[], Object, IMqttActionListener)
	 */
	@Override
	public IMqttToken subscribe(String topic, int qos, Object userContext,
			IMqttActionListener callback) {
		IMqttToken token = new MqttTokenAndroid(this, userContext,
				callback, new String[]{topic});
		String activityToken = storeToken(token);
		mqttService.subscribe(clientHandle, topic, qos, null, activityToken);
		return token;
	}

	/**
	 * Subscribes to multiple topics, each topic may include wildcards.
	 * <p>
	 * Provides an optimized way to subscribe to multiple topics compared to
	 * subscribing to each one individually.
	 * </p>
	 * <p>
	 * The {@link #setCallback(MqttCallback)} method should be called before
	 * this method, otherwise any received messages will be discarded.
	 * </p>
	 * <p>
	 * If (@link MqttConnectOptions#setCleanSession(boolean)} was set to true,
	 * when connecting to the server, the subscription remains in place until
	 * either:
     * </p>
	 * <ul>
	 * <li>The client disconnects</li>
	 * <li>An unsubscribe method is called to unsubscribe the topic</li>
     * </ul>
	 * <p>
	 * If (@link MqttConnectOptions#setCleanSession(boolean)} was set to false,
	 * when connecting to the server, the subscription remains in place
	 * until either:
     * </p>
	 * <ul>
	 * <li>An unsubscribe method is called to unsubscribe the topic</li>
	 * <li>The next time the client connects with cleanSession set to true
	 * </ul>
	 * <p>With cleanSession set to false the MQTT server will store messages
	 * on behalf of the client when the client is not connected. The next time
	 * the client connects with the <b>same client ID</b> the server will
	 * deliver the stored messages to the client.
     * </p>
	 * 
	 * <p>
	 * The "topic filter" string is used when subscription may contain special
	 * characters, which allows you to subscribe to multiple topics at once.
	 * <dl>
	 * <dt>Topic level separator</dt>
	 * <dd>The forward slash (/) is used to separate each level within a topic
	 * tree and provide a hierarchical structure to the topic space. The use of
	 * the topic level separator is significant when the two wildcard characters
	 * are encountered in topics specified by subscribers.</dd>
	 * 
	 * <dt>Multi-level wildcard</dt>
	 * <dd>
	 * <p>
	 * The number sign (#) is a wildcard character that matches any number of
	 * levels within a topic. For example, if you subscribe to <span><span
	 * class="filepath">finance/stock/ibm/#</span></span>, you receive messages
	 * on these topics:
     * </p>
     * <ul>
     *     <li><pre>finance/stock/ibm</pre></li>
     *     <li><pre>finance/stock/ibm/closingprice</pre></li>
     *     <li><pre>finance/stock/ibm/currentprice</pre></li>
     * </ul>
	 *
	 * <p>
	 * The multi-level wildcard can represent zero or more levels. Therefore,
	 * <em>finance/#</em> can also match the singular <em>finance</em>, where
	 * <em>#</em> represents zero levels. The topic level separator is
	 * meaningless in this context, because there are no levels to separate.
	 * </p>
	 * 
	 * <p>
	 * The <span>multi-level</span> wildcard can be specified only on its own or
	 * next to the topic level separator character. Therefore, <em>#</em> and
	 * <em>finance/#</em> are both valid, but <em>finance#</em> is not valid.
	 * <span>The multi-level wildcard must be the last character used within the
	 * topic tree. For example, <em>finance/#</em> is valid but
	 * <em>finance/#/closingprice</em> is not valid.</span>
	 * </p>
	 * </dd>
	 * 
	 * <dt>Single-level wildcard</dt>
	 * <dd>
	 * <p>
	 * The plus sign (+) is a wildcard character that matches only one topic
	 * level. For example, <em>finance/stock/+</em> matches
	 * <em>finance/stock/ibm</em> and <em>finance/stock/xyz</em>, but not
	 * <em>finance/stock/ibm/closingprice</em>. Also, because the single-level
	 * wildcard matches only a single level, <em>finance/+</em> does not match
	 * <em>finance</em>.
	 * </p>
	 * 
	 * <p>
	 * Use the single-level wildcard at any level in the topic tree, and in
	 * conjunction with the multilevel wildcard. Specify the single-level
	 * wildcard next to the topic level separator, except when it is specified
	 * on its own. Therefore, <em>+</em> and <em>finance/+</em> are both valid,
	 * but <em>finance+</em> is not valid. <span>The single-level wildcard can
	 * be used at the end of the topic tree or within the topic tree. For
	 * example, <em>finance/+</em> and <em>finance/+/ibm</em> are both
	 * valid.</span>
	 * </p>
	 * </dd>
	 * </dl>
	 * <p>
	 * The method returns control before the subscribe completes. Completion can
	 * be tracked by:
     * </p>
	 * <ul>
	 * <li>Waiting on the supplied token {@link MqttToken#waitForCompletion()}
	 * or</li>
	 * <li>Passing in a callback {@link IMqttActionListener} to this method</li>
	 * </ul>
	 * 
	 * @param topic
	 *            one or more topics to subscribe to, which can include
	 *            wildcards
	 * @param qos
	 *            the maximum quality of service to subscribe each topic
	 *            at.Messages published at a lower quality of service will be
	 *            received at the published QoS. Messages published at a higher
	 *            quality of service will be received using the QoS specified on
	 *            the subscription.
	 * @param userContext
	 *            optional object used to pass context to the callback. Use null
	 *            if not required.
	 * @param callback
	 *            optional listener that will be notified when subscribe has
	 *            completed
	 * @return token used to track and wait for the subscribe to complete. The
	 *         token will be passed to callback methods if set.
	 * @throws IllegalArgumentException
	 *             if the two supplied arrays are not the same size.
	 */
	@Override
	public IMqttToken subscribe(String[] topic, int[] qos, Object userContext,
			IMqttActionListener callback) {
		IMqttToken token = new MqttTokenAndroid(this, userContext,
				callback, topic);
		String activityToken = storeToken(token);
		mqttService.subscribe(clientHandle, topic, qos, null, activityToken);
		return token;
	}
	
	/**
	 * Subscribe to a topic, which may include wildcards.
	 *
	 * @see #subscribe(String[], int[], Object, IMqttActionListener)
	 *
	 * @param topicFilter the topic to subscribe to, which can include wildcards.
	 * @param qos the maximum quality of service at which to subscribe. Messages
	 * published at a lower quality of service will be received at the published
	 * QoS.  Messages published at a higher quality of service will be received using
	 * the QoS specified on the subscribe.
	 * @param userContext optional object used to pass context to the callback. Use
	 * null if not required.
	 * @param callback optional listener that will be notified when subscribe
	 * has completed
	 * @param messageListener a callback to handle incoming messages
	 * @return token used to track and wait for the subscribe to complete. The token
	 * will be passed to callback methods if set.
	 */
	public IMqttToken subscribe(String topicFilter, int qos, Object userContext, IMqttActionListener callback, IMqttMessageListener messageListener) {

		return subscribe(new String[] {topicFilter}, new int[] {qos}, userContext, callback, new IMqttMessageListener[] {messageListener});
	}

	/**
	 * Subscribe to a topic, which may include wildcards.
	 *
	 * @see #subscribe(String[], int[], Object, IMqttActionListener)
	 *
	 * @param topicFilter the topic to subscribe to, which can include wildcards.
	 * @param qos the maximum quality of service at which to subscribe. Messages
	 * published at a lower quality of service will be received at the published
	 * QoS.  Messages published at a higher quality of service will be received using
	 * the QoS specified on the subscribe.
	 * @param messageListener a callback to handle incoming messages
	 * @return token used to track and wait for the subscribe to complete. The token
	 * will be passed to callback methods if set.
	 */
	public IMqttToken subscribe(String topicFilter, int qos, IMqttMessageListener messageListener) {
		
		return subscribe(topicFilter, qos, null, null, messageListener);
	}

	
	/**
	 * Subscribe to multiple topics, each of which may include wildcards.
	 *
	 * <p>Provides an optimized way to subscribe to multiple topics compared to
	 * subscribing to each one individually.</p>
	 *
	 * @see #subscribe(String[], int[], Object, IMqttActionListener)
	 *
	 * @param topicFilters one or more topics to subscribe to, which can include wildcards
	 * @param qos the maximum quality of service at which to subscribe. Messages
	 * published at a lower quality of service will be received at the published
	 * QoS.  Messages published at a higher quality of service will be received using
	 * the QoS specified on the subscribe.
	 * @param messageListeners an array of callbacks to handle incoming messages
	 * @return token used to track and wait for the subscribe to complete. The token
	 * will be passed to callback methods if set.
	 */
	public IMqttToken subscribe(String[] topicFilters, int[] qos, IMqttMessageListener[] messageListeners) {
		
		return subscribe(topicFilters, qos, null, null, messageListeners);
	}


	/**
	 * Subscribe to multiple topics, each of which may include wildcards.
	 *
	 * <p>Provides an optimized way to subscribe to multiple topics compared to
	 * subscribing to each one individually.</p>
	 *
	 * @see #subscribe(String[], int[], Object, IMqttActionListener)
	 *
	 * @param topicFilters one or more topics to subscribe to, which can include wildcards
	 * @param qos the maximum quality of service at which to subscribe. Messages
	 * published at a lower quality of service will be received at the published
	 * QoS.  Messages published at a higher quality of service will be received using
	 * the QoS specified on the subscribe.
	 * @param userContext optional object used to pass context to the callback. Use
	 * null if not required.
	 * @param callback optional listener that will be notified when subscribe
	 * has completed
	 * @param messageListeners an array of callbacks to handle incoming messages
	 * @return token used to track and wait for the subscribe to complete. The token
	 * will be passed to callback methods if set.
	 */
	public IMqttToken subscribe(String[] topicFilters, int[] qos, Object userContext, IMqttActionListener callback, IMqttMessageListener[] messageListeners) {
		IMqttToken token = new MqttTokenAndroid(this, userContext, callback, topicFilters);
		String activityToken = storeToken(token);
		mqttService.subscribe(clientHandle, topicFilters, qos, null, activityToken, messageListeners);

		return null;
	}


	/**
	 * Requests the server unsubscribe the client from a topic.
	 * 
	 * @param topic
	 *            the topic to unsubscribe from. It must match a topic specified
	 *            on an earlier subscribe.
	 * @return token used to track and wait for the unsubscribe to complete. The
	 *         token will be passed to callback methods if set.
	 *
	 * @see #unsubscribe(String[], Object, IMqttActionListener)
	 */
	@Override
	public IMqttToken unsubscribe(String topic) {
		return unsubscribe(topic, null, null);
	}

	/**
	 * Requests the server to unsubscribe the client from one or more topics.
	 * 
	 * @param topic
	 *            one or more topics to unsubscribe from. Each topic must match
	 *            one specified on an earlier subscription.
	 * @return token used to track and wait for the unsubscribe to complete. The
	 *         token will be passed to callback methods if set.
	 *
	 * @see #unsubscribe(String[], Object, IMqttActionListener)
	 */
	@Override
	public IMqttToken unsubscribe(String[] topic) {
		return unsubscribe(topic, null, null);
	}

	/**
	 * Requests the server to unsubscribe the client from a topics.
	 * 
	 * @param topic
	 *            the topic to unsubscribe from. It must match a topic specified
	 *            on an earlier subscribe.
	 * @param userContext
	 *            optional object used to pass context to the callback. Use null
	 *            if not required.
	 * @param callback
	 *            optional listener that will be notified when unsubscribe has
	 *            completed
	 * @return token used to track and wait for the unsubscribe to complete. The
	 *         token will be passed to callback methods if set.
	 *
	 * @see #unsubscribe(String[], Object, IMqttActionListener)
	 */
	@Override
	public IMqttToken unsubscribe(String topic, Object userContext,
			IMqttActionListener callback) {
		IMqttToken token = new MqttTokenAndroid(this, userContext,
				callback);
		String activityToken = storeToken(token);
		mqttService.unsubscribe(clientHandle, topic, null, activityToken);
		return token;
	}

	/**
	 * Requests the server to unsubscribe the client from one or more topics.
	 * <p>
	 * Unsubcribing is the opposite of subscribing. When the server receives the
	 * unsubscribe request it looks to see if it can find a matching
	 * subscription for the client and then removes it. After this point the
	 * server will send no more messages to the client for this subscription.
	 * </p>
	 * <p>
	 * The topic(s) specified on the unsubscribe must match the topic(s)
	 * specified in the original subscribe request for the unsubscribe to
	 * succeed
	 * </p>
	 * <p>
	 * The method returns control before the unsubscribe completes. Completion
	 * can be tracked by:
     * </p>
	 * <ul>
	 * <li>Waiting on the returned token {@link MqttToken#waitForCompletion()}
	 * or</li>
	 * <li>Passing in a callback {@link IMqttActionListener} to this method</li>
	 * </ul>
	 * 
	 * @param topic
	 *            one or more topics to unsubscribe from. Each topic must match
	 *            one specified on an earlier subscription.
	 * @param userContext
	 *            optional object used to pass context to the callback. Use null
	 *            if not required.
	 * @param callback
	 *            optional listener that will be notified when unsubscribe has
	 *            completed
	 * @return token used to track and wait for the unsubscribe to complete. The
	 *         token will be passed to callback methods if set.
	 */
	@Override
	public IMqttToken unsubscribe(String[] topic, Object userContext,
			IMqttActionListener callback) {
		IMqttToken token = new MqttTokenAndroid(this, userContext,
				callback);
		String activityToken = storeToken(token);
		mqttService.unsubscribe(clientHandle, topic, null, activityToken);
		return token;
	}

	/**
	 * Returns the delivery tokens for any outstanding publish operations.
	 * <p>
	 * If a client has been restarted and there are messages that were in the
	 * process of being delivered when the client stopped, this method returns a
	 * token for each in-flight message to enable the delivery to be tracked.
	 * Alternately the {@link MqttCallback#deliveryComplete(IMqttDeliveryToken)}
	 * callback can be used to track the delivery of outstanding messages.
	 * </p>
	 * <p>
	 * If a client connects with cleanSession true then there will be no
	 * delivery tokens as the cleanSession option deletes all earlier state. For
	 * state to be remembered the client must connect with cleanSession set to
	 * false
	 * </P>
	 * 
	 * @return zero or more delivery tokens
	 */
	@Override
	public IMqttDeliveryToken[] getPendingDeliveryTokens() {
		return mqttService.getPendingDeliveryTokens(clientHandle);
	}

	/**
	 * Sets a callback listener to use for events that happen asynchronously.
	 * <p>
	 * There are a number of events that the listener will be notified about.
	 * These include:
     * </p>
	 * <ul>
	 * <li>A new message has arrived and is ready to be processed</li>
	 * <li>The connection to the server has been lost</li>
	 * <li>Delivery of a message to the server has completed</li>
	 * </ul>
	 * <p>
	 * Other events that track the progress of an individual operation such as
	 * connect and subscribe can be tracked using the {@link MqttToken} returned
	 * from each non-blocking method or using setting a
	 * {@link IMqttActionListener} on the non-blocking method.
	 * <p>
	 * 
	 * @param callback
	 *            which will be invoked for certain asynchronous events
	 * 
	 * @see MqttCallback
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
	 *            set <code>true</code> to enable trace, otherwise, set
	 *            <code>false</code> to disable trace
	 * 
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

		if ((handleFromIntent == null)
				|| (!handleFromIntent.equals(clientHandle))) {
			return;
		}

		String action = data.getString(MqttServiceConstants.CALLBACK_ACTION);

		if (MqttServiceConstants.CONNECT_ACTION.equals(action)) {
			connectAction(data);
		}
		else if (MqttServiceConstants.CONNECT_EXTENDED_ACTION.equals(action)){
			connectExtendedAction(data);
		}
		else if (MqttServiceConstants.MESSAGE_ARRIVED_ACTION.equals(action)) {
			messageArrivedAction(data);
		}
		else if (MqttServiceConstants.SUBSCRIBE_ACTION.equals(action)) {
			subscribeAction(data);
		}
		else if (MqttServiceConstants.UNSUBSCRIBE_ACTION.equals(action)) {
			unSubscribeAction(data);
		}
		else if (MqttServiceConstants.SEND_ACTION.equals(action)) {
			sendAction(data);
		}
		else if (MqttServiceConstants.MESSAGE_DELIVERED_ACTION.equals(action)) {
			messageDeliveredAction(data);
		}
		else if (MqttServiceConstants.ON_CONNECTION_LOST_ACTION
				.equals(action)) {
			connectionLostAction(data);
		}
		else if (MqttServiceConstants.DISCONNECT_ACTION.equals(action)) {
			disconnected(data);
		}
		else if (MqttServiceConstants.TRACE_ACTION.equals(action)) {
			traceAction(data);
		}else{
			mqttService.traceError(MqttService.TAG, "Callback action doesn't exist.");	
		}

	}

	/**
	 * Acknowledges a message received on the
	 * {@link MqttCallback#messageArrived(String, MqttMessage)}
	 * 
	 * @param messageId
	 *            the messageId received from the MqttMessage (To access this
	 *            field you need to cast {@link MqttMessage} to
	 *            {@link ParcelableMqttMessage})
	 * @return whether or not the message was successfully acknowledged
	 */
	public boolean acknowledgeMessage(String messageId) {
		if (messageAck == Ack.MANUAL_ACK) {
			Status status = mqttService.acknowledgeMessageArrival(clientHandle, messageId);
			return status == Status.OK;
		}
		return false;

	}
	
	public void messageArrivedComplete(int messageId, int qos) throws MqttException {
		throw new UnsupportedOperationException();	
	}
	
	public void setManualAcks(boolean manualAcks) {
		throw new UnsupportedOperationException();	
	}

	/**
	 * Process the results of a connection
	 * 
	 * @param data
	 */
	private void connectAction(Bundle data) {
		IMqttToken token = connectToken;
		removeMqttToken(data);
		
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
			((MqttTokenAndroid) token).notifyComplete();
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

	private void connectExtendedAction(Bundle data){
		// This is called differently from a normal connect

		if(callback instanceof MqttCallbackExtended){
			boolean reconnect = data.getBoolean(MqttServiceConstants.CALLBACK_RECONNECT, false);
			String serverURI = data.getString(MqttServiceConstants.CALLBACK_SERVER_URI);
			((MqttCallbackExtended) callback).connectComplete(reconnect, serverURI);
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
				((MqttTokenAndroid) token).notifyComplete();
			}
			else {
				Exception exceptionThrown = (Exception) data.getSerializable(MqttServiceConstants.CALLBACK_EXCEPTION);
				((MqttTokenAndroid) token).notifyFailure(exceptionThrown);
			}
		} else {
			mqttService.traceError(MqttService.TAG, "simpleAction : token is null");	
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
				if (status == Status.OK && token instanceof IMqttDeliveryToken) {
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

			ParcelableMqttMessage message = data
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
				mqttService.traceError(MqttService.TAG, "messageArrivedAction failed: " + e);
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
			if (MqttServiceConstants.TRACE_DEBUG.equals(severity)) 
				traceCallback.traceDebug(tag, message);
			else if (MqttServiceConstants.TRACE_ERROR.equals(severity)) 
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
		
		String activityToken = data.getString(MqttServiceConstants.CALLBACK_ACTIVITY_TOKEN);
		if (activityToken!=null){
			int tokenNumber = Integer.parseInt(activityToken);
			IMqttToken token = tokenMap.get(tokenNumber);
			tokenMap.delete(tokenNumber);
			return token;
		}
		return null;
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
		return tokenMap.get(Integer.parseInt(activityToken));
	}

	/**
	 * Sets the DisconnectedBufferOptions for this client
	 * @param bufferOpts the DisconnectedBufferOptions
	 */
	public void setBufferOpts(DisconnectedBufferOptions bufferOpts) {
		mqttService.setBufferOpts(clientHandle, bufferOpts);
	}

	public int getBufferedMessageCount(){
		return mqttService.getBufferedMessageCount(clientHandle);
	}

	public MqttMessage getBufferedMessage(int bufferIndex){
		return mqttService.getBufferedMessage(clientHandle, bufferIndex);
	}

	public void deleteBufferedMessage(int bufferIndex){
		mqttService.deleteBufferedMessage(clientHandle, bufferIndex);
	}
	
	/**
	 * Get the SSLSocketFactory using SSL key store and password
	 * <p>
	 * A convenience method, which will help user to create a SSLSocketFactory
	 * object
	 * </p>
	 * 
	 * @param keyStore
	 *            the SSL key store which is generated by some SSL key tool,
	 *            such as keytool in Java JDK
	 * @param password
	 *            the password of the key store which is set when the key store
	 *            is generated
	 * @return SSLSocketFactory used to connect to the server with SSL
	 *         authentication
	 * @throws MqttSecurityException
	 *             if there was any error when getting the SSLSocketFactory
	 */
	public SSLSocketFactory getSSLSocketFactory (InputStream keyStore, String password) throws MqttSecurityException {
	 try{
		 SSLContext ctx = null;
		 SSLSocketFactory sslSockFactory=null;
		 KeyStore ts;
		 ts = KeyStore.getInstance("BKS");			
		 ts.load(keyStore, password.toCharArray());
		 TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
		 tmf.init(ts);
		 TrustManager[] tm = tmf.getTrustManagers();
		 ctx = SSLContext.getInstance("TLSv1");
		 ctx.init(null, tm, null);
		 
		 sslSockFactory=ctx.getSocketFactory();
		 return sslSockFactory;
		 
		} catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException | KeyManagementException e) {
			throw new MqttSecurityException(e);
		}
	}

	@Override
	public void disconnectForcibly() throws MqttException {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void disconnectForcibly(long disconnectTimeout) throws MqttException {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void disconnectForcibly(long quiesceTimeout, long disconnectTimeout)
			throws MqttException {
		throw new UnsupportedOperationException();	
	}
	
	/**
	 * Unregister receiver which receives intent from MqttService avoids
	 * IntentReceiver leaks.
	 */
	public void unregisterResources(){
		if(myContext != null && receiverRegistered){
			synchronized (MqttAndroidClient.this) {
				LocalBroadcastManager.getInstance(myContext).unregisterReceiver(this);
				receiverRegistered = false;
			}
			if(bindedService){
				try{
					myContext.unbindService(serviceConnection);
					bindedService = false;
				}catch(IllegalArgumentException e){
					//Ignore unbind issue.
				}
			}
		}
	}
	
	/**
	 * Register receiver to receiver intent from MqttService. Call this method
	 * when activity is hidden and become to show again.
	 * 
	 * @param context
	 *            - Current activity context.
	 */
	public void registerResources(Context context){
		if(context != null){
			this.myContext = context;
			if(!receiverRegistered){
				registerReceiver(this);
			}
		}
	}
}
