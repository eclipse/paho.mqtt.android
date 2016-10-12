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

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

class TestProperties {


    private final Class<?> cclass = TestProperties.class;
    private final String className = cclass.getName();

    private final String KEY_SERVER_URI = "SERVER_URI";
    private final String KEY_CLIENT_KEY_STORE = "CLIENT_KEY_STORE";
    private final String KEY_CLIENT_KEY_STORE_PASSWORD = "CLIENT_KEY_STORE_PASSWORD";
    private final String KEY_SERVER_SSL_URI = "SERVER_SSL_URI";
    private final String KEY_WAIT_FOR_COMPLETION_TIME = "WAIT_FOR_COMPLETION_TIME";

    private Properties properties = new Properties();

    private Context context;


    /**
     * Reads properties from a properties file
     */
    public TestProperties(Context context) {

        this.context = context;
        InputStream stream = null;
        try {
            String filename = "test.properties";
            stream = getPropertyFileAsStream(filename);

            // Read the properties from the property file
            if (stream != null) {
                Log.i("TestProperties","Loading properties from: '" + filename + "'");
                properties.load(stream);
            }
        } catch (Exception e) {
            Log.e("TestProperties", "caught exception:", e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    Log.e("TestProperties", "caught exception:", e);
                }
            }
        }
    }

    /**
     * @param fileName
     * @return stream
     * @throws IOException
     */
    private InputStream getPropertyFileAsStream(String fileName) throws IOException {
        InputStream stream = null;
        try {

            stream = this.context.getResources().getAssets().open(fileName);

        }
        catch (Exception exception) {
            Log.e("TestProperties", "Property file: '" + fileName + "' not found");
        }

        return stream;
    }


    /**
     * @param key
     * @return value
     */
    private String getProperty(String key) {
        return properties.getProperty(key);
    }

    /**
     * @param key
     * @return value
     */
    public boolean getBooleanProperty(String key) {
        String value = getProperty(key);
        return Boolean.parseBoolean(value);
    }

    /**
     * @param key
     * @return value
     */
    private int getIntProperty(String key) {
        String value = getProperty(key);
        return Integer.parseInt(value);
    }


    /**
     * @return keystore file name
     */

    public String getClientKeyStore() {
        return getProperty(KEY_CLIENT_KEY_STORE);
    }

    /**
     * @return keystore file password
     */

    public String getClientKeyStorePassword() {
        return getProperty(KEY_CLIENT_KEY_STORE_PASSWORD);
    }

    /**
     * @return the SSL url of the server for testing
     */

    public String getServerSSLURI() {
        return getProperty(KEY_SERVER_SSL_URI);
    }

    /**
     * @return the server url for testing
     */
    public String getServerURI() {
        return getProperty(KEY_SERVER_URI);

    }

    public int getWaitForCompletionTime(){
        return getIntProperty(KEY_WAIT_FOR_COMPLETION_TIME);
    }
}
