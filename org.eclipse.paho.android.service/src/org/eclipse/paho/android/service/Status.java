/*
============================================================================ 
Licensed Materials - Property of IBM

5747-SM3
 
(C) Copyright IBM Corp. 1999, 2012 All Rights Reserved.
 
US Government Users Restricted Rights - Use, duplication or
disclosure restricted by GSA ADP Schedule Contract with
IBM Corp.
============================================================================
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
