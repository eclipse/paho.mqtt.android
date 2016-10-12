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
package org.eclipse.paho.android.sample.activity;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

//import org.eclipse.paho.android.sample.Connection.ConnectionStatus;
import org.eclipse.paho.android.sample.R;
import org.eclipse.paho.android.sample.internal.Connections;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Handles call backs from the MQTT Client
 *
 */
class MqttCallbackHandler implements MqttCallback {

  /** {@link Context} for the application used to format and import external strings**/
  private final Context context;
  /** Client handle to reference the connection that this handler is attached to**/
  private final String clientHandle;

  private static final String TAG = "MqttCallbackHandler";
    private static final String activityClass = "org.eclipse.paho.android.sample.activity.MainActivity";

  /**
   * Creates an <code>MqttCallbackHandler</code> object
   * @param context The application's context
   * @param clientHandle The handle to a {@link Connection} object
   */
  public MqttCallbackHandler(Context context, String clientHandle)
  {
    this.context = context;
    this.clientHandle = clientHandle;
  }

  /**
   * @see org.eclipse.paho.client.mqttv3.MqttCallback#connectionLost(java.lang.Throwable)
   */
  @Override
  public void connectionLost(Throwable cause) {
    if (cause != null) {
      Log.d(TAG, "Connection Lost: " + cause.getMessage());
      Connection c = Connections.getInstance(context).getConnection(clientHandle);
      c.addAction("Connection Lost");
      c.changeConnectionStatus(Connection.ConnectionStatus.DISCONNECTED);

      String message = context.getString(R.string.connection_lost, c.getId(), c.getHostName());

      //build intent
      Intent intent = new Intent();
      intent.setClassName(context, activityClass);
      intent.putExtra("handle", clientHandle);

      //notify the user
      Notify.notifcation(context, message, intent, R.string.notifyTitle_connectionLost);
    }
  }

  /**
   * @see org.eclipse.paho.client.mqttv3.MqttCallback#messageArrived(java.lang.String, org.eclipse.paho.client.mqttv3.MqttMessage)
   */
  @Override
  public void messageArrived(String topic, MqttMessage message) throws Exception {

    //Get connection object associated with this object
    Connection c = Connections.getInstance(context).getConnection(clientHandle);
    c.messageArrived(topic, message);
    //get the string from strings.xml and format
    String messageString = context.getString(R.string.messageRecieved, new String(message.getPayload()), topic+";qos:"+message.getQos()+";retained:"+message.isRetained());

    Log.i(TAG, messageString);

    //update client history
    c.addAction(messageString);

  }

  /**
   * @see org.eclipse.paho.client.mqttv3.MqttCallback#deliveryComplete(org.eclipse.paho.client.mqttv3.IMqttDeliveryToken)
   */
  @Override
  public void deliveryComplete(IMqttDeliveryToken token) {
    // Do nothing
  }

}
