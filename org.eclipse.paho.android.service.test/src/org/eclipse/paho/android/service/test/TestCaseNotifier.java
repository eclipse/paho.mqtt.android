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
package org.eclipse.paho.android.service.test;

/**
 * @author Rhys
 *
 */
public class TestCaseNotifier {

  private Throwable exception;

  public void storeException(Throwable exception) {
    this.exception = exception;
  }

  public synchronized void waitForCompletion(long timeout) throws Throwable {

    try {
      wait(timeout);
    }
    catch (InterruptedException e) {}

    if (exception != null) {
      throw exception;
    }

  }

}
