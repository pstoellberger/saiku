/*  
 *   Copyright 2012 OSBI Ltd
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package org.saiku.olap.util.exception;

public class SaikuIncompatibleException extends Exception {

	private static final long serialVersionUID = 6079334291828346380L;
	
	/**
	 * @see java.lang.Exception#Exception()
	 */
	public SaikuIncompatibleException() {
		super();
	}
	
	/**
	 * @see java.lang.Exception#Exception(String)
	 */
	public SaikuIncompatibleException(String message) {
		super(message);
	}

	/**
	 * @see java.lang.Exception#Exception(Throwable)
	 */
	public SaikuIncompatibleException(Throwable cause) {
		super(cause);
	}
	
	/**
	 * @see java.lang.Exception#Exception(String, Throwable)
	 */
	public SaikuIncompatibleException(String message, Throwable cause) {
		super(message,cause);
	}
}
