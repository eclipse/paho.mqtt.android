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
package org.eclipse.paho.android;

import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import android.content.Intent;
import android.os.IBinder;
import android.test.ServiceTestCase;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;

/**
 * @author Rhys
 *
 */
public class AndroidServiceWithActionListenerTest extends ServiceTestCase {

    private String serverURI;
    private String mqttSSLServerURI;

    private int waitForCompletionTime;

    private String keyStorePwd;

    //since we know tokens do not work when an action listener isn't specified
    private TestCaseNotifier notifier = new TestCaseNotifier();

    private final String classCanonicalName = this.getClass().getCanonicalName();


    public AndroidServiceWithActionListenerTest() {
        //noinspection unchecked
        super(org.eclipse.paho.android.service.MqttService.class);
    }

    @Override
    protected void setUp() throws Exception {

        super.setUp();
        Intent intent = new Intent();
        intent.setClassName("org.eclipse.paho.android.service", "MqttService");
        IBinder binder = bindService(intent);

        TestProperties properties = new TestProperties(this.getContext());
        serverURI = properties.getServerURI();
        mqttSSLServerURI = properties.getServerSSLURI();
        waitForCompletionTime = properties.getWaitForCompletionTime();
        String clientKeyStore = properties.getClientKeyStore();
        keyStorePwd = properties.getClientKeyStorePassword();

    }

    public void testConnect() throws Throwable {

        IMqttAsyncClient mqttClient = null;
        mqttClient = new MqttAndroidClient(mContext, serverURI, "testConnect");

        IMqttToken connectToken = null;
        IMqttToken disconnectToken = null;

        connectToken = mqttClient.connect(null, new ActionListener(notifier));
        notifier.waitForCompletion(waitForCompletionTime);

        disconnectToken = mqttClient.disconnect(null, new ActionListener(notifier));
        notifier.waitForCompletion(waitForCompletionTime);

        connectToken = mqttClient.connect(null, new ActionListener(notifier));
        notifier.waitForCompletion(waitForCompletionTime);

        disconnectToken = mqttClient.disconnect(null, new ActionListener(notifier));
        notifier.waitForCompletion(waitForCompletionTime);

    }

    public void testRemoteConnect() throws Throwable {
        String methodName = "testRemoteConnect";
        IMqttAsyncClient mqttClient = null;

        mqttClient = new MqttAndroidClient(mContext, serverURI, "testRemoteConnect");
        IMqttToken connectToken = null;
        IMqttToken subToken = null;
        IMqttDeliveryToken pubToken = null;
        IMqttToken disconnectToken = null;

        connectToken = mqttClient.connect(null, new ActionListener(notifier));
        notifier.waitForCompletion(waitForCompletionTime);

        disconnectToken = mqttClient.disconnect(null, new ActionListener(notifier));
        notifier.waitForCompletion(waitForCompletionTime);

        MqttV3Receiver mqttV3Receiver = new MqttV3Receiver(mqttClient,
                null);
        mqttClient.setCallback(mqttV3Receiver);

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setCleanSession(false);

        connectToken = mqttClient.connect(mqttConnectOptions, null, new ActionListener(notifier));
        notifier.waitForCompletion(waitForCompletionTime);

        String[] topicNames = new String[]{methodName + "/Topic"};
        int[] topicQos = {0};
        subToken = mqttClient.subscribe(topicNames, topicQos, null, new ActionListener(notifier));
        notifier.waitForCompletion(waitForCompletionTime);

        byte[] payload = ("Message payload " + classCanonicalName + "." + methodName)
                .getBytes();
        pubToken = mqttClient.publish(topicNames[0], payload, 1, false, null, new ActionListener(notifier));
        notifier.waitForCompletion(waitForCompletionTime);

        boolean ok = mqttV3Receiver.validateReceipt(topicNames[0], 0,
                payload);
        if (!ok) {
            Assert.fail("Receive failed");
        }

        disconnectToken = mqttClient.disconnect(null, new ActionListener(notifier));
        notifier.waitForCompletion(waitForCompletionTime);

    }

    public void testLargeMessage() throws Throwable {
        notifier = new TestCaseNotifier();
        String methodName = "testLargeMessage";
        IMqttAsyncClient mqttClient = null;
        try {
            mqttClient = new MqttAndroidClient(mContext, serverURI,
                    "testLargeMessage");
            IMqttToken connectToken;
            IMqttToken subToken;
            IMqttToken unsubToken;
            IMqttDeliveryToken pubToken;

            MqttV3Receiver mqttV3Receiver = new MqttV3Receiver(mqttClient, null); //TODO do something about this?
            mqttClient.setCallback(mqttV3Receiver);

            connectToken = mqttClient.connect(null, new ActionListener(notifier));
            notifier.waitForCompletion(waitForCompletionTime);

            int largeSize = 1000;
            String[] topicNames = new String[]{"testLargeMessage" + "/Topic"};
            int[] topicQos = {0};
            byte[] message = new byte[largeSize];

            java.util.Arrays.fill(message, (byte) 's');

            subToken = mqttClient.subscribe(topicNames, topicQos, null, new ActionListener(notifier));
            notifier.waitForCompletion(waitForCompletionTime);

            unsubToken = mqttClient.unsubscribe(topicNames, null, new ActionListener(notifier));
            notifier.waitForCompletion(waitForCompletionTime);

            subToken = mqttClient.subscribe(topicNames, topicQos, null, new ActionListener(notifier));
            notifier.waitForCompletion(waitForCompletionTime);

            pubToken = mqttClient.publish(topicNames[0], message, 0, false, null, new ActionListener(notifier));
            notifier.waitForCompletion(waitForCompletionTime);

            boolean ok = mqttV3Receiver.validateReceipt(topicNames[0], 0,
                    message);
            if (!ok) {
                Assert.fail("Receive failed");
            }

        }
        catch (Exception exception) {
            Assert.fail("Failed to instantiate:" + methodName + " exception="
                    + exception);
        }
        finally {
            try {
                IMqttToken disconnectToken;
                disconnectToken = mqttClient.disconnect(null, new ActionListener(notifier));
                notifier.waitForCompletion(waitForCompletionTime);

                mqttClient.close();
            }
            catch (Exception ignored) {

            }
        }

    }

    public void testMultipleClients() throws Throwable {

        int publishers = 2;
        int subscribers = 5;
        String methodName = "testMultipleClients";
        IMqttAsyncClient[] mqttPublisher = new IMqttAsyncClient[publishers];
        IMqttAsyncClient[] mqttSubscriber = new IMqttAsyncClient[subscribers];

        IMqttToken connectToken;
        IMqttToken subToken;
        IMqttDeliveryToken pubToken;
        IMqttToken disconnectToken;

        String[] topicNames = new String[]{methodName + "/Topic"};
        int[] topicQos = {0};

        for (int i = 0; i < mqttPublisher.length; i++) {
            mqttPublisher[i] = new MqttAndroidClient(mContext,
                    serverURI, "MultiPub" + i);

            connectToken = mqttPublisher[i].connect(null, new ActionListener(notifier));
            notifier.waitForCompletion(waitForCompletionTime);
        } // for...

        MqttV3Receiver[] mqttV3Receiver = new MqttV3Receiver[mqttSubscriber.length];
        for (int i = 0; i < mqttSubscriber.length; i++) {
            mqttSubscriber[i] = new MqttAndroidClient(mContext,
                    serverURI, "MultiSubscriber" + i);
            mqttV3Receiver[i] = new MqttV3Receiver(mqttSubscriber[i],
                    null);
            mqttSubscriber[i].setCallback(mqttV3Receiver[i]);

            connectToken = mqttSubscriber[i].connect(null, new ActionListener(notifier));
            notifier.waitForCompletion(waitForCompletionTime);

            subToken = mqttSubscriber[i].subscribe(topicNames, topicQos, null, new ActionListener(notifier));
            notifier.waitForCompletion(waitForCompletionTime);
        } // for...

        for (int iMessage = 0; iMessage < 2; iMessage++) {
            byte[] payload = ("Message " + iMessage).getBytes();
            for (IMqttAsyncClient aMqttPublisher : mqttPublisher) {
                pubToken = aMqttPublisher.publish(topicNames[0], payload, 0, false,
                        null, new ActionListener(notifier));
                notifier.waitForCompletion(waitForCompletionTime);
            }

            TimeUnit.MILLISECONDS.sleep(30000);

            for (int i = 0; i < mqttSubscriber.length; i++) {
                for (IMqttAsyncClient aMqttPublisher : mqttPublisher) {
                    boolean ok = mqttV3Receiver[i].validateReceipt(
                            topicNames[0], 0, payload);
                    if (!ok) {
                        Assert.fail("Receive failed");
                    }
                } // for publishers...
            } // for subscribers...
        } // for messages...


        for (IMqttAsyncClient aMqttPublisher : mqttPublisher) {
            disconnectToken = aMqttPublisher.disconnect(null, null);
            disconnectToken.waitForCompletion(waitForCompletionTime);
            aMqttPublisher.close();
        }
        for (IMqttAsyncClient aMqttSubscriber : mqttSubscriber) {
            disconnectToken = aMqttSubscriber.disconnect(null, null);
            disconnectToken.waitForCompletion(waitForCompletionTime);
            aMqttSubscriber.close();
        }

    }

//  public void testNonDurableSubs() throws Throwable {
//    String methodName = "testNonDurableSubs";
//    notifier = new TestCaseNotifier();
//    IMqttAsyncClient mqttClient = null;
//
//    IMqttToken connectToken;
//    IMqttToken subToken;
//    IMqttToken unsubToken;
//    IMqttDeliveryToken pubToken;
//    IMqttToken disconnectToken;
//
//    mqttClient = new MqttAndroidClient(mContext, serverURI,
//        "testNonDurableSubs");
//    MqttV3Receiver mqttV3Receiver = new MqttV3Receiver(mqttClient,
//        null);
//    mqttClient.setCallback(mqttV3Receiver);
//    MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
//    // Clean session true is the default and implies non durable
//    // subscriptions.
//    mqttConnectOptions.setCleanSession(true);
//    connectToken = mqttClient.connect(mqttConnectOptions, null, new ActionListener(notifier));
//    notifier.waitForCompletion(1000);
//
//    String[] topicNames = new String[]{methodName + "/Topic"};
//    int[] topicQos = {2};
//    subToken = mqttClient.subscribe(topicNames, topicQos, null, new ActionListener(notifier));
//    notifier.waitForCompletion(1000);
//
//    byte[] payloadNotRetained = ("Message payload "
//                                 + classCanonicalName + "." + methodName + " not retained")
//        .getBytes();
//    pubToken = mqttClient.publish(topicNames[0], payloadNotRetained, 2, false,
//        null, new ActionListener(notifier));
//    notifier.waitForCompletion(1000);
//
//    boolean ok = mqttV3Receiver.validateReceipt(topicNames[0], 2,
//        payloadNotRetained);
//    if (!ok) {
//      Assert.fail("Receive failed");
//    }
//
//    // Retained publications.
//    // ----------------------
//    byte[] payloadRetained = ("Message payload " + classCanonicalName
//                              + "." + methodName + " retained").getBytes();
//    pubToken = mqttClient.publish(topicNames[0], payloadRetained, 2, true,
//        null, new ActionListener(notifier));
//    notifier.waitForCompletion(1000);
//
//    ok = mqttV3Receiver.validateReceipt(topicNames[0], 2,
//        payloadRetained);
//    if (!ok) {
//      Assert.fail("Receive failed");
//    }
//
//    // Check that unsubscribe and re subscribe resends the publication.
//    unsubToken = mqttClient.unsubscribe(topicNames, null, new ActionListener(notifier));
//    notifier.waitForCompletion(1000);
//
//    subToken = mqttClient.subscribe(topicNames, topicQos, null, new ActionListener(notifier));
//    notifier.waitForCompletion(1000);
//
//    ok = mqttV3Receiver.validateReceipt(topicNames[0], 2,
//        payloadRetained);
//    if (!ok) {
//      Assert.fail("Receive failed");
//    }
//
//    // Check that subscribe without unsubscribe receives the
//    // publication.
//    subToken = mqttClient.subscribe(topicNames, topicQos, null, new ActionListener(notifier));
//    notifier.waitForCompletion(1000);
//    ok = mqttV3Receiver.validateReceipt(topicNames[0], 2,
//        payloadRetained);
//    if (!ok) {
//      Assert.fail("Receive failed");
//    }
//
//    // Disconnect, reconnect and check that the retained publication is
//    // still delivered.
//    disconnectToken = mqttClient.disconnect(null, new ActionListener(notifier));
//    notifier.waitForCompletion(1000);
//
//    mqttClient.close();
//
//    mqttClient = new MqttAndroidClient(mContext, serverURI,
//        "testNonDurableSubs");
//
//    mqttV3Receiver = new MqttV3Receiver(mqttClient,
//        null);
//    mqttClient.setCallback(mqttV3Receiver);
//
//    mqttConnectOptions = new MqttConnectOptions();
//    mqttConnectOptions.setCleanSession(true);
//    connectToken = mqttClient.connect(mqttConnectOptions, null, new ActionListener(notifier));
//    connectToken.waitForCompletion(1000);
//
//    subToken = mqttClient.subscribe(topicNames, topicQos, null, new ActionListener(notifier));
//    notifier.waitForCompletion(1000);
//
//    ok = mqttV3Receiver.validateReceipt(topicNames[0], 2,
//        payloadRetained);
//    if (!ok) {
//      Assert.fail("Receive failed");
//    }
//
//    disconnectToken = mqttClient.disconnect(null, new ActionListener(notifier));
//    notifier.waitForCompletion(1000);
//
//    mqttClient.close();
//
//  }

    public void testQoSPreserved() throws Throwable {

        IMqttAsyncClient mqttClient = null;
        IMqttToken connectToken;
        IMqttToken subToken;
        IMqttDeliveryToken pubToken;
        IMqttToken disconnectToken;
        String methodName = "testQoSPreserved";

        mqttClient = new MqttAndroidClient(mContext, serverURI, "testQoSPreserved");
        MqttV3Receiver mqttV3Receiver = new MqttV3Receiver(mqttClient,
                null);
        mqttClient.setCallback(mqttV3Receiver);

        connectToken = mqttClient.connect(null, new ActionListener(notifier));
        notifier.waitForCompletion(waitForCompletionTime);

        String[] topicNames = new String[]{methodName + "/Topic0",
                methodName + "/Topic1", methodName + "/Topic2"};
        int[] topicQos = {0, 1, 2};
        subToken = mqttClient.subscribe(topicNames, topicQos, null, new ActionListener(notifier));
        notifier.waitForCompletion(waitForCompletionTime);

        for (int i = 0; i < topicNames.length; i++) {
            byte[] message = ("Message payload " + classCanonicalName + "."
                    + methodName + " " + topicNames[i]).getBytes();
            for (int iQos = 0; iQos < 3; iQos++) {
                pubToken = mqttClient.publish(topicNames[i], message, iQos, false,
                        null, null);
                notifier.waitForCompletion(waitForCompletionTime);

                boolean ok = mqttV3Receiver.validateReceipt(topicNames[i],
                        Math.min(iQos, topicQos[i]), message);
                if (!ok) {
                    Assert.fail("Receive failed sub Qos=" + topicQos[i]
                            + " PublishQos=" + iQos);
                }
            }
        }

        disconnectToken = mqttClient.disconnect(null, new ActionListener(notifier));
        notifier.waitForCompletion(waitForCompletionTime);

    }


    public void testHAConnect() throws Throwable{

        String methodName = "testHAConnect";

        IMqttAsyncClient client = null;
        try {
            try {
                String junk = "tcp://junk:123";
                client = new MqttAndroidClient(mContext, junk, methodName);

                String[] urls = new String[]{"tcp://junk", serverURI};

                MqttConnectOptions options = new MqttConnectOptions();
                options.setServerURIs(urls);

                Log.i(methodName,"HA connect");
                IMqttToken connectToken = client.connect(options, null, new ActionListener(notifier));
                notifier.waitForCompletion(waitForCompletionTime);

                Log.i(methodName,"HA disconnect");
                IMqttToken disconnectToken = client.disconnect(null, new ActionListener(notifier));
                notifier.waitForCompletion(waitForCompletionTime);

                Log.i(methodName,"HA success");
            }
            catch (Exception e) {

                e.printStackTrace();
                throw e;
            }
        }
        finally {
            if (client != null) {
                client.close();
            }
        }
    }

    public void testPubSub() throws Throwable{

        String methodName = "testPubSub";
        IMqttAsyncClient mqttClient = null;
        try {
            mqttClient = new MqttAndroidClient(mContext, serverURI, methodName);
            IMqttToken connectToken;
            IMqttToken subToken;
            IMqttDeliveryToken pubToken;

            MqttV3Receiver mqttV3Receiver = new MqttV3Receiver(mqttClient, null);
            mqttClient.setCallback(mqttV3Receiver);

            connectToken = mqttClient.connect(null, new ActionListener(notifier));
            notifier.waitForCompletion(waitForCompletionTime);

            String[] topicNames = new String[]{"testPubSub" + "/Topic"};
            int[] topicQos = {0};
            MqttMessage mqttMessage = new MqttMessage("message for testPubSub".getBytes());
            byte[] message = mqttMessage.getPayload();

            subToken = mqttClient.subscribe(topicNames, topicQos, null, new ActionListener(notifier));
            notifier.waitForCompletion(waitForCompletionTime);

            pubToken = mqttClient.publish(topicNames[0], message, 0, false, null, new ActionListener(notifier));
            notifier.waitForCompletion(waitForCompletionTime);

            TimeUnit.MILLISECONDS.sleep(3000);

            boolean ok = mqttV3Receiver.validateReceipt(topicNames[0], 0, message);
            if (!ok) {
                Assert.fail("Receive failed");
            }

        }
        catch (Exception exception) {
            Assert.fail("Failed to instantiate:" + methodName + " exception="
                    + exception);
        }
        finally {
            try {
                IMqttToken disconnectToken;
                disconnectToken = mqttClient.disconnect(null, new ActionListener(notifier));
                notifier.waitForCompletion(waitForCompletionTime);

                mqttClient.close();
            }
            catch (Exception ignored) {

            }
        }

    }


    public void testRetainedMessage() throws Throwable{

        String methodName = "testRetainedMessage";
        IMqttAsyncClient mqttClient = null;
        IMqttAsyncClient mqttClientRetained = null;
        IMqttToken disconnectToken = null;

        try {
            mqttClient = new MqttAndroidClient(mContext, serverURI, methodName);
            IMqttToken connectToken;
            IMqttToken subToken;
            IMqttDeliveryToken pubToken;

            MqttV3Receiver mqttV3Receiver = new MqttV3Receiver(mqttClient, null);
            mqttClient.setCallback(mqttV3Receiver);

            connectToken = mqttClient.connect(null, new ActionListener(notifier));
            notifier.waitForCompletion(waitForCompletionTime);

            String[] topicNames = new String[]{"testRetainedMessage" + "/Topic"};
            int[] topicQos = {0};
            MqttMessage mqttMessage = new MqttMessage("message for testPubSub".getBytes());
            byte[] message = mqttMessage.getPayload();

            subToken = mqttClient.subscribe(topicNames, topicQos, null, new ActionListener(notifier));
            notifier.waitForCompletion(waitForCompletionTime);

            pubToken = mqttClient.publish(topicNames[0], message, 0, true, null, new ActionListener(notifier));
            notifier.waitForCompletion(waitForCompletionTime);

            TimeUnit.MILLISECONDS.sleep(3000);

            boolean ok = mqttV3Receiver.validateReceipt(topicNames[0], 0, message);
            if (!ok) {
                Assert.fail("Receive failed");
            }

            Log.i(methodName, "First client received message successfully");

            disconnectToken = mqttClient.disconnect(null, new ActionListener(notifier));
            notifier.waitForCompletion(waitForCompletionTime);
            mqttClient.close();

            mqttClientRetained = new MqttAndroidClient(mContext, serverURI, "Retained");

            Log.i(methodName, "New MqttAndroidClient mqttClientRetained");

            MqttV3Receiver mqttV3ReceiverRetained = new MqttV3Receiver(mqttClientRetained, null);
            mqttClientRetained.setCallback(mqttV3ReceiverRetained);

            Log.i(methodName, "Assigning callback...");

            connectToken = mqttClientRetained.connect(null, new ActionListener(notifier));
            notifier.waitForCompletion(waitForCompletionTime);

            Log.i(methodName, "Connect to mqtt server");

            subToken = mqttClientRetained.subscribe(topicNames, topicQos, null, new ActionListener(notifier));
            notifier.waitForCompletion(waitForCompletionTime);

            Log.i(methodName, "subscribe "+ topicNames[0] + " QoS is " + topicQos[0]);

            TimeUnit.MILLISECONDS.sleep(3000);

            ok = mqttV3ReceiverRetained.validateReceipt(topicNames[0], 0, message);
            if (!ok) {
                Assert.fail("Receive retained message failed");
            }

            Log.i(methodName, "Second client received message successfully");

            disconnectToken = mqttClientRetained.disconnect(null, new ActionListener(notifier));
            notifier.waitForCompletion(waitForCompletionTime);
            mqttClientRetained.close();

        }
        catch (Exception exception) {
            Assert.fail("Failed to instantiate:" + methodName + " exception="
                    + exception);
        }

    }

    /**
     * Tests that a client can be constructed and that it can connect to and
     * disconnect from the service via SSL
     *
     * @throws Exception
     */

    public void testSSLConnect() throws Exception {

        MqttAndroidClient mqttClient = null;
        try {
            mqttClient = new MqttAndroidClient(mContext, mqttSSLServerURI, "testSSLConnect");

            MqttConnectOptions options = new MqttConnectOptions();
            options.setSocketFactory(mqttClient.getSSLSocketFactory(this.getContext().getAssets().open("test.bks"),keyStorePwd));


            IMqttToken connectToken = null;
            IMqttToken disconnectToken = null;

            connectToken = mqttClient.connect(options, this.getContext(), new ActionListener(notifier));
            connectToken.waitForCompletion(waitForCompletionTime);

            disconnectToken = mqttClient.disconnect(null, new ActionListener(notifier));
            disconnectToken.waitForCompletion(waitForCompletionTime);

            connectToken = mqttClient.connect(options, this.getContext(), new ActionListener(notifier));
            connectToken.waitForCompletion(waitForCompletionTime);

            disconnectToken = mqttClient.disconnect(null, new ActionListener(notifier));
            disconnectToken.waitForCompletion(waitForCompletionTime);
        }
        catch (Exception exception) {
            Assert.fail("Failed:" + "testSSLConnect" + " exception=" + exception);
        }
        finally {
            if (mqttClient != null) {
                mqttClient.close();
            }
        }

    }


    /**
     * An SSL connection with server cert authentication, simple pub/sub of an message
     *
     * @throws Exception
     */

    public void testSSLPubSub() throws Exception {

        MqttAndroidClient mqttClient = null;


        IMqttToken connectToken = null;
        IMqttToken disconnectToken = null;
        IMqttToken subToken = null;
        IMqttDeliveryToken pubToken = null;

        try {
            mqttClient = new MqttAndroidClient(mContext, mqttSSLServerURI, "testSSLPubSub");

            MqttConnectOptions options = new MqttConnectOptions();
            options.setSocketFactory(mqttClient.getSSLSocketFactory(this.getContext().getAssets().open("test.bks"),keyStorePwd));


            MqttV3Receiver mqttV3Receiver = new MqttV3Receiver(mqttClient, null);
            mqttClient.setCallback(mqttV3Receiver);

            connectToken = mqttClient.connect(options,this.getContext(),new ActionListener(notifier));
            connectToken.waitForCompletion(waitForCompletionTime);

            String[] topicNames = new String[]{"testSSLPubSub"+"/Topic"};
            int[] topicQos = {0};
            MqttMessage mqttMessage = new MqttMessage(("message for testSSLPubSub").getBytes());
            byte[] message = mqttMessage.getPayload();

            subToken = mqttClient.subscribe(topicNames, topicQos, null, new ActionListener(notifier));
            subToken.waitForCompletion(waitForCompletionTime);

            pubToken = mqttClient.publish(topicNames[0], message, 0, false, null, new ActionListener(notifier));
            pubToken.waitForCompletion(waitForCompletionTime);

            TimeUnit.MILLISECONDS.sleep(6000);

            boolean ok = mqttV3Receiver.validateReceipt(topicNames[0], 0, message);
            if (!ok) {
                Assert.fail("Receive failed");
            }

        }
        catch (Exception exception) {
            Assert.fail("Failed:" + "testSSLPubSub" + " exception=" + exception);
        }
        finally {

            disconnectToken = mqttClient.disconnect(null, new ActionListener(notifier));
            disconnectToken.waitForCompletion(waitForCompletionTime);

            if (mqttClient != null) {
                mqttClient.close();
            }
        }

    }


    private class ActionListener implements IMqttActionListener {

        private TestCaseNotifier notifier = null;

        public ActionListener(TestCaseNotifier notifier) {
            this.notifier = notifier;
        }

        /* (non-Javadoc)
         * @see org.eclipse.paho.client.mqttv3.IMqttActionListener#onFailure(org.eclipse.paho.client.mqttv3.IMqttToken, java.lang.Throwable)
         */
        public void onFailure(IMqttToken token, Throwable exception) {
            notifier.storeException(exception);
            synchronized (notifier) {
                notifier.notifyAll();
            }

        }

        /* (non-Javadoc)
         * @see org.eclipse.paho.client.mqttv3.IMqttActionListener#onSuccess(org.eclipse.paho.client.mqttv3.IMqttToken)
         */
        public void onSuccess(IMqttToken token) {
            synchronized (notifier) {
                notifier.notifyAll();
            }

        }

    }

}
