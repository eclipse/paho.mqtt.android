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

import java.util.Iterator;

import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * <p>
 * Mechanism for persisting messages until we know they have been received
 * </p>
 * <ul>
 * <li>A Service should store messages as they arrive via
 * {@link #storeArrived(String, String, MqttMessage)}.
 * <li>When a message has been passed to the consuming entity,
 * {@link #discardArrived(String, String)} should be called.
 * <li>To recover messages which have not been definitely passed to the
 * consumer, {@link MessageStore#getAllArrivedMessages(String)} is used.
 * <li>When a clean session is started {@link #clearArrivedMessages(String)} is
 * used.
 * </ul>
 */
interface MessageStore {

	/**
	 * External representation of a stored message
	 */
	interface StoredMessage {
		/**
		 * @return the identifier for the message within the store
		 */
		String getMessageId();

		/**
		 * @return the identifier of the client which stored this message
		 */
		String getClientHandle();

		/**
		 * @return the topic on which the message was received
		 */
		String getTopic();

		/**
		 * @return the identifier of the client which stored this message
		 */
		MqttMessage getMessage();
	}

	/**
	 * Store a message and return an identifier for it
	 * 
	 * @param clientHandle
	 *            identifier for the client
	 * @param message
	 *            message to be stored
	 * @return a unique identifier for it
	 */
	String storeArrived(String clientHandle, String Topic,
						MqttMessage message);

	/**
	 * Discard a message - called when we are certain that an arrived message
	 * has reached the application.
	 * 
	 * @param clientHandle
	 *            identifier for the client
	 * @param id
	 *            id of message to be discarded
	 */
	boolean discardArrived(String clientHandle, String id);

	/**
	 * Get all the stored messages, usually for a specific client
	 * 
	 * @param clientHandle
	 *            identifier for the client - if null, then messages for all
	 *            clients are returned
	 */
	Iterator<StoredMessage> getAllArrivedMessages(String clientHandle);

	/**
	 * Discard stored messages, usually for a specific client
	 * 
	 * @param clientHandle
	 *            identifier for the client - if null, then messages for all
	 *            clients are discarded
	 */
	void clearArrivedMessages(String clientHandle);

	void close();
}
