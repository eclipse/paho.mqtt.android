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

/**
 * Interface for simple trace handling, pass the trace message to trace
 * callback.
 * 
 */

public interface MqttTraceHandler {

	/**
	 * Trace debugging information
	 * 
	 * @param tag
	 *            identifier for the source of the trace
	 * @param message
	 *            the text to be traced
	 */
	public abstract void traceDebug(String source, String message);

	/**
	 * Trace error information
	 * 
	 * @param tag
	 *            identifier for the source of the trace
	 * @param message
	 *            the text to be traced
	 */
	public abstract void traceError(String source, String message);

	/**
	 * trace exceptions
	 * 
	 * @param tag
	 *            identifier for the source of the trace
	 * @param message
	 *            the text to be traced
	 * @param e
	 *            the exception
	 */
	public abstract void traceException(String source, String message,
			Exception e);

}