/*******************************************************************************
 * Copyright (c) 2015, 2016 IBM Corp.
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
package org.eclipse.paho.android;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import android.util.Log;

public class MqttV3Receiver implements MqttCallback{

    private final java.io.PrintStream reportStream;
    private boolean reportConnectionLoss = true;
    private boolean connected = false;
    private String clientId;

    /**
     * For the in bound message.
     */
    public class ReceivedMessage {

        /** */
        public String topic;
        /** */
        public MqttMessage message;

        ReceivedMessage(String topic, MqttMessage message) {
            this.topic = topic;
            this.message = message;
        }
    }

    private java.util.List<ReceivedMessage> receivedMessages = Collections.synchronizedList(new java.util.ArrayList<ReceivedMessage>());

    /**
     * @param mqttClient
     * @param reportStream
     */
    public MqttV3Receiver(IMqttClient mqttClient, PrintStream reportStream) {

        this.reportStream = reportStream;
        connected = true;

        clientId = mqttClient.getClientId();

    }

    public MqttV3Receiver(IMqttAsyncClient mqttClient, PrintStream reportStream) {

        this.reportStream = reportStream;
        connected = true;

        clientId = mqttClient.getClientId();

    }

    /**
     * @return flag
     */
    public final boolean isReportConnectionLoss() {
        return reportConnectionLoss;
    }

    /**
     * @param reportConnectionLoss
     */
    public final void setReportConnectionLoss(boolean reportConnectionLoss) {
        this.reportConnectionLoss = reportConnectionLoss;
    }

    /**
     * @param waitMilliseconds
     * @return message
     * @throws InterruptedException
     */
    public ReceivedMessage receiveNext(long waitMilliseconds)
            throws InterruptedException {
        final String methodName = "receiveNext";

        ReceivedMessage receivedMessage = null;


        if (receivedMessages.isEmpty()) {
//      wait(waitMilliseconds);
            TimeUnit.MILLISECONDS.sleep(waitMilliseconds);

        }

        Log.i(methodName, "receiveNext time is "+new Date().toString());
        Log.i(methodName, "receivedMessages = "+receivedMessages.toString());


        if (!receivedMessages.isEmpty()) {

            Log.i(methodName, "MqttV3Receiver receive message");
            receivedMessage = receivedMessages.remove(0);
        }

        return receivedMessage;
    }

    /**
     * @param sendTopic
     * @param expectedQos
     * @param sentBytes
     * @return flag
     * @throws MqttException
     * @throws InterruptedException
     */
    public boolean validateReceipt(String sendTopic, int expectedQos,
                                   byte[] sentBytes) throws MqttException, InterruptedException {
        final String methodName = "validateReceipt";

        long waitMilliseconds = 10000;
        ReceivedMessage receivedMessage = receiveNext(waitMilliseconds);
        if (receivedMessage == null) {
            report(" No message received in waitMilliseconds="
                    + waitMilliseconds);

            return false;
        }

        if (!sendTopic.equals(receivedMessage.topic)) {
            report(" Received invalid topic sent=" + sendTopic
                    + " received topic=" + receivedMessage.topic);

            return false;
        }

        if (expectedQos != receivedMessage.message.getQos()) {
            report("expectedQos=" + expectedQos + " != Received Qos="
                    + receivedMessage.message.getQos());

            return false;
        }

        if (!java.util.Arrays.equals(sentBytes,
                receivedMessage.message.getPayload())) {
            report("Received invalid payload="
                    + Arrays.toString(receivedMessage.message.getPayload()) + "\n" + "Sent:"
                    + new String(sentBytes) + "\n" + "Received:"
                    + new String(receivedMessage.message.getPayload()));

            return false;
        }

        return true;
    }

    /**
     * Validate receipt of a batch of messages sent to a topic by a number of
     * publishers The message payloads are expected to have the format<b>
     * "Batch Message payload :<batch>:<publisher>:<messageNumber>:<any additional payload>"
     *
     * We want to detect excess messages, so we don't just handle a certain
     * number. Instead we wait for a timeout period, and exit if no message is
     * received in that period.<b> The timeout period can make this test long
     * running, so we attempt to dynamically adjust, allowing 10 seconds for the
     * first message and then averaging the time taken to receive messages and
     * applying some fudge factors.
     *
     * @param sendTopics
     * @param expectedQosList
     * @param nPublishers
     * @param expectedBatchNumber
     * @param sentBytes
     * @param expectOrdered
     * @return flag
     * @throws MqttException
     * @throws InterruptedException
     */
    public boolean validateReceipt(List<String> sendTopics, List<Integer> expectedQosList,
                                   int expectedBatchNumber, int nPublishers, List<byte[]> sentBytes,
                                   boolean expectOrdered) throws MqttException, InterruptedException {
        final String methodName = "validateReceipt";

        int expectedMessageNumbers[] = new int[nPublishers];
        for (int i = 0; i < nPublishers; i++) {
            expectedMessageNumbers[i] = 0;
        }
        long waitMilliseconds = 10000;

        // track time taken to receive messages
        long totWait = 0;
        int messageNo = 0;
        while (true) {
            long startWait = System.currentTimeMillis();
            ReceivedMessage receivedMessage = receiveNext(waitMilliseconds);
            if (receivedMessage == null) {
                break;
            }
            messageNo++;
            totWait += (System.currentTimeMillis() - startWait);

            // Calculate new wait time based on experience, but not allowing it
            // to get too small
            waitMilliseconds = Math.max(totWait / messageNo, 500);

            byte[] payload = receivedMessage.message.getPayload();
            String payloadString = new String(payload);
            if (!payloadString.startsWith("Batch Message payload :")) {
                report("Received invalid payload\n" + "Received:"
                        + payloadString);
                report("Payload did not start with {"
                        + "Batch Message payload :" + "}");

                return false;
            }

            String[] payloadParts = payloadString.split(":");
            if (payloadParts.length != 5) {
                report("Received invalid payload\n" + "Received:"
                        + payloadString);
                report("Payload was not of expected format");
                return false;
            }

            try {
                int batchNumber = Integer.parseInt(payloadParts[1]);
                if (batchNumber != expectedBatchNumber) {
                    report("Received invalid payload\n" + "Received:"
                            + payloadString);
                    report("batchnumber" + batchNumber
                            + " was not the expected value "
                            + expectedBatchNumber);

                    return false;
                }
            }
            catch (NumberFormatException e) {
                report("Received invalid payload\n" + "Received:"
                        + payloadString);
                report("batchnumber was not a numeric value");

                return false;
            }

            int publisher = -1;
            try {
                publisher = Integer.parseInt(payloadParts[2]);
                if ((publisher < 0) || (publisher >= nPublishers)) {
                    report("Received invalid payload\n" + "Received:"
                            + payloadString);
                    report("publisher " + publisher
                            + " was not in the range 0 - " + (nPublishers - 1));

                    return false;
                }
            }
            catch (NumberFormatException e) {
                report("Received invalid payload\n" + "Received:"
                        + payloadString);
                report("publisher was not a numeric value");

                return false;
            }

            if (expectOrdered) {
                try {
                    int messageNumber = Integer.parseInt(payloadParts[3]);
                    if (messageNumber == expectedMessageNumbers[publisher]) {
                        expectedMessageNumbers[publisher] += 1;
                    }
                    else {
                        report("Received invalid payload\n" + "Received:"
                                + payloadString);
                        report("messageNumber "
                                + messageNumber
                                + " was received out of sequence - expected value was "
                                + expectedMessageNumbers[publisher]);

                        return false;
                    }
                }
                catch (NumberFormatException e) {
                    report("Received invalid payload\n" + "Received:"
                            + payloadString);
                    report("messageNumber was not a numeric value");

                    return false;
                }
            }

            int location;
            for (location = 0; location < sentBytes.size(); location++) {
                if (Arrays.equals(payload, sentBytes.get(location))) {
                    break;
                }
            }

            String sendTopic = null;
            int expectedQos = -1;
            if (location < sentBytes.size()) {
                sentBytes.remove(location);
                sendTopic = sendTopics.remove(location);
                expectedQos = expectedQosList.remove(location);
            }
            else {
                report("Received invalid payload\n" + "Received:"
                        + payloadString);
                for (byte[] expectedPayload : sentBytes) {
                    report("\texpected message :" + new String(expectedPayload));
                }

                return false;
            }

            if (!sendTopic.equals(receivedMessage.topic)) {
                report(" Received invalid topic sent=" + sendTopic
                        + " received topic=" + receivedMessage.topic);

                return false;
            }

            if (expectedQos != receivedMessage.message.getQos()) {
                report("expectedQos=" + expectedQos + " != Received Qos="
                        + receivedMessage.message.getQos());

                return false;
            }

        }

        if (!sentBytes.isEmpty()) {
            for (byte[] missedPayload : sentBytes) {
                report("Did not receive message \n" + new String(missedPayload));
            }

            return false;
        }

        return true;
    }

    /**
     * @param waitMilliseconds
     * @return flag
     * @throws InterruptedException
     */
    public synchronized boolean waitForConnectionLost(long waitMilliseconds)
            throws InterruptedException {
        final String methodName = "waitForConnectionLost";

        if (connected) {
            wait(waitMilliseconds);
        }

        return connected;
    }

    /**
     * @param cause
     */
    public void connectionLost(Throwable cause) {
        final String methodName = "connectionLost";

        if (reportConnectionLoss) {
            report("ConnectionLost: clientId=" + clientId + " cause=" + cause);
        }

        synchronized (this) {
            connected = false;
            notifyAll();
        }

    }

    /**
     * @param arg0
     */
    public void deliveryComplete(IMqttDeliveryToken arg0) {
        // Auto-generated method stub
    }

    /**
     * @param arg0
     * @param arg1
     */
    public void deliveryFailed(IMqttDeliveryToken arg0, MqttException arg1) {
        // Auto-generated method stub
    }

    /**
     * @param topic
     * @param message
     * @throws Exception
     */
    public void messageArrived(String topic, MqttMessage message)
            throws Exception {
        final String methodName = "messageArrived";
        Log.i(methodName, "messageArrived "+topic+ " = "+message.toString()+ " clientId = "+this.clientId);
        Log.i(methodName, "messageArrived "+new Date().toString());

        receivedMessages.add(new ReceivedMessage(topic, message));
        Log.i(methodName, "receivedMessages = "+receivedMessages.toString());

        // notify();
    }

    /**
     * @param text
     *            to be written to the report.
     */
    public void report(String text) {
        Log.e(this.getClass().getCanonicalName(), text);
    }
}
