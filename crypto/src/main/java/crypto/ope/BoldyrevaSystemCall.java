package main.java.crypto.ope;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import main.java.craftinterfaces.OPECryptoScheme;

/**
 * Symetric OPE cryptosystem from Boldyreva.
 */
public class BoldyrevaSystemCall implements OPECryptoScheme {

	/**
	 * Logger of this class.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(BoldyrevaSystemCall.class);

	/**
	 * String used to generate the key.
	 */
	private String key = "Boldyreva";

	/**
	 * Creates new python session and generates cipher object.
	 * 
	 * @param key
	 *            - string used to generate key.
	 */
	public BoldyrevaSystemCall(final String key) {
		this.key = key;
	}

	/**
	 * Encrypts the given value.
	 * 
	 * @param value
	 *            - value which shall be encrypted
	 * @return encrypted value or -1 in case of errors
	 */
	public long encrypt(final long value) {
		String[] result = executePyScript("encrypt.py", key, "" + value);
		if (result.length == 1) {
			try {
				return Long.parseLong(result[0]);
			} catch (NumberFormatException e) {
				LOGGER.error(e.getLocalizedMessage(), e);
			}
		}
		LOGGER.error("Unable to encyrpt " + value);
		return -1;
	}
	
	/**
	 * Decrypts the given value with given key.
	 * 
	 * @param value
	 *            - value which shall be decrypted
	 * @return decrypted value or -1 in case of errors
	 */
	public long decrypt(final long value) {
		String[] result = executePyScript("decrypt.py", key, "" + value);
		if (result.length == 1) {
			try {
				return Long.parseLong(result[0]);
			} catch (NumberFormatException e) {
				LOGGER.error(e.getLocalizedMessage(), e);
			}
		}
		LOGGER.error("Unable to decyrpt " + value);
		return -1;
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
			if (encryptBlock) {
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
				
				//remove leading zeros
				while (blockAsString.startsWith("0")) {
					blockAsString = blockAsString.substring(1);
				}
				// encrypt block
				long encryptedLong = encrypt(Long.parseLong(blockAsString));
				//Fill up with zero until lenght=16
				String encryptedString = "" + encryptedLong;
				while (encryptedString.length() < 16) {
					encryptedString = "0" + encryptedString;
				}
				//split into 8 parts
				for (int i = 0; i < 8; i++) {
					String part = encryptedString.substring(i * 2, (i * 2) + 2);
					char c = (char) (Integer.parseInt(part) + 27);
					encryptedResult = encryptedResult + c;
				}
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
		
		int[] parts = new int[value.length()];
		for (int i = 0; i < value.length(); i++) {
			parts[i] = value.charAt(i) - 27;
		}
		
		String currentBlock = "";
		for (int i = 0; i < parts.length; i++) {
			if (parts[i] < 10) {
				currentBlock = currentBlock + "0";
			}
			currentBlock = currentBlock + parts[i];
			if (currentBlock.length() == 16) {
				//Current Block is complete to decryption
				while (currentBlock.startsWith("0")) {
					currentBlock = currentBlock.substring(1);
				}
				long encryptedBlockAsLong = Long.parseLong(currentBlock);
				String decryptedBlockAsString = "" + decrypt(encryptedBlockAsLong);
				
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
				currentBlock = "";
			}
		}
		return decrypted;
	}

	/**
	 * Reference operation if no cryptosystem is used.
	 * 
	 * @param value
	 *            - value which shall be printed again
	 * @return value or -1 in case of errors
	 */
	public long nocrypt(final long value) {
		String[] result = executePyScript("nocrypt.py", key, "" + value);
		if (result.length == 1) {
			try {
				return Long.parseLong(result[0]);
			} catch (NumberFormatException e) {
				LOGGER.error(e.getLocalizedMessage(), e);
			}
		}
		LOGGER.error("Unable to nocyrpt " + value);
		return -1;
	}

	/**
	 * Reference operation for string en-/decryption.
	 * 
	 * @param value
	 *            - value which shall not be en-/decrypted
	 * @return - result as String or null in case of errors
	 */
	public String nocryptString(final String value) {
		String[] result = executePyScript("nocryptString.py", key, value);
		if (result.length == 1) {
			return result[0];
		}
		LOGGER.error("Unable to nocyrpt " + value);
		return null;
	}

	/**
	 * Executes py-script and returns output as String[].
	 * 
	 * @param scriptname
	 *            - name of script in resource folder
	 * @param key
	 *            - key used for en-/decryption
	 * @param value
	 *            - value to en-/decrypt
	 * 
	 * @return String[] with values or empty array if there was an error
	 */
	private String[] executePyScript(final String scriptname, final String key, final String value) {
		String[] values = value.split(" ");
		String[] command = new String[3 + values.length];
		command[0] = "python";
		command[1] = scriptname;
		command[2] = key;
		for (int i = 3; i < command.length; i++) {
			command[i] = values[i - 3];
		}
		
		ProcessBuilder builder = new ProcessBuilder(command);
		String call = "python " + scriptname + " " + key + " " + value;

		File resourceFolder = new File("../resources/ope/boldyreva");
		if (!resourceFolder.exists()) {
			LOGGER.error("resouce folder " + resourceFolder.getAbsolutePath() + " not found.");
		}

		builder.directory(resourceFolder);
		ArrayList<String> resultList = new ArrayList<>();
		ArrayList<String> errorList = new ArrayList<>();
		try {
			Process p = builder.start();
			
			BufferedReader readerIn = new BufferedReader(new InputStreamReader(p.getInputStream()));
			BufferedReader readerErr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			String line = null;
			while ((line = readerIn.readLine()) != null) {
				resultList.add(line);
			}
			while ((line = readerErr.readLine()) != null) {
				errorList.add(line);
			}
			String[] result = new String[resultList.size()];
			for (int i = 0; i < result.length; i++) {
				result[i] = resultList.get(i);
			}
			if (result.length == 0) {
				LOGGER.warn("result of script is empty, script: " + call);
				if (errorList.size() > 0) {
					for (String e : errorList) {
						LOGGER.error(e);
					}
				}
			}
			return result;

		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}
		LOGGER.error("unable to run " + scriptname + " for value " + value + ".");
		return new String[0];
	}

}
