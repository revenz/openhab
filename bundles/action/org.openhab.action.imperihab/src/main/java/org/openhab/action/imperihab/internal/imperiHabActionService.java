/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.action.imperihab.internal;

import java.util.Dictionary;
import java.util.Enumeration;

import org.apache.commons.lang.StringUtils;
import org.openhab.core.scriptengine.action.ActionService;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
	

/**
 * This class registers an OSGi service for the imperiHab action.
 * 
 * @author reven
 * @since 1.7.0
 */
public class imperiHabActionService implements ActionService, ManagedService {

	private static final Logger logger = LoggerFactory.getLogger(imperiHabActionService.class);
	
	
	static void log(String message){
		logger.debug("ImperiHabAction: " + message);
	}

	/**
	 * Indicates whether this action is properly configured which means all
	 * necessary configurations are set. This flag can be checked by the
	 * action methods before executing code.
	 */
	/* default */ static boolean isProperlyConfigured = false;
	
	public imperiHabActionService() {
	}

	public void activate() {
		log("ImperiHab action service activated");
	}

	public void deactivate() {
		log("ImperiHab action service deactivated");
	}

	@Override
	public String getActionClassName() {
		return imperiHab.class.getCanonicalName();
	}

	@Override
	public Class<?> getActionClass() {
		return imperiHab.class;
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void updated(Dictionary<String, ?> config) throws ConfigurationException {
		log("ImperiHab Action: Configuration file is being parsed.");
		if (config != null) {
			log("ImperiHab Action: Configuration data exists. Parsing the paramaters.");
			
			// read config parameters here ...
			String defaultServer = (String) config.get("default");
			if (!StringUtils.isEmpty(defaultServer)) {
				imperiHab.defaultImperiHomeAddress = defaultServer;
				isProperlyConfigured = true;
			}else
				isProperlyConfigured = false;

		   for (Enumeration<String> e = config.keys(); e.hasMoreElements();)
			   log("ConfigKey: " + e.nextElement());
	

			log("defaultServer: " + defaultServer);
			log("isProperlyConfigured: " + isProperlyConfigured);
		}
	}
	
}
