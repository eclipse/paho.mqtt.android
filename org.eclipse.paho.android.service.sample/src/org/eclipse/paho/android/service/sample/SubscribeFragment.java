/*
 * Licensed Materials - Property of IBM
 *
 * 5747-SM3
 *
 * (C) Copyright IBM Corp. 1999, 2012 All Rights Reserved.
 *
 * US Government Users Restricted Rights - Use, duplication or
 * disclosure restricted by GSA ADP Schedule Contract with
 * IBM Corp.
 *
 */
package org.eclipse.paho.android.service.sample;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import org.eclipse.paho.android.service.sample.R;

/**
 * Fragment for the subscribe pane for the client
 *
 */
public class SubscribeFragment extends Fragment {

  /**
   * @see Fragment#onCreateView(LayoutInflater, ViewGroup, Bundle)
   */
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {

    return LayoutInflater.from(getActivity()).inflate(R.layout.activity_subscribe, null);

  }

}
