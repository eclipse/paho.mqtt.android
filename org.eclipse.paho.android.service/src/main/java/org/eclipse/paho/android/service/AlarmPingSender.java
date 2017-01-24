/*******************************************************************************
 * Copyright (c) 2014 IBM Corp.
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
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttPingSender;
import org.eclipse.paho.client.mqttv3.internal.ClientComms;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

/**
 * Default ping sender implementation on Android. It is based on AlarmManager.
 *
 * <p>This class implements the {@link MqttPingSender} pinger interface
 * allowing applications to send ping packet to server every keep alive interval.
 * </p>
 *
 * @see MqttPingSender
 */
class AlarmPingSender implements MqttPingSender {
	// Identifier for Intents, log messages, etc..
	private static final String TAG = "AlarmPingSender";

	// TODO: Add log.
	private ClientComms comms;
	private MqttService service;
	private BroadcastReceiver alarmReceiver;
	private AlarmPingSender that;
	private PendingIntent pendingIntent;
	private volatile boolean hasStarted = false;

	public AlarmPingSender(MqttService service) {
		if (service == null) {
			throw new IllegalArgumentException(
					"Neither service nor client can be null.");
		}
		this.service = service;
		that = this;
	}

	@Override
	public void init(ClientComms comms) {
		this.comms = comms;
		this.alarmReceiver = new AlarmReceiver();
	}

	@Override
	public void start() {
		String action = MqttServiceConstants.PING_SENDER
				+ comms.getClient().getClientId();
		Log.d(TAG, "Register alarmreceiver to MqttService"+ action);
		service.registerReceiver(alarmReceiver, new IntentFilter(action));

		pendingIntent = PendingIntent.getBroadcast(service, 0, new Intent(
				action), PendingIntent.FLAG_UPDATE_CURRENT);

		schedule(comms.getKeepAlive());
		hasStarted = true;
	}

	@Override
	public void stop() {

		Log.d(TAG, "Unregister alarmreceiver to MqttService"+comms.getClient().getClientId());
		if(hasStarted){
			if(pendingIntent != null){
				// Cancel Alarm.
				AlarmManager alarmManager = (AlarmManager) service.getSystemService(Service.ALARM_SERVICE);
				alarmManager.cancel(pendingIntent);
			}

			hasStarted = false;
			try{
				service.unregisterReceiver(alarmReceiver);
			}catch(IllegalArgumentException e){
				//Ignore unregister errors.			
			}
		}
	}

	@Override
	public void schedule(long delayInMilliseconds) {
		long nextAlarmInMilliseconds = System.currentTimeMillis()
				+ delayInMilliseconds;
		Log.d(TAG, "Schedule next alarm at " + nextAlarmInMilliseconds);
		AlarmManager alarmManager = (AlarmManager) service
				.getSystemService(Service.ALARM_SERVICE);

        if(Build.VERSION.SDK_INT >= 23){
			// In SDK 23 and above, dosing will prevent setExact, setExactAndAllowWhileIdle will force
			// the device to run this task whilst dosing.
			Log.d(TAG, "Alarm scheule using setExactAndAllowWhileIdle, next: " + delayInMilliseconds);
			alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAlarmInMilliseconds,
					pendingIntent);
		} else if (Build.VERSION.SDK_INT >= 19) {
			Log.d(TAG, "Alarm scheule using setExact, delay: " + delayInMilliseconds);
			alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextAlarmInMilliseconds,
					pendingIntent);
		} else {
			alarmManager.set(AlarmManager.RTC_WAKEUP, nextAlarmInMilliseconds,
					pendingIntent);
		}
	}

	/*
	 * This class sends PingReq packet to MQTT broker
	 */
	class AlarmReceiver extends BroadcastReceiver {
		private WakeLock wakelock;
		private final String wakeLockTag = MqttServiceConstants.PING_WAKELOCK
				+ that.comms.getClient().getClientId();

		@Override
        @SuppressLint("Wakelock")
		public void onReceive(Context context, Intent intent) {
			// According to the docs, "Alarm Manager holds a CPU wake lock as
			// long as the alarm receiver's onReceive() method is executing.
			// This guarantees that the phone will not sleep until you have
			// finished handling the broadcast.", but this class still get
			// a wake lock to wait for ping finished.

			Log.d(TAG, "Sending Ping at:" + System.currentTimeMillis());

			PowerManager pm = (PowerManager) service
					.getSystemService(Service.POWER_SERVICE);
			wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, wakeLockTag);
			wakelock.acquire();

			// Assign new callback to token to execute code after PingResq
			// arrives. Get another wakelock even receiver already has one,
			// release it until ping response returns.
			IMqttToken token = comms.checkForActivity(new IMqttActionListener() {

				@Override
				public void onSuccess(IMqttToken asyncActionToken) {
					Log.d(TAG, "Success. Release lock(" + wakeLockTag + "):"
							+ System.currentTimeMillis());
					//Release wakelock when it is done.
					wakelock.release();
				}

				@Override
				public void onFailure(IMqttToken asyncActionToken,
									  Throwable exception) {
					Log.d(TAG, "Failure. Release lock(" + wakeLockTag + "):"
							+ System.currentTimeMillis());
					//Release wakelock when it is done.
					wakelock.release();
				}
			});


			if (token == null && wakelock.isHeld()) {
				wakelock.release();
			}
		}
	}
}
