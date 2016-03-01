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

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * <p>
 * Implementation of the IMqttDeliveryToken interface for use from within the
 * MqttAndroidClient implementation
 */
class MqttDeliveryTokenAndroid extends MqttTokenAndroid
		implements IMqttDeliveryToken {

	// The message which is being tracked by this token
	private MqttMessage message;

	MqttDeliveryTokenAndroid(MqttAndroidClient client,
			Object userContext, IMqttActionListener listener, MqttMessage message) {
		super(client, userContext, listener);
		this.message = message;
	}

	/**
	 * @see org.eclipse.paho.client.mqttv3.IMqttDeliveryToken#getMessage()
	 */
	@Override
	public MqttMessage getMessage() throws MqttException {
		return message;
	}

	void setMessage(MqttMessage message) {
		this.message = message;
	}

	void notifyDelivery(MqttMessage delivered) {
		message = delivered;
		super.notifyComplete();
	}

}
