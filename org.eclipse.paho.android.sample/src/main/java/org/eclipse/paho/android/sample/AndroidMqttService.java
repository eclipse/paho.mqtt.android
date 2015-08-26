package org.eclipse.paho.android.sample;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class AndroidMqttService extends Service {
    public AndroidMqttService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
