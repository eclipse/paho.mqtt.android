package org.eclipse.paho.android.sample.activity;

class ActivityConstants {

    /** Bundle key for passing a connection around by it's name **/
    public static final String CONNECTION_KEY = "CONNECTION_KEY";

    public static final String AUTO_CONNECT = "AUTO_CONNECT";
    public static final String CONNECTED = "CONNECTEd";

    public static final String LOGGING_KEY = "LOGGING_ENABLED";


    /** Property name for the history field in {@link Connection} object for use with {@link java.beans.PropertyChangeEvent} **/
    public static final String historyProperty = "history";

    /** Property name for the connection status field in {@link Connection} object for use with {@link java.beans.PropertyChangeEvent} **/
    public static final String ConnectionStatusProperty = "connectionStatus";



    /** Empty String for comparisons **/
    static final String empty = "";
}
