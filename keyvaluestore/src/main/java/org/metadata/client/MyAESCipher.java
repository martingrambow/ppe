package main.java.org.metadata.client;

import java.security.MessageDigest;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import main.java.TimeKeeper;

/**
 * AES cipher with helping methods.
 */
public final class MyAESCipher {

	/**
	 * password used to genrate key.
	 */
	private String password;

	/**
	 * AES encryption key.
	 */
	private static SecretKeySpec secretKeySpec;

	/**
	 * AES cipher for encryptions.
	 */
	private static Cipher aesCipherEncrypt;

	/**
	 * AES cipher for decryptions.
	 */
	private static Cipher aesCipherDecrypt;

	/**
	 * Measures encryption time.
	 */
	private TimeKeeper encryptionTime = new TimeKeeper();

	/**
	 * Measures decryption time.
	 */
	private TimeKeeper decryptionTime = new TimeKeeper();

	/**
	 * Logger of this class.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(MyAESCipher.class);

	/**
	 * Initializes a AES Cipher.
	 * 
	 * @param password
	 *            - password to use.
	 */
	public MyAESCipher(final String password) {
		try {
			this.password = password;
			byte[] key = (this.password).getBytes("UTF-8");
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			key = md5.digest(key);
			// only use the first 128 bits 
			key = Arrays.copyOf(key, 16);
			secretKeySpec = new SecretKeySpec(key, "AES");

			aesCipherEncrypt = Cipher.getInstance("AES/ECB/PKCS5Padding");
			aesCipherEncrypt.init(Cipher.ENCRYPT_MODE, secretKeySpec);
			aesCipherDecrypt = Cipher.getInstance("AES/ECB/PKCS5Padding");
			aesCipherDecrypt.init(Cipher.DECRYPT_MODE, secretKeySpec);

		} catch (Exception e) {
			LOGGER.error(e.getLocalizedMessage(), e);
		}
	}

	/**
	 * Encrypts given String value.
	 * 
	 * @param plainText
	 *            - plain String
	 * @return encrypted String
	 */
	public byte[] encrypt(final String plainText) {
		try {
			encryptionTime.start();
			byte[] byteText = plainText.getBytes();
			byte[] byteCipherText = aesCipherEncrypt.doFinal(byteText);
			encryptionTime.stop();
			return byteCipherText;
		} catch (Exception e) {
			LOGGER.error(e.getLocalizedMessage(), e);
		}
		return null;
	}

	/**
	 * Decrypts given String value.
	 * 
	 * @param cipherText
	 *            - cipher String
	 * @return decrypted String
	 */
	public String decrypt(final byte[] cipherText) {
		try {
			decryptionTime.start();
			byte[] bytePlainText = aesCipherDecrypt.doFinal(cipherText);
			String decrypted = new String(bytePlainText);
			decryptionTime.stop();
			return decrypted;
		} catch (BadPaddingException e) {
			//LOGGER.error("Bad padding");
		} catch (Exception e) {
			LOGGER.error(e.getLocalizedMessage(), e);
		}
		return null;
	}

	/**
	 * Provides password used to generate key.
	 * 
	 * @return password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Provides the consumed encryption time.
	 * 
	 * @return encryption time in ms
	 */
	public long getEncryptionTime() {
		return encryptionTime.getDuration();
	}

	/**
	 * Provides the consumed decryption time.
	 * 
	 * @return decryption time in ms
	 */
	public long getDecryptionTime() {
		return decryptionTime.getDuration();
	}
}
