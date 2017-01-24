package org.eclipse.paho.android.sample.internal;

import org.eclipse.paho.android.sample.model.ReceivedMessage;

public interface IReceivedMessageListener {

    void onMessageReceived(ReceivedMessage message);
}