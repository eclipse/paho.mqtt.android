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

import org.eclipse.paho.client.mqttv3.MqttMessage;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * <p>
 * A way to flow MqttMessages via Bundles/Intents
 * </p>
 * 
 * <p>
 * An application will probably use this only when receiving a message from a
 * Service in a Bundle - the necessary code will be something like this :-
 * </p>
 * <pre>
 * <code>
 * 	private void messageArrivedAction(Bundle data) {
 * 		ParcelableMqttMessage message = (ParcelableMqttMessage) data
 * 			.getParcelable(MqttServiceConstants.CALLBACK_MESSAGE_PARCEL);
 *		<i>Use the normal {@link MqttMessage} methods on the the message object.</i>
 * 	}
 * 
 * </code>
 * </pre>
 *
 * <p>
 * It is unlikely that an application will directly use the methods which are
 * specific to this class.
 * </p>
 */

public class ParcelableMqttMessage extends MqttMessage implements Parcelable {

  String messageId = null;

  ParcelableMqttMessage(MqttMessage original) {
    super(original.getPayload());
    setQos(original.getQos());
    setRetained(original.isRetained());
    setDuplicate(original.isDuplicate());
  }

  ParcelableMqttMessage(Parcel parcel) {
    super(parcel.createByteArray());
    setQos(parcel.readInt());
    boolean[] flags = parcel.createBooleanArray();
    setRetained(flags[0]);
    setDuplicate(flags[1]);
    messageId = parcel.readString();
  }

  /**
   * @return the messageId
   */
  public String getMessageId() {
    return messageId;
  }

  /**
   * Describes the contents of this object
   */
  @Override
  public int describeContents() {
    return 0;
  }

  /**
   * Writes the contents of this object to a parcel
   * 
   * @param parcel
   *            The parcel to write the data to.
   * @param flags
   *            this parameter is ignored
   */
  @Override
  public void writeToParcel(Parcel parcel, int flags) {
    parcel.writeByteArray(getPayload());
    parcel.writeInt(getQos());
    parcel.writeBooleanArray(new boolean[]{isRetained(), isDuplicate()});
    parcel.writeString(messageId);
  }

	/**
	 * A creator which creates the message object from a parcel
	 */
	public static final Parcelable.Creator<ParcelableMqttMessage> CREATOR = new Parcelable.Creator<ParcelableMqttMessage>() {

		/**
		 * Creates a message from the parcel object
		 */
		@Override
		public ParcelableMqttMessage createFromParcel(Parcel parcel) {
			return new ParcelableMqttMessage(parcel);
		}

		/**
		 * creates an array of type {@link ParcelableMqttMessage}[]
		 * 
		 */
		@Override
		public ParcelableMqttMessage[] newArray(int size) {
			return new ParcelableMqttMessage[size];
		}
	};
}
