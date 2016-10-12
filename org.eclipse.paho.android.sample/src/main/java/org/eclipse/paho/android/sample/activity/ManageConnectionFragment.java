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
 *   Authors:
 *       - James Sutton - Created file 10-2015
 */
package org.eclipse.paho.android.sample.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.eclipse.paho.android.sample.R;
import org.eclipse.paho.android.sample.internal.Connections;

import java.util.Map;


public class ManageConnectionFragment extends Fragment {
    private Connection connection;
    private Map<String, Connection> connections;
    private String connectionKey;


    public ManageConnectionFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
         connections = Connections.getInstance(this.getActivity())
                .getConnections();
        connectionKey = this.getArguments().getString(ActivityConstants.CONNECTION_KEY);
        connection = connections.get(connectionKey);
        setHasOptionsMenu(false);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_manage, container, false);
        final String name = connection.getId() + "@" + connection.getHostName() + ":" + connection.getPort();
        TextView label = (TextView) rootView.findViewById(R.id.connection_id_text);
        label.setText(name);

        Button deleteButton = (Button) rootView.findViewById(R.id.delete_button);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               System.out.println("Deleting Connection: " + name + ".");
                connections.remove(connectionKey);
                Connections.getInstance(getActivity()).removeConnection(connection);
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.container_body, new HomeFragment());
                fragmentTransaction.commit();
                ((MainActivity) getActivity()).removeConnectionRow(connection);
            }
        });

        Button editButton = (Button) rootView.findViewById(R.id.edit_button);
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("Editing Connection: " + name + ".");
                EditConnectionFragment editConnectionFragment = new EditConnectionFragment();
                Bundle bundle = new Bundle();
                bundle.putString(ActivityConstants.CONNECTION_KEY, connection.handle());
                editConnectionFragment.setArguments(bundle);
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.container_body, editConnectionFragment);
                fragmentTransaction.commit();
            }
        });


        // Inflate the layout for this fragment
        return rootView;
    }

}