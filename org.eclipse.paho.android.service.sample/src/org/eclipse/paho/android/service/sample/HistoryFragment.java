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
package org.eclipse.paho.android.service.sample;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.Spanned;
import android.widget.ArrayAdapter;
import org.eclipse.paho.android.service.sample.R;

/**
 * This fragment displays the history information for a client
 *
 */
public class HistoryFragment extends ListFragment {

  /** Client handle to a {@link Connection} object **/
  String clientHandle = null;
  /** {@link ArrayAdapter} to display the formatted text **/
  ArrayAdapter<Spanned> arrayAdapter = null;

  /**
   * @see ListFragment#onCreate(Bundle)
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    //Pull history information out of bundle

    clientHandle = getArguments().getString("handle");
    Connection connection = Connections.getInstance(getActivity()).getConnection(clientHandle);

    Spanned[] history = connection.history();

    //Initialise the arrayAdapter, view and add data
    arrayAdapter = new ArrayAdapter<Spanned>(getActivity(), R.layout.list_view_text_view);

    arrayAdapter.addAll(history);
    setListAdapter(arrayAdapter);

  }

  /**
   * Updates the data displayed to match the current history
   */
  public void refresh() {
    if (arrayAdapter != null) {
      arrayAdapter.clear();
      arrayAdapter.addAll(Connections.getInstance(getActivity()).getConnection(clientHandle).history());
      arrayAdapter.notifyDataSetChanged();
    }

  }

}
