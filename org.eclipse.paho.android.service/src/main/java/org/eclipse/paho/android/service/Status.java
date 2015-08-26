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
 * Enumeration representing the success or failure of an operation
 */
enum Status {
	/**
	 * Indicates that the operation succeeded
	 */
	OK, 
	
	/**
	 * Indicates that the operation failed
	 */
	ERROR,
	
	/**
	 * Indicates that the operation's result may be returned asynchronously
	 */
	NO_RESULT
}
