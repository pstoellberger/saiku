package org.saiku.web.rest.util;

import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.glassfish.jersey.servlet.WebComponent;

public class StartupResource {
	
	private static final Logger log = LoggerFactory.getLogger(StartupResource.class);
	
	public void init() {
		//com.sun.jersey.spi.container.servlet.WebComponent
		try {
			java.util.logging.Logger jerseyLogger = java.util.logging.Logger.getLogger(WebComponent.class.getName());
			if (jerseyLogger != null) {
				jerseyLogger.setLevel(Level.SEVERE);
				log.debug("Disabled INFO Logging for org.glassfish.jersey.servlet.WebComponent");
			} else {
				
			}
		} catch (Exception e) {
			log.error("Trying to disabling logging for org.glassfish.jersey.servlet.WebComponent INFO Output failed", e);
		}
	}

}
