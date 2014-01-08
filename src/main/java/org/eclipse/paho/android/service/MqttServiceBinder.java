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

import android.os.Binder;

/**
 * What the Service passes to the Activity on binding:-
 * <ul>
 * <li>a reference to the Service
 * <li>the activityToken provided when the Service was started
 * </ul>
 * 
 */
public class MqttServiceBinder extends Binder {

	private MqttService mqttService;
	private String activityToken;

	MqttServiceBinder(MqttService mqttService) {
		this.mqttService = mqttService;
	}

	/**
	 * @return a reference to the Service
	 */
	public MqttService getService() {
		return mqttService;
	}

	void setActivityToken(String activityToken) {
		this.activityToken = activityToken;
	}

	/**
	 * @return the activityToken provided when the Service was started
	 */
	public String getActivityToken() {
		return activityToken;
	}

}
