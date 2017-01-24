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
 */
package org.eclipse.paho.android.service;

/**
 * Various strings used to identify operations or data in the Android MQTT
 * service, mainly used in Intents passed between Activities and the Service.
 */
interface MqttServiceConstants {

	/*
	 * Version information
	 */
	
	String VERSION = "v0";
	
  /*
   * Attributes of messages <p> Used for the column names in the database
   */
  String DUPLICATE = "duplicate";
  String RETAINED = "retained";
  String QOS = "qos";
  String PAYLOAD = "payload";
  String DESTINATION_NAME = "destinationName";
  String CLIENT_HANDLE = "clientHandle";
  String MESSAGE_ID = "messageId";

  /* Tags for actions passed between the Activity and the Service */
  String SEND_ACTION = "send";
  String UNSUBSCRIBE_ACTION = "unsubscribe";
  String SUBSCRIBE_ACTION = "subscribe";
  String DISCONNECT_ACTION = "disconnect";
  String CONNECT_ACTION = "connect";
  String CONNECT_EXTENDED_ACTION = "connectExtended";
  String MESSAGE_ARRIVED_ACTION = "messageArrived";
  String MESSAGE_DELIVERED_ACTION = "messageDelivered";
  String ON_CONNECTION_LOST_ACTION = "onConnectionLost";
  String TRACE_ACTION = "trace";

  /* Identifies an Intent which calls back to the Activity */
  String CALLBACK_TO_ACTIVITY = MqttService.TAG
                                             + ".callbackToActivity"+"."+VERSION;

  /* Identifiers for extra data on Intents broadcast to the Activity */
  String CALLBACK_ACTION = MqttService.TAG + ".callbackAction";
  String CALLBACK_STATUS = MqttService.TAG + ".callbackStatus";
  String CALLBACK_CLIENT_HANDLE = MqttService.TAG + "."
                                               + CLIENT_HANDLE;
  String CALLBACK_ERROR_MESSAGE = MqttService.TAG
                                               + ".errorMessage";
  String CALLBACK_EXCEPTION_STACK = MqttService.TAG
                                                 + ".exceptionStack";
  String CALLBACK_INVOCATION_CONTEXT = MqttService.TAG + "."
                                                    + "invocationContext";
  String CALLBACK_ACTIVITY_TOKEN = MqttService.TAG + "."
                                                + "activityToken";
  String CALLBACK_DESTINATION_NAME = MqttService.TAG + '.'
                                                  + DESTINATION_NAME;
  String CALLBACK_MESSAGE_ID = MqttService.TAG + '.'
                                            + MESSAGE_ID;
  String CALLBACK_RECONNECT = MqttService.TAG + ".reconnect";
  String CALLBACK_SERVER_URI = MqttService.TAG + ".serverURI";
  String CALLBACK_MESSAGE_PARCEL = MqttService.TAG + ".PARCEL";
  String CALLBACK_TRACE_SEVERITY = MqttService.TAG
                                                + ".traceSeverity";
  String CALLBACK_TRACE_TAG = MqttService.TAG + ".traceTag";
  String CALLBACK_TRACE_ID = MqttService.TAG + ".traceId";
  String CALLBACK_ERROR_NUMBER = MqttService.TAG
                                              + ".ERROR_NUMBER";

  String CALLBACK_EXCEPTION = MqttService.TAG + ".exception";
  
  //Intent prefix for Ping sender.
  String PING_SENDER = MqttService.TAG + ".pingSender.";
  
  //Constant for wakelock
  String PING_WAKELOCK = MqttService.TAG + ".client.";
  String WAKELOCK_NETWORK_INTENT = MqttService.TAG + "";

  //Trace severity levels  
  String TRACE_ERROR = "error";
  String TRACE_DEBUG = "debug";
  String TRACE_EXCEPTION = "exception";
  
  
  //exception code for non MqttExceptions
  int NON_MQTT_EXCEPTION = -1;

}