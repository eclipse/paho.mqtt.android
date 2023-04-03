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

    private static final String TAG = "AlarmPingSender";

    private final AlarmPingSender that;
    private final MqttService mqttService;

    private ClientComms clientComms;
    private AlarmManager alarmManager;
    private BroadcastReceiver alarmReceiver;
    private PendingIntent pendingIntent;
    private volatile boolean hasStarted = false;

    public AlarmPingSender(MqttService service) {
        if (service == null) {
            throw new IllegalArgumentException("Neither service nor client can be null.");
        }
        that = this;
        this.mqttService = service;
    }

    @Override
    public void init(ClientComms clientComms) {
        this.clientComms = clientComms;
        this.alarmReceiver = new AlarmReceiver();
    }

    private AlarmManager getAlarmManager() {
        if (mqttService == null) {
            throw new IllegalArgumentException("MqttService is null.");
        }
        if (alarmManager == null) {
            alarmManager = (AlarmManager) mqttService.getSystemService(Service.ALARM_SERVICE);
        }
        return alarmManager;
    }


    @Override
    public void start() {
        String action = MqttServiceConstants.PING_SENDER + clientComms.getClient().getClientId();
        Log.d(TAG, "Register alarmReceiver to MqttService " + action);
        mqttService.registerReceiver(alarmReceiver, new IntentFilter(action));

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        pendingIntent = PendingIntent.getBroadcast(mqttService, 0, new Intent(action), flags);
        schedule(clientComms.getKeepAlive());
        hasStarted = true;
    }

    @Override
    public void stop() {
        Log.d(TAG, "Unregister alarmReceiver to MqttService " + clientComms.getClient().getClientId());
        if (hasStarted) {
            if (pendingIntent != null) {
                AlarmManager am = getAlarmManager();
                if (am != null) {
                    am.cancel(pendingIntent);
                }
            }
            hasStarted = false;
            try {
                mqttService.unregisterReceiver(alarmReceiver);
            } catch (IllegalArgumentException e) {
                //Ignore unregister errors.
            }
        }
    }

    @Override
    public void schedule(long delayInMilliseconds) {
        long triggerInMilliseconds = System.currentTimeMillis() + delayInMilliseconds;
        Log.d(TAG, "Schedule next alarm at " + triggerInMilliseconds);
        AlarmManager am = getAlarmManager();
        if (am != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    Log.d(TAG, "Alarm schedule using setExactAndAllowWhileIdle, next: " + delayInMilliseconds);
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerInMilliseconds, pendingIntent);
                } else {
                    // TODO 没有权限时的处理逻辑
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // In SDK 23 and above, dosing will prevent setExact, setExactAndAllowWhileIdle will force
                // the device to run this task whilst dosing.
                Log.d(TAG, "Alarm schedule using setExactAndAllowWhileIdle, next: " + delayInMilliseconds);
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerInMilliseconds, pendingIntent);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                Log.d(TAG, "Alarm schedule using setExact, delay: " + delayInMilliseconds);
                am.setExact(AlarmManager.RTC_WAKEUP, triggerInMilliseconds, pendingIntent);
            } else {
                am.set(AlarmManager.RTC_WAKEUP, triggerInMilliseconds, pendingIntent);
            }
        }
    }

    /**
     * This class sends PingReq packet to MQTT broker
     */
    class AlarmReceiver extends BroadcastReceiver {
        private WakeLock wakelock;
        private final String wakeLockTag = MqttServiceConstants.PING_WAKELOCK + that.clientComms.getClient().getClientId();

        @Override
        @SuppressLint("Wakelock")
        public void onReceive(Context context, Intent intent) {
            // According to the docs, "Alarm Manager holds a CPU wake lock as long as the alarm
            // receiver's onReceive() method is executing.
            // This guarantees that the phone will not sleep until you have finished handling the
            // broadcast.", but this class still get a wake lock to wait for ping finished.

            Log.d(TAG, "Sending Ping at:" + System.currentTimeMillis());
            PowerManager pm = (PowerManager) mqttService.getSystemService(Service.POWER_SERVICE);
            if (pm != null) {
                wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, wakeLockTag);
                if (wakelock != null) {
                    wakelock.acquire(10 * 60 * 1000L /*10 minutes*/);
                }
            }

            // Assign new callback to token to execute code after PingResq arrives.
            // Get another wakelock even receiver already has one, release it until ping response returns.
            IMqttToken token = clientComms.checkForActivity(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, String.format("Success. Release lock(\"%s\"):%d", wakeLockTag, System.currentTimeMillis()));
                    if (wakelock != null) {
                        wakelock.release();    //Release wakelock when it is done.
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d(TAG, String.format("Failure. Release lock(\"%s\"):%d", wakeLockTag, System.currentTimeMillis()));
                    if (wakelock != null) {
                        wakelock.release();    //Release wakelock when it is done.
                    }
                }
            });

            if (token == null && wakelock.isHeld() && wakelock != null) {
                wakelock.release();
            }
        }
    }
}