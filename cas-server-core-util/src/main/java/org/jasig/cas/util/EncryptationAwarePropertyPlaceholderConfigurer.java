/*******************************************************************************
 * Copyright (c) 2017, Wavity Inc. and/or its affiliates. All rights reserved.
 *******************************************************************************/

package org.jasig.cas.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

public class EncryptationAwarePropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer{

	/** Log instance for logging events, info, warnings, errors, etc. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Override
	protected String convertPropertyValue(String originalValue) {
	        return decrypt(originalValue);
	}
	
    /**
     * Decryption the given value using KeyManagemtn mechanism   
     * 
     * @param ciphertext
     * @return The decrypted value.
     */
    private String decrypt(String ciphertext) 
	{
    	try {
    		ciphertext = AWS.getAWSKeyManager().decrypt(ciphertext);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error occured while decripting the give value {}", e);
		}
	    return ciphertext;
	}
}