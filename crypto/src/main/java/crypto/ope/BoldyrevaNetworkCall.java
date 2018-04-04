package main.java.crypto.ope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import main.java.CryptoService;
import main.java.TimeKeeper;
import main.java.craftinterfaces.OPECryptoScheme;

/**
 * Symetric OPE cryptosystem from Boldyreva et al.
 */
public class BoldyrevaNetworkCall implements OPECryptoScheme {

	/**
	 * Logger of this class.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(BoldyrevaNetworkCall.class);
	
	/**
	 * Timerkeeper for conversion time.
	 */
	private static TimeKeeper conversionTime = new TimeKeeper();
	
	/**
	 * Timekeeper for compression time.
	 */
	private static TimeKeeper compressionTime = new TimeKeeper();
	
	/**
	 * Timekeeper for en-/decryption time.
	 */
	private static TimeKeeper realCryptoTime = new TimeKeeper();
	
	/**
	 * Service for encryption.
	 */
	private CryptoService encryptionService;
	
	/**
	 * Service for encryption.
	 */
	private CryptoService decryptionService;
	
	/**
	 * Reference service.
	 */
	private CryptoService nocryptionService;

	/**
	 * Creates new cipher object.
	 * 
	 * @param encrytionService - service for encryption
	 * @param decryptionService - service for decryption
	 * @param nocryptionService - reference service
	 */
	public BoldyrevaNetworkCall(final CryptoService encrytionService, final CryptoService decryptionService, final CryptoService nocryptionService) {
		this.encryptionService = encrytionService;
		this.decryptionService = decryptionService;
		this.nocryptionService = nocryptionService;
	}

	/**
	 * Encrypts the given value.
	 * 
	 * @param value
	 *            - value which shall be encrypted
	 * @return encrypted value or -1 in case of errors
	 */
	public final long encrypt(final long value) {
		realCryptoTime.start();
		long result = encryptionService.crypt(value);
		realCryptoTime.stop();
		return result;
	}

	/**
	 * Decrypts the given value with given key.
	 * 
	 * @param value
	 *            - value which shall be decrypted
	 * @return decrypted value or -1 in case of errors
	 */
	public long decrypt(final long value) {
		realCryptoTime.start();
		long result = decryptionService.crypt(value);
		realCryptoTime.stop();
		return result;
	}
	
	/**
	 * Reference operation if no cryptosystem is used.
	 * 
	 * @param value
	 *            - value which shall be printed again
	 * @return value or -1 in case of errors
	 */
	public long nocrypt(final long value) {
		realCryptoTime.start();
		long result = nocryptionService.crypt(value);
		realCryptoTime.stop();
		return result;
	}

	/**
	 * Encrypts given String.
	 * 
	 * @param value
	 *            - value which shall be encrypted
	 * @return - encrypted result as String or null in case of errors
	 */
	public String encryptString(final String value) {
		String encryptedResult = "";
		int pos = 0;
		String plain = "";
		while (pos < value.length()) {
			conversionTime.start();
			boolean encryptBlock = false;
			plain = plain + value.charAt(pos);
			if (plain.length() == 5) {
				encryptBlock = true;
			} else {
				if (pos == value.length() - 1) {
					encryptBlock = true;
				}
			}			
			pos++;
			conversionTime.stop();
			if (encryptBlock) {
				conversionTime.start();
				// transform to int
				String blockAsString = "";
				for (int i = 0; i < plain.length(); i++) {
					int c = plain.charAt(i);
					c = c - 31;
					if (c < 10) {
						blockAsString = blockAsString + "0";
					}
					blockAsString = blockAsString + c;
				}
				if (plain.length() < 5) {
					// incomplete block, add "00"
					while (blockAsString.length() < 10) {
						blockAsString = blockAsString + "00";
					}
				}

				// remove leading zeros
				while (blockAsString.startsWith("0")) {
					blockAsString = blockAsString.substring(1);
				}
				conversionTime.stop();
				// encrypt block
				long encryptedLong = encrypt(Long.parseLong(blockAsString));

				compressionTime.start();
				// Fill up with zero until lenght=16
				String encryptedString = "" + encryptedLong;
				while (encryptedString.length() < 16) {
					encryptedString = "0" + encryptedString;
				}
				// split into 8 parts
				for (int i = 0; i < 8; i++) {
					String part = encryptedString.substring(i * 2, (i * 2) + 2);
					char c = (char) (Integer.parseInt(part) + 27);
					encryptedResult = encryptedResult + c;
				}
				compressionTime.stop();
				// reset plain text
				plain = "";
			}
		}
		return encryptedResult;
	}

	/**
	 * Decrypts given String.
	 * 
	 * @param value
	 *            - value which shall be decrypted
	 * @return - decrypted String or null in case of errors
	 */
	public String decryptString(final String value) {
		String decrypted = "";
		compressionTime.start();
		int[] parts = new int[value.length()];
		for (int i = 0; i < value.length(); i++) {
			parts[i] = value.charAt(i) - 27;
		}
		compressionTime.stop();

		String currentBlock = "";
		for (int i = 0; i < parts.length; i++) {
			compressionTime.start();
			if (parts[i] < 10) {
				currentBlock = currentBlock + "0";
			}
			currentBlock = currentBlock + parts[i];
			compressionTime.stop();
			if (currentBlock.length() == 16) {
				compressionTime.start();
				// Current Block is complete to decryption
				while (currentBlock.startsWith("0")) {
					currentBlock = currentBlock.substring(1);
				}
				long encryptedBlockAsLong = Long.parseLong(currentBlock);
				compressionTime.stop();
				long decryptedBlockAsLong = decrypt(encryptedBlockAsLong);
				if (decryptedBlockAsLong == -1) {
					LOGGER.error("unable to decrypt " + value + ", current decrypted is: " + decrypted + ", currentBlockAsLong is " + encryptedBlockAsLong);
					return "";
				}
				conversionTime.start();
				String decryptedBlockAsString = "" + decryptedBlockAsLong;

				while (decryptedBlockAsString.length() < 10) {
					decryptedBlockAsString = "0" + decryptedBlockAsString;
				}

				for (int j = 0; j < decryptedBlockAsString.length(); j = j + 2) {
					String valueString = "" + decryptedBlockAsString.charAt(j) + decryptedBlockAsString.charAt(j + 1);
					int valueInteger = Integer.parseInt(valueString) + 31;
					// 99 asci-alphabet + 31 shift = 130
					// 00 (31) is special number to fill up blocks
					if (valueInteger != 31) {
						char decryptedChar = (char) valueInteger;
						decrypted = decrypted + decryptedChar;
					}
				}
				conversionTime.stop();
				currentBlock = "";
			}
		}
		return decrypted;
	}

	/**
	 * Reference operation for string en-/decryption.
	 * 
	 * @param value
	 *            - value which shall not be en-/decrypted
	 * @return - result as String or null in case of errors
	 */
	public String nocryptString(final String value) {
		nocryptionService.crypt(0);
		return value;
	}
	
	/**
	 * Provides the duration of encryptions.
	 * @return duration in ms.
	 */
	public static long getRealCryptionTime() {
		return realCryptoTime.getDuration();
	}
	
	/**
	 * Provides the duration of conversions.
	 * @return duration in ms.
	 */
	public static long getConversionTime() {
		return conversionTime.getDuration();
	}
	
	/**
	 * Provides the duration of compressions.
	 * @return duration in ms.
	 */
	public static long getCompressionTime() {
		return compressionTime.getDuration();
	}
}
