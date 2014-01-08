/*
 * Copyright (c) 2009, 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.eclipse.paho.android.service;

/**
 * Interface for simple trace handling
 *
 */
public interface MqttTraceHandler {

	public abstract void traceDebug(String source, String message);

	public abstract void traceError(String source, String message);
	
	public abstract void traceException(String source, String message, Exception e);

}