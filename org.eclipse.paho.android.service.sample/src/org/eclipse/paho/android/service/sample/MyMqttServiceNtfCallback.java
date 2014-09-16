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

import org.eclipse.paho.android.service.MqttService;
import org.eclipse.paho.android.service.MqttServiceNotificationCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.util.Log;


public class MyMqttServiceNtfCallback implements MqttServiceNotificationCallback {

	@Override
	public void notify(MqttService service,String topic,MqttMessage message) {
		
		NotificationCompat.Builder b=new NotificationCompat.Builder(service)
		.setContentTitle("message["+topic+"]")
		.setContentText(new String(message.getPayload()))
		.setSmallIcon(R.drawable.ic_launcher)
		.setAutoCancel(true);
		
		Notification n=b.build();
		
		NotificationManager nm = (NotificationManager)service.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify(103245, n);
	}


}
