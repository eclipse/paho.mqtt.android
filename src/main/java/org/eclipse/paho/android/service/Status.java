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
 * Enumeration representing the success or failure of an operation
 */
public enum Status {
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
