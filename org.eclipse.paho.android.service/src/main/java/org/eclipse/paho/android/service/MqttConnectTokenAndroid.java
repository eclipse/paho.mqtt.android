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
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;

/**
 * <p>
 * Implementation of the IMqttToken interface only for sessionPresent
 */
class MqttConnectTokenAndroid implements IMqttToken {
    private boolean sessionPresent;

    MqttConnectTokenAndroid(boolean sessionPresent) {
        this.sessionPresent = sessionPresent;
    }

    void setMessage(MqttMessage message) {
    }

    void notifyDelivery(MqttMessage delivered) {
    }

    @Override
    public void waitForCompletion() throws MqttException {

    }

    @Override
    public void waitForCompletion(long timeout) throws MqttException {

    }

    @Override
    public boolean isComplete() {
        return false;
    }

    @Override
    public MqttException getException() {
        return null;
    }

    @Override
    public IMqttActionListener getActionCallback() {
        return null;
    }

    @Override
    public void setActionCallback(IMqttActionListener listener) {

    }

    @Override
    public IMqttAsyncClient getClient() {
        return null;
    }

    @Override
    public String[] getTopics() {
        return new String[0];
    }

    @Override
    public Object getUserContext() {
        return null;
    }

    @Override
    public void setUserContext(Object userContext) {

    }

    @Override
    public int getMessageId() {
        return 0;
    }

    @Override
    public int[] getGrantedQos() {
        return new int[0];
    }

    @Override
    public boolean getSessionPresent() {
        return this.sessionPresent;
    }

    @Override
    public MqttWireMessage getResponse() {
        return null;
    }
}
