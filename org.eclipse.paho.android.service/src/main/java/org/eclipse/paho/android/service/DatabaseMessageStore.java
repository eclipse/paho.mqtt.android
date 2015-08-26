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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Implementation of the {@link MessageStore} interface, using a SQLite database
 * 
 */
class DatabaseMessageStore implements MessageStore {

	// TAG used for indentify trace data etc.
	private static String TAG = "DatabaseMessageStore";

	// One "private" database column name
	// The other database column names are defined in MqttServiceConstants
	private static final String MTIMESTAMP = "mtimestamp";

	// the name of the table in the database to which we will save messages
	private static final String ARRIVED_MESSAGE_TABLE_NAME = "MqttArrivedMessageTable";

	// the database
	private SQLiteDatabase db = null;

	// a SQLiteOpenHelper specific for this database
	private MQTTDatabaseHelper mqttDb = null;

	// a place to send trace data
	private MqttTraceHandler traceHandler = null;

	/**
	 * We need a SQLiteOpenHelper to handle database creation and updating
	 * 
	 */
	private static class MQTTDatabaseHelper extends SQLiteOpenHelper {
		// TAG used for indentify trace data etc.
		private static String TAG = "MQTTDatabaseHelper";

		private static final String DATABASE_NAME = "mqttAndroidService.db";

		// database version, used to recognise when we need to upgrade
		// (delete and recreate)
		private static final int DATABASE_VERSION = 1;

		// a place to send trace data
		private MqttTraceHandler traceHandler = null;

		/**
		 * Constructor.
		 * 
		 * @param traceHandler
		 * @param context
		 */
		public MQTTDatabaseHelper(MqttTraceHandler traceHandler, Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			this.traceHandler = traceHandler;
		}

		/**
		 * When the database is (re)created, create our table
		 * 
		 * @param database
		 */
		@Override
		public void onCreate(SQLiteDatabase database) {
			String createArrivedTableStatement = "CREATE TABLE "
					+ ARRIVED_MESSAGE_TABLE_NAME + "("
					+ MqttServiceConstants.MESSAGE_ID + " TEXT PRIMARY KEY, "
					+ MqttServiceConstants.CLIENT_HANDLE + " TEXT, "
					+ MqttServiceConstants.DESTINATION_NAME + " TEXT, "
					+ MqttServiceConstants.PAYLOAD + " BLOB, "
					+ MqttServiceConstants.QOS + " INTEGER, "
					+ MqttServiceConstants.RETAINED + " TEXT, "
					+ MqttServiceConstants.DUPLICATE + " TEXT, " + MTIMESTAMP
					+ " INTEGER" + ");";
			traceHandler.traceDebug(TAG, "onCreate {"
					+ createArrivedTableStatement + "}");
			try {
				database.execSQL(createArrivedTableStatement);
				traceHandler.traceDebug(TAG, "created the table");
			} catch (SQLException e) {
				traceHandler.traceException(TAG, "onCreate", e);
				throw e;
			}
		}

		/**
		 * To upgrade the database, drop and recreate our table
		 * 
		 * @param db
		 *            the database
		 * @param oldVersion
		 *            ignored
		 * @param newVersion
		 *            ignored
		 */

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			traceHandler.traceDebug(TAG, "onUpgrade");
			try {
				db.execSQL("DROP TABLE IF EXISTS " + ARRIVED_MESSAGE_TABLE_NAME);
			} catch (SQLException e) {
				traceHandler.traceException(TAG, "onUpgrade", e);
				throw e;
			}
			onCreate(db);
			traceHandler.traceDebug(TAG, "onUpgrade complete");
		}
	}

	/**
	 * Constructor - create a DatabaseMessageStore to store arrived MQTT message
	 * 
	 * @param service
	 *            our parent MqttService
	 * @param context
	 *            a context to use for android calls
	 */
	public DatabaseMessageStore(MqttService service, Context context) {
		this.traceHandler = (MqttTraceHandler) service;

		// Open message database
		mqttDb = new MQTTDatabaseHelper(traceHandler, context);

		// Android documentation suggests that this perhaps
		// could/should be done in another thread, but as the
		// database is only one table, I doubt it matters...
		
		traceHandler.traceDebug(TAG, "DatabaseMessageStore<init> complete");
	}

	/**
	 * Store an MQTT message
	 * 
	 * @param clientHandle
	 *            identifier for the client storing the message
	 * @param topic
	 *            The topic on which the message was published
	 * @param message
	 *            the arrived MQTT message
	 * @return an identifier for the message, so that it can be removed when appropriate
	 */
	@Override
	public String storeArrived(String clientHandle, String topic,
			MqttMessage message) {
		
		db = mqttDb.getWritableDatabase();
		
		traceHandler.traceDebug(TAG, "storeArrived{" + clientHandle + "}, {"
				+ message.toString() + "}");

		byte[] payload = message.getPayload();
		int qos = message.getQos();
		boolean retained = message.isRetained();
		boolean duplicate = message.isDuplicate();

		ContentValues values = new ContentValues();
		String id = java.util.UUID.randomUUID().toString();
		values.put(MqttServiceConstants.MESSAGE_ID, id);
		values.put(MqttServiceConstants.CLIENT_HANDLE, clientHandle);
		values.put(MqttServiceConstants.DESTINATION_NAME, topic);
		values.put(MqttServiceConstants.PAYLOAD, payload);
		values.put(MqttServiceConstants.QOS, qos);
		values.put(MqttServiceConstants.RETAINED, retained);
		values.put(MqttServiceConstants.DUPLICATE, duplicate);
		values.put(MTIMESTAMP, System.currentTimeMillis());
		try {
			db.insertOrThrow(ARRIVED_MESSAGE_TABLE_NAME, null, values);
		} catch (SQLException e) {
			traceHandler.traceException(TAG, "onUpgrade", e);
			throw e;
		}
		int count = getArrivedRowCount(clientHandle);
		traceHandler
				.traceDebug(
						TAG,
						"storeArrived: inserted message with id of {"
								+ id
								+ "} - Number of messages in database for this clientHandle = "
								+ count);
		return id;
	}

	private int getArrivedRowCount(String clientHandle) {
		String[] cols = new String[1];
		cols[0] = "COUNT(*)";
		Cursor c = db.query(ARRIVED_MESSAGE_TABLE_NAME, cols,
				MqttServiceConstants.CLIENT_HANDLE + "='" + clientHandle + "'",
				null, null, null, null);
		int count = 0;
		if (c.moveToFirst()) {
			count = c.getInt(0);
		}
		c.close();
		return count;
	}

	/**
	 * Delete an MQTT message.
	 * 
	 * @param clientHandle
	 *            identifier for the client which stored the message
	 * @param id
	 *            the identifying string returned when the message was stored
	 * 
	 * @return true if the message was found and deleted
	 */
	@Override
	public boolean discardArrived(String clientHandle, String id) {
		
		db = mqttDb.getWritableDatabase();
		
		traceHandler.traceDebug(TAG, "discardArrived{" + clientHandle + "}, {"
				+ id + "}");
		int rows;
		try {
			rows = db.delete(ARRIVED_MESSAGE_TABLE_NAME,
					MqttServiceConstants.MESSAGE_ID + "='" + id + "' AND "
							+ MqttServiceConstants.CLIENT_HANDLE + "='"
							+ clientHandle + "'", null);
		} catch (SQLException e) {
			traceHandler.traceException(TAG, "discardArrived", e);
			throw e;
		}
		if (rows != 1) {
			traceHandler.traceError(TAG,
					"discardArrived - Error deleting message {" + id
							+ "} from database: Rows affected = " + rows);
			return false;
		}
		int count = getArrivedRowCount(clientHandle);
		traceHandler
				.traceDebug(
						TAG,
						"discardArrived - Message deleted successfully. - messages in db for this clientHandle "
								+ count);
		return true;
	}

	/**
	 * Get an iterator over all messages stored (optionally for a specific client)
	 * 
	 * @param clientHandle
	 *            identifier for the client.<br>
	 *            If null, all messages are retrieved
	 * @return iterator of all the arrived MQTT messages
	 */
	@Override
	public Iterator<StoredMessage> getAllArrivedMessages(
			final String clientHandle) {
		return new Iterator<StoredMessage>() {
			private Cursor c;
			private boolean hasNext;

			{
				db = mqttDb.getWritableDatabase();
				// anonymous initialiser to start a suitable query
				// and position at the first row, if one exists
				if (clientHandle == null) {
					c = db.query(ARRIVED_MESSAGE_TABLE_NAME, null, null, null,
							null, null, "mtimestamp ASC");
				} else {
					c = db.query(ARRIVED_MESSAGE_TABLE_NAME, null,
							MqttServiceConstants.CLIENT_HANDLE + "='"
									+ clientHandle + "'", null, null, null,
							"mtimestamp ASC");
				}
				hasNext = c.moveToFirst();
			}

			@Override
			public boolean hasNext() {
		        if (hasNext == false){
		          	c.close();
				}
				return hasNext;
			}

			@Override
			public StoredMessage next() {
				String messageId = c.getString(c
						.getColumnIndex(MqttServiceConstants.MESSAGE_ID));
				String clientHandle = c.getString(c
						.getColumnIndex(MqttServiceConstants.CLIENT_HANDLE));
				String topic = c.getString(c
						.getColumnIndex(MqttServiceConstants.DESTINATION_NAME));
				byte[] payload = c.getBlob(c
						.getColumnIndex(MqttServiceConstants.PAYLOAD));
				int qos = c.getInt(c.getColumnIndex(MqttServiceConstants.QOS));
				boolean retained = Boolean.parseBoolean(c.getString(c
						.getColumnIndex(MqttServiceConstants.RETAINED)));
				boolean dup = Boolean.parseBoolean(c.getString(c
						.getColumnIndex(MqttServiceConstants.DUPLICATE)));

				// build the result
				MqttMessageHack message = new MqttMessageHack(payload);
				message.setQos(qos);
				message.setRetained(retained);
				message.setDuplicate(dup);

				// move on
				hasNext = c.moveToNext();
				return new DbStoredData(messageId, clientHandle, topic, message);
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

      /* (non-Javadoc)
       * @see java.lang.Object#finalize()
       */
      @Override
      protected void finalize() throws Throwable {
        c.close();
        super.finalize();
      }

    };
  }

	/**
	 * Delete all messages (optionally for a specific client)
	 * 
	 * @param clientHandle
	 *            identifier for the client.<br>
	 *            If null, all messages are deleted
	 */
	@Override
	public void clearArrivedMessages(String clientHandle) {
		
		db = mqttDb.getWritableDatabase();
		
		int rows = 0;
		if (clientHandle == null) {
			traceHandler.traceDebug(TAG,
					"clearArrivedMessages: clearing the table");
			rows = db.delete(ARRIVED_MESSAGE_TABLE_NAME, null, null);
		} else {
			traceHandler.traceDebug(TAG,
					"clearArrivedMessages: clearing the table of "
							+ clientHandle + " messages");
			rows = db.delete(ARRIVED_MESSAGE_TABLE_NAME,
					MqttServiceConstants.CLIENT_HANDLE + "='" + clientHandle
							+ "'", null);
		}
		traceHandler.traceDebug(TAG, "clearArrivedMessages: rows affected = "
				+ rows);
		return;
	}

	private class DbStoredData implements StoredMessage {
		private String messageId;
		private String clientHandle;
		private String topic;
		private MqttMessage message;

		DbStoredData(String messageId, String clientHandle, String topic,
				MqttMessage message) {
			this.messageId = messageId;
			this.topic = topic;
			this.message = message;
		}

		@Override
		public String getMessageId() {
			return messageId;
		}

		@Override
		public String getClientHandle() {
			return clientHandle;
		}

		@Override
		public String getTopic() {
			return topic;
		}

		@Override
		public MqttMessage getMessage() {
			return message;
		}
	}

	/**
	 * A way to get at the "setDuplicate" method of MqttMessage
	 */
	private class MqttMessageHack extends MqttMessage {

		public MqttMessageHack(byte[] payload) {
			super(payload);
		}

		@Override
		protected void setDuplicate(boolean dup) {
			super.setDuplicate(dup);
		}
	}

	@Override
	public void close() {
		if (this.db!=null)
			this.db.close();
		
	}

}