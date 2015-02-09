/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.action.imperihab.internal;

import org.openhab.core.scriptengine.action.ActionDoc;
import org.openhab.core.scriptengine.action.ParamDoc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains the methods that are made available in scripts and rules for imperiHab.
 * 
 * @author reven
 * @since 1.7.0
 */
public class imperiHab {

	private static final Logger logger = LoggerFactory.getLogger(imperiHab.class);

	static void log(String message){
		logger.debug("ImperiHabAction: " + message);
	}

	public static String defaultImperiHomeAddress = null;
	// provide public static methods here
	
	@ActionDoc(text="Sends a Text to Speech command to the default ImperiHAB device", 
			returns="<code>true</code>, if successful and <code>false</code> otherwise.")
	public static boolean imperiHabSay(@ParamDoc(name="text", text="The text to speak") String text) {
		if (!imperiHabActionService.isProperlyConfigured) {
			logger.debug("imperiHab action is not yet configured - execution aborted!");
			return false;
		}
		log("About to say: " + text);
		imperiHabBackgroundWorker worker = new imperiHabBackgroundWorker(defaultImperiHomeAddress, text);		
		Thread t = new Thread(worker);
		t.start();
		return true;
	}

	@ActionDoc(text="Goes to a specific dashboard page on the default ImperiHAB device", 
			returns="<code>true</code>, if successful and <code>false</code> otherwise.")
	public static boolean imperiHabGotoPage(@ParamDoc(name="pageIndex") int pageIndex){
		imperiHabBackgroundWorker worker = new imperiHabBackgroundWorker(defaultImperiHomeAddress, pageIndex);		
		Thread t = new Thread(worker);
		t.start();
		return true;
	}

}