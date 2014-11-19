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

import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * This Class provides constants used for returning results from an activity 
 *
 */
public class ActivityConstants {

  /** Application TAG for logs where class name is not used*/
  static final String TAG = "MQTT Android";

  /*Default values **/

  /** Default QOS value*/
  static final int defaultQos = 0;
  /** Default timeout*/
  static final int defaultTimeOut = 1000;
  /** Default keep alive value*/
  static final int defaultKeepAlive = 10;
  /** Default SSL enabled flag*/
  static final boolean defaultSsl = false;
  /** Default message retained flag */
  static final boolean defaultRetained = false;
  /** Default last will message*/
  static final MqttMessage defaultLastWill = null;
  /** Default port*/
  static final int defaultPort = 1883;

  /** Connect Request Code */
  static final int connect = 0;
  /** Advanced Connect Request Code  **/
  static final int advancedConnect = 1;
  /** Last will Request Code  **/
  static final int lastWill = 2;
  /** Show History Request Code  **/
  static final int showHistory = 3;

  /* Bundle Keys */

  /** Server Bundle Key **/
  static final String server = "server";
  /** Port Bundle Key **/
  static final String port = "port";
  /** ClientID Bundle Key **/
  static final String clientId = "clientId";
  /** Topic Bundle Key **/
  static final String topic = "topic";
  /** History Bundle Key **/
  static final String history = "history";
  /** Message Bundle Key **/
  static final String message = "message";
  /** Retained Flag Bundle Key **/
  static final String retained = "retained";
  /** QOS Value Bundle Key **/
  static final String qos = "qos";
  /** User name Bundle Key **/
  static final String username = "username";
  /** Password Bundle Key **/
  static final String password = "password";
  /** Keep Alive value Bundle Key **/
  static final String keepalive = "keepalive";
  /** Timeout Bundle Key **/
  static final String timeout = "timeout";
  /** SSL Enabled Flag Bundle Key **/
  static final String ssl = "ssl";
  /** SSL Key File Bundle Key **/
  static final String ssl_key = "ssl_key";
  /** Connections Bundle Key **/
  static final String connections = "connections";
  /** Clean Session Flag Bundle Key **/
  static final String cleanSession = "cleanSession";
  /** Action Bundle Key **/
  static final String action = "action";

  /* Property names */

  /** Property name for the history field in {@link Connection} object for use with {@link java.beans.PropertyChangeEvent} **/
  static final String historyProperty = "history";

  /** Property name for the connection status field in {@link Connection} object for use with {@link java.beans.PropertyChangeEvent} **/
  static final String ConnectionStatusProperty = "connectionStatus";

  /* Useful constants*/

  /** Space String Literal **/
  static final String space = " ";
  /** Empty String for comparisons **/
  static final String empty = new String();

}
