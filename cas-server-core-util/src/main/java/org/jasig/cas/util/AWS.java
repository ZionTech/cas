
package org.jasig.cas.util;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wavity.kms.KeyManager;

/**
 * Used for encrypt/decrypt the keys.
 */
public class AWS
{
  // singleton instance.
  private final static AWS instance =
      new AWS();

  // logger. 
  /** Log instance for logging events, info, warnings, errors, etc. */
  private final Logger logger = LoggerFactory.getLogger(this.getClass());


  private KeyManager keymanager = null;



  /**
   * Returns the singleton instance.
   *
   * @return The singleton instance.
   * @throws InternalException
   *           In case of any errors.
   */
  public static AWS getAWSKeyManager()
  {
    if (instance != null)
    {
      instance.init();
    }
    return instance;
  }



  /**
   * Performs the initialization of the encryption/decryption service.
   *
   * @throws InternalException
   *           In case of any errors.
   */
  public synchronized void init() 
  {
	logger.info("Initializing Mail server");
    keymanager = new KeyManager();
  }



  /**
   * decryptes the properties in the properties file.
   * 
   * @param encryptedProperties The encrypted {@link Properties}.
   * @return The decrypted {@link Properties}.
   * @throws IOException In case of exceptions.
   */
  public Properties decryptProperties(
      final Properties encryptedProperties) throws IOException
  {
	logger.info("decrypting the properties {} ",
        encryptedProperties.toString());
    final Properties decryptedProperties = new Properties();
    final Iterator<Entry<Object, Object>> propertyIterator =
        encryptedProperties.entrySet().iterator();
    while (propertyIterator.hasNext())
    {
      final Entry<Object, Object> property = propertyIterator.next();
      logger.debug("decrypting property {}", property.toString());
      decryptedProperties.put(property.getKey(),
          decrypt((String) property.getValue()));
    }
    logger.info("completed decrypting th properties");
    logger.debug("completed decrypting th properties {}",
        decryptedProperties.toString());
    return decryptedProperties;
  }



  /**
   * encrypts the key.
   * 
   * @param key The key.
   * @return The encrypted value.
   * @throws IOException  In case of IO exception.
   */
  public String encrypt(final String key) throws IOException
  {
    return keymanager.encrypt(key);
  }



  /**
   * decrypts the key.
   * 
   * @param key The key.
   * @return The encrypted value.
   * @throws IOException  In case of IO exception.
   */
  public String decrypt(final String ciphertext) throws IOException
  {
    return keymanager.decrypt(ciphertext);
  }
}
