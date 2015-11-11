package org.eclipse.paho.android.sample.internal;

import org.eclipse.paho.android.sample.model.ReceivedMessage;

/**
 * Created by james on 12/10/15.
 */
public interface IReceivedMessageListener {

    public void onMessageReceived(ReceivedMessage message);
}