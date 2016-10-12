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
 *
 *   Contributors:
 *     James Sutton - Updating in new Sample App
 */
package org.eclipse.paho.android.sample.internal;


import android.content.Context;

import org.eclipse.paho.android.sample.activity.Connection;
import org.eclipse.paho.android.service.MqttAndroidClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <code>Connections</code> is a singleton class which stores all the connection objects
 * in one central place so they can be passed between activities using a client handle.
 */
public class Connections {

    /** Singleton instance of <code>Connections</code>**/
    private static Connections instance = null;

    /** List of {@link Connection} object **/
    private HashMap<String, Connection> connections = null;

    /** {@link Persistence} object used to save, delete and restore connections**/
    private Persistence persistence = null;

    /**
     * Create a Connections object
     * @param context Applications context
     */
    private Connections(Context context){
        connections = new HashMap<String, Connection>();

        // If there is state, attempt to restore it
        persistence = new Persistence(context);
        try {
            List<Connection> connectionList = persistence.restoreConnections(context);
            for(Connection connection : connectionList) {
                System.out.println("Connection was persisted.." + connection.handle());
                connections.put(connection.handle(), connection);
            }
        } catch (PersistenceException e){
            e.printStackTrace();
        }
    }

    /**
     * Returns an already initialised instance of <code>Connections</code>, if Connections has yet to be created, it will
     * create and return that instance.
     * @param context The applications context used to create the <code>Connections</code> object if it is not already initialised
     * @return <code>Connections</code> instance
     */
    public synchronized static Connections getInstance(Context context){
        if(instance ==  null){
            instance = new Connections(context);
        }
        return instance;
    }

    /**
     * Finds and returns a {@link Connection} object that the given client handle points to
     * @param handle The handle to the {@link Connection} to return
     * @return a connection associated with the client handle, <code>null</code> if one is not found
     */
    public Connection getConnection(String handle){
        return connections.get(handle);
    }

    /**
     * Adds a {@link Connection} object to the collection of connections associated with this object
     * @param connection {@link Connection} to add
     */
    public void addConnection(Connection connection){
        connections.put(connection.handle(), connection);
        try{
            persistence.persistConnection(connection);
        } catch (PersistenceException e){
            // @todo Handle this error more appropriately
            //error persisting well lets just swallow this
            e.printStackTrace();
        }

    }

// --Commented out by Inspection START (12/10/2016, 10:21):
//    /**
//     * Create a fully initialised <code>MqttAndroidClient</code> for the parameters given
//     * @param context The Applications context
//     * @param serverURI The ServerURI to connect to
//     * @param clientId The clientId for this client
//     * @return new instance of MqttAndroidClient
//     */
//    public MqttAndroidClient createClient(Context context, String serverURI, String clientId){
//        return new MqttAndroidClient(context, serverURI, clientId);
//    }
// --Commented out by Inspection STOP (12/10/2016, 10:21)

    /**
     * Get all the connections associated with this <code>Connections</code> object.
     * @return <code>Map</code> of connections
     */
    public Map<String, Connection> getConnections(){
        return connections;
    }

    /**
     * Removes a connection from the map of connections
     * @param connection connection to be removed
     */
    public void removeConnection(Connection connection){
        connections.remove(connection.handle());
        persistence.deleteConnection(connection);
    }


    /**
     * Updates an existing connection within the map of
     * connections as well as in the persisted model
     * @param connection connection to be updated.
     */
    public void updateConnection(Connection connection){
        connections.put(connection.handle(), connection);
        persistence.updateConnection(connection);
    }


}
