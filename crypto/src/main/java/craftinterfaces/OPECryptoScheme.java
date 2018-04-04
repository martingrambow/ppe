package main.java.craftinterfaces;

/**
 * Interface for OPE cryptography schemes.
 */
public interface OPECryptoScheme {

	/**
	 * Encrypts the given value.
	 * 
	 * @param value
	 *            - value which shall be encrypted
	 * @return encrypted value or -1 in case of errors
	 */
	long encrypt(long value);
	
	/**
	 * Decrypts the given value with given key.
	 * 
	 * @param value
	 *            - value which shall be decrypted
	 * @return decrypted value or -1 in case of errors
	 */
	long decrypt(long value);
	
	/**
	 * Reference operation if no cryptosystem is used.
	 * 
	 * @param value
	 *            - value which shall be printed again
	 * @return value or -1 in case of errors
	 */
	long nocrypt(long value);
	
	
	/**
	 * Encrypts given String.
	 * 
	 * @param value
	 *            - value which shall be encrypted
	 * @return - encrypted result as String or null in case of errors
	 */
	String encryptString(String value);
	
	
	/**
	 * Decrypts given String.
	 * 
	 * @param value
	 *            - value which shall be decrypted
	 * @return - decrypted String or null in case of errors
	 */
	String decryptString(String value);	
	
	
	/**
	 * Reference operation for string en-/decryption.
	 * 
	 * @param value
	 *            - value which shall not be en-/decrypted
	 * @return - result as String or null in case of errors
	 */
	String nocryptString(String value);
	
}
