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
package org.eclipse.paho.android.service;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseServiceNotificationCallbackStore implements
		ServiceNotificationCallbackStore {
	
	// TAG used for indentify trace data etc.
	private static String TAG = "DatabaseServiceNotificationCallbackStore";

	
	private static final String SERVICE_NTFCALLBACK_TABLE_NAME = "MqttServiceNTFCallbackTable";

	// the database
	private SQLiteDatabase db = null;

	// a SQLiteOpenHelper specific for this database
	private MQTTDatabaseHelper mqttDb = null;

	// a place to send trace data
	private MqttTraceHandler traceHandler = null;
	
	public DatabaseServiceNotificationCallbackStore(MqttService service, Context context) {
		this.traceHandler = (MqttTraceHandler) service;

		// Open message database
		mqttDb = new MQTTDatabaseHelper(traceHandler, context);

		// Android documentation suggests that this perhaps
		// could/should be done in another thread, but as the
		// database is only one table, I doubt it matters...
		traceHandler.traceDebug(TAG, "DatabaseMessageStore<init> complete");
	}

	@Override
	public String getAppServiceNTFCallbackClass(String appPackageName) {
		
		db = mqttDb.getWritableDatabase();
		
		String[] cols = new String[2];
		cols[0] = "apppack";
		cols[1]="ntfcls";
		Cursor c = db.query(SERVICE_NTFCALLBACK_TABLE_NAME, cols,
				 "apppack='" + appPackageName + "'",
				null, null, null, null);
		String ntfCallbackcls=null;
		if (c.moveToFirst()) {
			ntfCallbackcls = c.getString(1);
		}
		c.close();
		
		return ntfCallbackcls;
	}

	@Override
	public boolean setAppServiceNTFCallbackClass(String appPackageName,
			String NTFCallback) {
		
		db = mqttDb.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put("ntfcls", NTFCallback);
		
		//check if the appPackageName key exists
		String oldNTFCallback=getAppServiceNTFCallbackClass(appPackageName);
		
		try {
			//if exists, let's update it
			if (oldNTFCallback!=null) {
				if(!oldNTFCallback.equals(NTFCallback)) {
					db.update(SERVICE_NTFCALLBACK_TABLE_NAME, values, "apppack=?",new String[]{appPackageName});
				}
			}
			//if not, let's insert it
			else {
				values.put("apppack", appPackageName);
				db.insertOrThrow(SERVICE_NTFCALLBACK_TABLE_NAME, null, values);
			}
			return true;
		} catch (SQLException e) {
			traceHandler.traceException(TAG, "onUpgrade", e);
		}
		
		return false;
	}

	private static class MQTTDatabaseHelper extends SQLiteOpenHelper {
		// TAG used for indentify trace data etc.
		private static String TAG = "MQTTDatabaseHelper";

		private static final String DATABASE_NAME = "mqttAndroidServiceNTFCallback.db";

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

		@Override
		public void onCreate(SQLiteDatabase database) {
			String createArrivedTableStatement = "CREATE TABLE "
					+ SERVICE_NTFCALLBACK_TABLE_NAME + "("
					+" apppack TEXT PRIMARY KEY,"
					+" ntfcls TEXT"
					+ ");";
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

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			traceHandler.traceDebug(TAG, "onUpgrade");
			try {
				db.execSQL("DROP TABLE IF EXISTS " + SERVICE_NTFCALLBACK_TABLE_NAME);
			} catch (SQLException e) {
				traceHandler.traceException(TAG, "onUpgrade", e);
				throw e;
			}
			onCreate(db);
			traceHandler.traceDebug(TAG, "onUpgrade complete");
		}
	}

	@Override
	public void close() {
		this.db.close();
		
	}
}
