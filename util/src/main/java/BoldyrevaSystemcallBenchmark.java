package main.java;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helperclass for Boldyreva Systemcall Benachmarking experiment.
 *
 */
public final class BoldyrevaSystemcallBenchmark {

	/**
	 * Logger of this class.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(BoldyrevaSystemcallBenchmark.class);
	
	/**
	 * Private default constructor.
	 */
	private BoldyrevaSystemcallBenchmark() {
		
	}		
	
	/**
	 * Starts experiment.
	 * @param args [0] - port, [1] - Start value, [2] - End value
	 */
	public static void main(final String[] args) {
		String key = args[0];
		int startvalue = Integer.parseInt(args[1]);
		int endvalue = Integer.parseInt(args[2]);
		
		long start = System.currentTimeMillis();
		
		for (int i = startvalue; i < endvalue; i++) {
			String[] answer = executePyScript(key, "" + i);
			for (String s : answer) {
				System.out.println(s);
			}
		}
		
		long end = System.currentTimeMillis();
		
		System.out.println("Duration = " + (end - start) + "ms");
	}
	
	/**
	 * Executes python benchmark script in the systemcall scenario .
	 * 
	 * @param key
	 *            - key used for en-/decryption
	 * @param value
	 *            - value to en-/decrypt
	 * 
	 * @return String[] with values or empty array if there was an error
	 */
	private static String[] executePyScript(final String key, final String value) {
		String[] command = new String[4];
		command[0] = "python";
		command[1] = "benchmarkSystemcall.py";
		command[2] = key;
		command[3] = value;
		
		ProcessBuilder builder = new ProcessBuilder(command);

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
				LOGGER.warn("result of script is empty");
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
		LOGGER.error("unable to run " + "benchmarkSystemcall" + " for value " + value + ".");
		return new String[0];
	}
}
