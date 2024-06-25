package com.hoonit.xeye.util;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

public class ResourceBundleHandler {
	
	protected final Logger logger = Logger.getLogger(getClass().getName());
	
	private final String resourcePropertiesFile = "resource/conf/config";
	
    private ResourceBundle rscBundle;
    
    private static ResourceBundleHandler instance = new ResourceBundleHandler();

    private ResourceBundleHandler() {
        init();
    }
 
    private void init() {
    	rscBundle = ResourceBundle.getBundle(resourcePropertiesFile);
    }

    public static ResourceBundleHandler getInstance() {
    	
    	if(instance == null){
			instance = new ResourceBundleHandler();
		}
		
		return instance;
    }

  
    public ResourceBundle getResourceBundle() {
        return rscBundle;
    }

   
    public String getString(String pm_sKey) {
        String bundleString = null;

        try {
            bundleString = rscBundle.getString(pm_sKey);
        } catch (MissingResourceException e) {
        	logger.error("could not find resource class: " + resourcePropertiesFile);
        }

        return bundleString.trim();
    }
}
