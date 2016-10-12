package org.eclipse.paho.android.sample.internal;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import org.eclipse.paho.android.sample.activity.Connection;
import org.eclipse.paho.android.sample.model.Subscription;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * <code>Persistence</code> deals with interacting with the database to persist
 * {@link Connection} objects so created clients survive, the destruction of the
 * singleton {@link Connections} object.
 *
 */
public class Persistence extends SQLiteOpenHelper implements BaseColumns {

    private static final String TAG = "Persistence";

    /** The version of the database **/
    private static final int DATABASE_VERSION = 1;

    /** The name of the database file **/
    private static final String DATABASE_NAME = "connections.db";

    /** The name of the connections table **/
    private static final String TABLE_CONNECTIONS = "connections";



    /** Table column for client handle **/
    private static final String COLUMN_CLIENT_HANDLE = "clientHandle";

    /** Table column for host **/
    private static final String COLUMN_HOST = "host";
    /** Table column for client id **/
    private static final String COLUMN_client_ID = "clientID";
    /** Table column for port **/
    private static final String COLUMN_port = "port";
    /** Table column for ssl enabled**/
    private static final String COLUMN_ssl = "ssl";

    //connection options
    /** Table column for client's timeout**/
    private static final String COLUMN_TIME_OUT = "timeout";
    /** Table column for client's keepalive **/
    private static final String COLUMN_KEEP_ALIVE = "keepalive";
    /** Table column for the client's username**/
    private static final String COLUMN_USER_NAME = "username";
    /** Table column for the client's password**/
    private static final String COLUMN_PASSWORD = "password";
    /** Table column for clean session **/
    private static final String COLUMN_CLEAN_SESSION = "cleanSession";
    /** Table column for **/

    //last will
    /** Table column for last will topic **/
    private static final String COLUMN_TOPIC = "topic";
    /** Table column for the last will message payload **/
    private static final String COLUMN_MESSAGE = "message";
    /** Table column for the last will message qos **/
    private static final String COLUMN_QOS = "qos";
    /** Table column for the retained state of the message **/
    private static final String COLUMN_RETAINED = "retained";


    /** The name of the subscriptions table **/
    private static final String TABLE_SUBSCRIPTIONS = "subscriptions";

    /** Table column for subscription topic **/
    private static final String SUBSCRIPTIONS_COLUMN_TOPIC = "topic";
    /** Table column for the subscription qos **/
    private static final String SUBSCRIPTIONS_COLUMN_QOS = "qos";
    /** Table column for the subscription enable notification setting **/
    private static final String SUBSCRIPTIONS_COLUMN_NOTIFY = "notify";


    //sql lite data types
    /** Text type for SQLite**/
    private static final String TEXT_TYPE = " TEXT";
    /** Int type for SQLite**/
    private static final String INT_TYPE = " INTEGER";
    /**Comma separator **/
    private static final String COMMA_SEP = ",";

    /** Create tables query **/
    private static final String SQL_CREATE_ENTRIES =

            "CREATE TABLE " + TABLE_CONNECTIONS + " (" +
                    _ID + " INTEGER PRIMARY KEY," +
                    COLUMN_CLIENT_HANDLE + TEXT_TYPE + COMMA_SEP +
                    COLUMN_HOST + TEXT_TYPE + COMMA_SEP +
                    COLUMN_client_ID + TEXT_TYPE + COMMA_SEP +
                    COLUMN_port + INT_TYPE + COMMA_SEP +
                    COLUMN_ssl + INT_TYPE + COMMA_SEP +
                    COLUMN_TIME_OUT + INT_TYPE + COMMA_SEP +
                    COLUMN_KEEP_ALIVE + INT_TYPE + COMMA_SEP +
                    COLUMN_USER_NAME + TEXT_TYPE + COMMA_SEP +
                    COLUMN_PASSWORD + TEXT_TYPE + COMMA_SEP +
                    COLUMN_CLEAN_SESSION + INT_TYPE + COMMA_SEP +
                    COLUMN_TOPIC + TEXT_TYPE + COMMA_SEP +
                    COLUMN_MESSAGE + TEXT_TYPE + COMMA_SEP +
                    COLUMN_QOS + INT_TYPE + COMMA_SEP +
                    COLUMN_RETAINED + " INTEGER);";

    private static final String SQL_CREATE_SUBSCRIPTION_ENTRIES =

            "CREATE TABLE " + TABLE_SUBSCRIPTIONS + " (" +
                    _ID + " INTEGER PRIMARY KEY," +
                    COLUMN_CLIENT_HANDLE + TEXT_TYPE + COMMA_SEP +
                    COLUMN_HOST + TEXT_TYPE + COMMA_SEP +
                    SUBSCRIPTIONS_COLUMN_TOPIC + TEXT_TYPE + COMMA_SEP +
                    SUBSCRIPTIONS_COLUMN_NOTIFY + INT_TYPE + COMMA_SEP +
                    SUBSCRIPTIONS_COLUMN_QOS + INT_TYPE + ");";

    /** Delete tables entry **/
    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + TABLE_CONNECTIONS;

    private static final String SQL_DELETE_SUBSCRIPTION_ENTRIES =
            "DROP TABLE IF EXISTS " + TABLE_SUBSCRIPTIONS;

    /**
     * Creates the persistence object passing it a context
     * @param context Context that the application is running in
     */
    public Persistence(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /* (non-Javadoc)
     * @see org.eclipse.paho.android.service.database.sqlite.SQLiteOpenHelper#onCreate(org.eclipse.paho.android.service.database.sqlite.SQLiteDatabase)
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
        db.execSQL(SQL_CREATE_SUBSCRIPTION_ENTRIES);

    }

    /* (non-Javadoc)
     * @see org.eclipse.paho.android.service.database.sqlite.SQLiteOpenHelper#onUpgrade(org.eclipse.paho.android.service.database.sqlite.SQLiteDatabase, int, int)
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        db.execSQL(SQL_DELETE_SUBSCRIPTION_ENTRIES);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.paho.android.service.database.sqlite.SQLiteOpenHelper#onDowngrade(org.eclipse.paho.android.service.database.sqlite.SQLiteDatabase, int, int)
     */
    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    /**
     * Persist a Connection to the database
     * @param connection the connection to persist
     * @throws PersistenceException If storing the data fails
     */
    public void persistConnection(Connection connection) throws PersistenceException {
        SQLiteDatabase db = getWritableDatabase();

        //insert the values into the tables, returns the ID for the row
        long newRowId = db.insert(TABLE_CONNECTIONS, null, getValues(connection));

        db.close(); //close the db then deal with the result of the query

        if (newRowId == -1) {
            throw new PersistenceException("Failed to persist connection: " + connection.handle());
        }
        else { //Successfully persisted assigning persistenceID
            connection.assignPersistenceId(newRowId);
        }
    }

    /**
     * Updates a {@link Connection} in the database
     * @param connection {@link Connection} to update
     */
    public void updateConnection(Connection connection) {
        SQLiteDatabase db = getWritableDatabase();
        String whereClause = _ID + "=?";
        String[] whereArgs = new String[1];
        whereArgs[0] = String.valueOf(connection.persistenceId());
        db.update(TABLE_CONNECTIONS, getValues(connection), whereClause, whereArgs);
    }

    private ContentValues getValues(Connection connection){
        MqttConnectOptions conOpts = connection.getConnectionOptions();
        MqttMessage lastWill = conOpts.getWillMessage();
        ContentValues values = new ContentValues();

        //put the column values object
        values.put(COLUMN_CLIENT_HANDLE, connection.handle());
        values.put(COLUMN_HOST, connection.getHostName());
        values.put(COLUMN_port, connection.getPort());
        values.put(COLUMN_client_ID, connection.getId());
        values.put(COLUMN_ssl, connection.isSSL());

        values.put(COLUMN_KEEP_ALIVE, conOpts.getKeepAliveInterval());
        values.put(COLUMN_TIME_OUT, conOpts.getConnectionTimeout());
        values.put(COLUMN_USER_NAME, conOpts.getUserName());
        values.put(COLUMN_TOPIC, conOpts.getWillDestination());

        //uses "condition ? trueValue: falseValue" for in line converting of values
        char[] password = conOpts.getPassword();
        values.put(COLUMN_CLEAN_SESSION, conOpts.isCleanSession() ? 1 : 0); //convert boolean to int and then put in values
        values.put(COLUMN_PASSWORD, password != null ? String.valueOf(password) : null); //convert char[] to String
        values.put(COLUMN_MESSAGE, lastWill != null ? new String(lastWill.getPayload()) : null); // convert byte[] to string
        values.put(COLUMN_QOS, lastWill != null ? lastWill.getQos() : 0);

        if (lastWill == null) {
            values.put(COLUMN_RETAINED, 0);
        }
        else {
            values.put(COLUMN_RETAINED, lastWill.isRetained() ? 1 : 0); //convert from boolean to int
        }
        return values;
    }

    /**
     * Persist a Subscription to the database
     * @param subscription the subscription to persist
     * @throws PersistenceException If storing the data fails
     */
    public long persistSubscription(Subscription subscription) throws PersistenceException {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CLIENT_HANDLE, subscription.getClientHandle());
        values.put(SUBSCRIPTIONS_COLUMN_TOPIC, subscription.getTopic());
        values.put(SUBSCRIPTIONS_COLUMN_NOTIFY, subscription.isEnableNotifications() ? 1 : 0); //convert boolean to int and then put in values
        values.put(SUBSCRIPTIONS_COLUMN_QOS, subscription.getQos());

        long newRowId = db.insert(TABLE_SUBSCRIPTIONS, null, values);
        db.close();
        if(newRowId == -1){
            throw new PersistenceException("Failed to persist subscription: " + subscription.toString());
        } else {
            subscription.setPersistenceId(newRowId);
            return newRowId;
        }
    }


    /**
     * Deletes a subscription from the database
     * @param subscription The subscription to delete from the database
     */
    public void deleteSubscription(Subscription subscription) {
        Log.d(TAG, "Deleting Subscription: " + subscription.toString());
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_SUBSCRIPTIONS, _ID + "=?", new String[]{String.valueOf(subscription.getPersistenceId())});
        db.close();
        //don't care if it failed, means it's not in the db therefore no need to delete

    }


    /**
     * Recreates connection objects based upon information stored in the database
     * @param context Context for creating {@link Connection} objects
     * @return list of connections that have been restored
     * @throws PersistenceException if restoring connections fails, this is thrown
     */
    public List<Connection> restoreConnections(Context context) throws PersistenceException
    {


        //columns to return
        String[] connectionColumns = {
                COLUMN_CLIENT_HANDLE,
                COLUMN_HOST,
                COLUMN_port,
                COLUMN_client_ID,
                COLUMN_ssl,
                COLUMN_KEEP_ALIVE,
                COLUMN_CLEAN_SESSION,
                COLUMN_TIME_OUT,
                COLUMN_USER_NAME,
                COLUMN_PASSWORD,
                COLUMN_TOPIC,
                COLUMN_MESSAGE,
                COLUMN_RETAINED,
                COLUMN_QOS,
                _ID

           };

        // Columns to return for subscription
        String[] subscriptionColumns = {
                COLUMN_CLIENT_HANDLE,
                SUBSCRIPTIONS_COLUMN_TOPIC,
                SUBSCRIPTIONS_COLUMN_NOTIFY,
                SUBSCRIPTIONS_COLUMN_QOS,
                _ID
        };

        String subscriptionWhereQuery = COLUMN_CLIENT_HANDLE + "=?";

        //how to sort the data being returned
        String sort = COLUMN_HOST;

        SQLiteDatabase db = getReadableDatabase();

        Cursor c = db.query(TABLE_CONNECTIONS, connectionColumns, null, null, null, null, sort);
        ArrayList<Connection> list = new ArrayList<Connection>(c.getCount());
        Connection connection;
        for (int i = 0; i < c.getCount(); i++) {
            if (!c.moveToNext()) { //move to the next item throw persistence exception, if it fails
                throw new PersistenceException("Failed restoring connection - count: " + c.getCount() + "loop iteration: " + i);
            }
            //get data from cursor
            Long id = c.getLong(c.getColumnIndexOrThrow(_ID));
            //basic client information
            String clientHandle = c.getString(c.getColumnIndexOrThrow(COLUMN_CLIENT_HANDLE));
            String host = c.getString(c.getColumnIndexOrThrow(COLUMN_HOST));
            String clientID = c.getString(c.getColumnIndexOrThrow(COLUMN_client_ID));
            int port = c.getInt(c.getColumnIndexOrThrow(COLUMN_port));

            //connect options strings
            String username = c.getString(c.getColumnIndexOrThrow(COLUMN_USER_NAME));
            String password = c.getString(c.getColumnIndexOrThrow(COLUMN_PASSWORD));
            String topic = c.getString(c.getColumnIndexOrThrow(COLUMN_TOPIC));
            String message = c.getString(c.getColumnIndexOrThrow(COLUMN_MESSAGE));

            //connect options integers
            int qos = c.getInt(c.getColumnIndexOrThrow(COLUMN_QOS));
            int keepAlive = c.getInt(c.getColumnIndexOrThrow(COLUMN_KEEP_ALIVE));
            int timeout = c.getInt(c.getColumnIndexOrThrow(COLUMN_TIME_OUT));

            //get all values that need converting and convert integers to booleans in line using "condition ? trueValue : falseValue"
            boolean cleanSession = c.getInt(c.getColumnIndexOrThrow(COLUMN_CLEAN_SESSION)) == 1 ;
            boolean retained = c.getInt(c.getColumnIndexOrThrow(COLUMN_RETAINED)) == 1;
            boolean ssl = c.getInt(c.getColumnIndexOrThrow(COLUMN_ssl)) == 1;

            //rebuild objects starting with the connect options
            MqttConnectOptions opts = new MqttConnectOptions();
            opts.setCleanSession(cleanSession);
            opts.setKeepAliveInterval(keepAlive);
            opts.setConnectionTimeout(timeout);

            opts.setPassword(password != null ? password.toCharArray() : null);
            opts.setUserName(username);

            if (topic != null) {
                opts.setWill(topic, message.getBytes(), qos, retained);
            }

            //now create the connection object
            connection = Connection.createConnection(clientHandle, clientID, host, port, context, ssl);
            connection.addConnectionOptions(opts);
            connection.assignPersistenceId(id);

            // Now we recover all subscriptions for this connection
            String[] args = {clientHandle};
            System.out.println("SUB: " + connection.toString());

            Cursor sub_c = db.query(TABLE_SUBSCRIPTIONS, subscriptionColumns, subscriptionWhereQuery, args, null, null, sort);
            ArrayList<Subscription> subscriptions = new ArrayList<Subscription>(sub_c.getCount());
            for (int x = 0; x < sub_c.getCount(); x++) {
                if (!sub_c.moveToNext()) { //move to the next item throw persistence exception, if it fails
                    throw new PersistenceException("Failed restoring subscription - count: " + sub_c.getCount() + "loop iteration: " + x);
                }
                Long sub_id =  sub_c.getLong(sub_c.getColumnIndexOrThrow(_ID));
                String sub_clientHandle = sub_c.getString(sub_c.getColumnIndexOrThrow(COLUMN_CLIENT_HANDLE));
                String sub_topic = sub_c.getString(sub_c.getColumnIndexOrThrow(SUBSCRIPTIONS_COLUMN_TOPIC));
                boolean sub_notify = sub_c.getInt(sub_c.getColumnIndexOrThrow(SUBSCRIPTIONS_COLUMN_NOTIFY)) == 1;
                int sub_qos = sub_c.getInt(sub_c.getColumnIndexOrThrow(SUBSCRIPTIONS_COLUMN_QOS));
                Subscription sub = new Subscription(sub_topic, sub_qos, sub_clientHandle, sub_notify);
                sub.setPersistenceId(sub_id);
                Log.d(TAG, "Restoring Subscription: " + sub.toString());
                subscriptions.add(sub);
            }
            sub_c.close();

            connection.setSubscriptions(subscriptions);

            //store it in the list
            list.add(connection);

        }
        //close the cursor now we are finished with it
        c.close();


            db.close();
        return list;

    }

    /**
     * Deletes a connection from the database
     * @param connection The connection to delete from the database
     */
    public void deleteConnection(Connection connection) {
        SQLiteDatabase db = getWritableDatabase();

        db.delete(TABLE_CONNECTIONS, _ID + "=?", new String[]{String.valueOf(connection.persistenceId())});
        db.close();
        //don't care if it failed, means it's not in the db therefore no need to delete

    }






}