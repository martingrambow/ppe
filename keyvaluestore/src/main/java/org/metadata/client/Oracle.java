package main.java.org.metadata.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts and stops an oracle.
 */
public class Oracle {

	/**
	 * Port of oracle.
	 */
	private int oraclePort;
	/**
	 * password to genrate AES encryption key.
	 */
	private String password;

	/**
	 * Host address of this oracle.
	 */
	private String host;

	/**
	 * Internal name of this oracle.
	 */
	private String name;

	/**
	 * Logger of this class.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(Oracle.class);

	/**
	 * Process of oracle.
	 */
	private Process oracleProcess = null;

	/**
	 * Time needed to partition.
	 */
	private double partitionTime = 0;

	/**
	 * Constructor.
	 * 
	 * @param host
	 *            - host address of this oracle
	 * @param port
	 *            - port on which oracle should be started
	 * @param password
	 *            - password to generate aes encryption key
	 * @param name
	 *            - internal name
	 */
	public Oracle(final String host, final int port, final String password, final String name) {
		this.oraclePort = port;
		this.password = password;
		this.name = name;
		this.host = host;
	}

	/**
	 * Starts Oracle.
	 */
	public void startOracle() {
		String[] command = new String[7];
		command[0] = "python3";
		command[1] = "orac_serv.py";
		command[2] = host;
		command[3] = "" + oraclePort;
		command[4] = password;
		// command[5] = "256";
		command[5] = "10000000";
		command[6] = "-d";

		ProcessBuilder builder = new ProcessBuilder(command);

		File resourceFolder = new File("../resources/ope/pope/servers");
		if (!resourceFolder.exists()) {
			LOGGER.error("resouce folder " + resourceFolder.getAbsolutePath() + " not found.");
		}

		builder.directory(resourceFolder);
		try {
			oracleProcess = builder.start();
			LOGGER.info("Oracle process started, oracle ist listening on port " + oraclePort);

			Thread readOutpus1 = new Thread(new Runnable() {

				@Override
				public void run() {
					BufferedReader readerIn = new BufferedReader(new InputStreamReader(oracleProcess.getInputStream()));
					String line = null;
					try {
						while ((line = readerIn.readLine()) != null) {
							if (line.startsWith("Finished in ")) {
								String[] parts = line.split(" ");
								double time = Double.parseDouble(parts[2]);
								partitionTime = partitionTime + time;
							} else {
								switch (line) {
								case "Received PARTITION request":
									break;
								case "Connection open":
									break;
								case "Press CTL-C to stop":
									break;
								case "Received MAX_SIZE request":
									break;
								default:
									if (!line.contains("The comparison oracle server is listening on")) {
										LOGGER.info(line);
									}
									break;
								}
							}
						}
					} catch (Exception e) {
						if (!e.getMessage().contains("Stream closed")) {
							LOGGER.error(e.getLocalizedMessage(), e);
						}
					}
				}
			});
			readOutpus1.start();
			Thread readOutpus2 = new Thread(new Runnable() {

				@Override
				public void run() {
					BufferedReader readerErr = new BufferedReader(
							new InputStreamReader(oracleProcess.getErrorStream()));
					String line = null;
					try {
						while ((line = readerErr.readLine()) != null) {
							LOGGER.error(line);
						}
					} catch (Exception e) {
						LOGGER.error(e.getLocalizedMessage(), e);
					}
				}
			});
			readOutpus2.start();
			LOGGER.info("Standard and error outputs are read from oracle process.");
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	/**
	 * Provides the internal name.
	 * 
	 * @return internal name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Stops the oracle process.
	 */
	public void stopOracle() {
		if (oracleProcess != null) {
			oracleProcess.destroy();
			oracleProcess = null;
			LOGGER.info("Oracle " + name + " terminated.");
			LOGGER.info("Partitiontime: " + partitionTime + "ms");
		}
	}
}
