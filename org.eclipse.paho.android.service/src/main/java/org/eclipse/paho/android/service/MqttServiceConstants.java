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
package org.eclipse.paho.android.service;

/**
 * Various strings used to identify operations or data in the Android MQTT
 * service, mainly used in Intents passed between Activities and the Service.
 */
interface MqttServiceConstants {

	/*
	 * Version information
	 */
	
	static final String VERSION = "v0";
	
  /*
   * Attributes of messages <p> Used for the column names in the database
   */
  static final String DUPLICATE = "duplicate";
  static final String RETAINED = "retained";
  static final String QOS = "qos";
  static final String PAYLOAD = "payload";
  static final String DESTINATION_NAME = "destinationName";
  static final String CLIENT_HANDLE = "clientHandle";
  static final String MESSAGE_ID = "messageId";

  /* Tags for actions passed between the Activity and the Service */
  static final String SEND_ACTION = "send";
  static final String UNSUBSCRIBE_ACTION = "unsubscribe";
  static final String SUBSCRIBE_ACTION = "subscribe";
  static final String DISCONNECT_ACTION = "disconnect";
  static final String CONNECT_ACTION = "connect";
  static final String MESSAGE_ARRIVED_ACTION = "messageArrived";
  static final String MESSAGE_DELIVERED_ACTION = "messageDelivered";
  static final String ON_CONNECTION_LOST_ACTION = "onConnectionLost";
  static final String TRACE_ACTION = "trace";

  /* Identifies an Intent which calls back to the Activity */
  static final String CALLBACK_TO_ACTIVITY = MqttService.TAG
                                             + ".callbackToActivity"+"."+VERSION;

  /* Identifiers for extra data on Intents broadcast to the Activity */
  static final String CALLBACK_ACTION = MqttService.TAG + ".callbackAction";
  static final String CALLBACK_STATUS = MqttService.TAG + ".callbackStatus";
  static final String CALLBACK_CLIENT_HANDLE = MqttService.TAG + "."
                                               + CLIENT_HANDLE;
  static final String CALLBACK_ERROR_MESSAGE = MqttService.TAG
                                               + ".errorMessage";
  static final String CALLBACK_EXCEPTION_STACK = MqttService.TAG
                                                 + ".exceptionStack";
  static final String CALLBACK_INVOCATION_CONTEXT = MqttService.TAG + "."
                                                    + "invocationContext";
  static final String CALLBACK_ACTIVITY_TOKEN = MqttService.TAG + "."
                                                + "activityToken";
  static final String CALLBACK_DESTINATION_NAME = MqttService.TAG + '.'
                                                  + DESTINATION_NAME;
  static final String CALLBACK_MESSAGE_ID = MqttService.TAG + '.'
                                            + MESSAGE_ID;
  static final String CALLBACK_MESSAGE_PARCEL = MqttService.TAG + ".PARCEL";
  static final String CALLBACK_TRACE_SEVERITY = MqttService.TAG
                                                + ".traceSeverity";
  static final String CALLBACK_TRACE_TAG = MqttService.TAG + ".traceTag";
  static final String CALLBACK_TRACE_ID = MqttService.TAG + ".traceId";
  static final String CALLBACK_ERROR_NUMBER = MqttService.TAG
                                              + ".ERROR_NUMBER";

  static final String CALLBACK_EXCEPTION = MqttService.TAG + ".exception";
  
  //Intent prefix for Ping sender.
  static final String PING_SENDER = MqttService.TAG + ".pingSender.";
  
  //Constant for wakelock
  static final String PING_WAKELOCK = MqttService.TAG + ".client.";  
  static final String WAKELOCK_NETWORK_INTENT = MqttService.TAG + "";  

  //Trace severity levels  
  static final String TRACE_ERROR = "error";
  static final String TRACE_DEBUG = "debug";
  static final String TRACE_EXCEPTION = "exception";
  
  
  //exception code for non MqttExceptions
  static final int NON_MQTT_EXCEPTION = -1;

}