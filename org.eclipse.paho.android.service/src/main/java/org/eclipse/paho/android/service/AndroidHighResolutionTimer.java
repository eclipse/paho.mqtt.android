package org.eclipse.paho.android.service;

import android.os.Build;
import android.os.SystemClock;

import org.eclipse.paho.client.mqttv3.internal.HighResolutionTimer;

import java.util.concurrent.TimeUnit;

/**
 * An android {@code HighResolutionTimer} implementation that includes the time spent asleep.
 */
public class AndroidHighResolutionTimer implements HighResolutionTimer {
    @Override
    public long nanoTime() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return SystemClock.elapsedRealtimeNanos();
        } else {
            // Use the old currentTimeMillis implementation in API levels that don't support elapsedRealtimeNanos
            // See: https://github.com/eclipse/paho.mqtt.java/issues/278
            return TimeUnit.NANOSECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }
    }
}
