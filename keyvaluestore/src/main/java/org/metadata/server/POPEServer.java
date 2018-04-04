package main.java.org.metadata.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts and stops an POPE Server.
 */
public class POPEServer {

	/**
	 * Hostname of oracle.
	 */
	private String oracleHost;

	/**
	 * Port of oracle.
	 */
	private int oraclePort;
	
	/**
	 * Host of this server.
	 */
	private String popeHost;

	/**
	 * Port of POPE server.
	 */
	private int popePort;

	/**
	 * Internal name of POPE server.
	 */
	private String name;

	/**
	 * Logger of this class.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(POPEServer.class);

	/**
	 * Process of pope server.
	 */
	private Process popeProcess = null;

	/**
	 * Counts the number of inserts.
	 */
	private int inserts = 0;
	/**
	 * Counts the number of range queries.
	 */
	private int selects = 0;

	/**
	 * Measures time for inserts.
	 */
	private double insertTime = 0;

	/**
	 * Measures time for queries.
	 */
	private double queryTime = 0;

	/**
	 * Constructor.
	 * 
	 * @param oracleHost
	 *            - Hostname of oracle
	 * @param oraclePort
	 *            - port of oracle
	 * @param popeHost
	 *            - Host of this server
	 * @param popePort
	 *            - Port on which this pope server should be started
	 * @param name
	 *            - internal name
	 */
	public POPEServer(final String oracleHost, final int oraclePort, final String popeHost, final int popePort,
			final String name) {
		this.oracleHost = oracleHost;
		this.oraclePort = oraclePort;
		this.popeHost = popeHost;
		this.popePort = popePort;
		this.name = name;
	}

	/**
	 * Starts Pope Server.
	 */
	public void startPopeServer() {
		String[] command = new String[7];
		command[0] = "python3";
		command[1] = "pope_serv.py";
		command[2] = oracleHost;
		command[3] = "" + oraclePort;
		command[4] = popeHost;
		command[5] = "" + popePort;
		command[6] = "-d";

		ProcessBuilder builder = new ProcessBuilder(command);

		File resourceFolder = new File("../resources/ope/pope/servers");
		if (!resourceFolder.exists()) {
			LOGGER.error("resouce folder " + resourceFolder.getAbsolutePath() + " not found.");
		}

		builder.directory(resourceFolder);
		try {
			popeProcess = builder.start();
			LOGGER.info("Pope server process started, " + name + "-server ist listening on port " + popePort);
			LOGGER.info("; Name; Kind; Count; Time");
			Thread readOutpus1 = new Thread(new Runnable() {

				@Override
				public void run() {
					BufferedReader readerIn = new BufferedReader(new InputStreamReader(popeProcess.getInputStream()));
					String line = null;
					int state = 0;
					try {
						boolean logInsert = false;
						boolean logQuery = false;
						while ((line = readerIn.readLine()) != null) {
							switch (line) {
							case "Press CTL-C to stop":
								break;
							case "Received CLEAR request":
								break;
							case "Connection open":
								break;
							case "Received INSERT request":
								state = 1;
								inserts++;
								logInsert = true;
								break;
							case "Received RANGE_SEARCH request":
								state = 2;
								selects++;
								logQuery = true;
								break;
							case "":
								break;
							default:
								if (line.contains("The POPE server is listening")) {
									break;
								}
								if (line.startsWith("Finished in ")) {
									String[] parts = line.split(" ");
									double time = Double.parseDouble(parts[2]);
									if (state == 1) {
										insertTime = insertTime + time;
									}
									if (state == 2) {
										queryTime = queryTime + time;
									}
									break;
								} else {
									LOGGER.info(line);
								}
								break;
							}
							if (logInsert) {
								if (inserts % 500 == 0) {
									LOGGER.info("; " + name + "; I; " + inserts + "; " + insertTime);
								}
								logInsert = false;
							}
							if (logQuery) {
								if (selects % 50 == 0) {
									LOGGER.info(";" + name + "; Q; " + selects + "; " + queryTime);
								}
								logQuery = false;
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
					BufferedReader readerErr = new BufferedReader(new InputStreamReader(popeProcess.getErrorStream()));
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
			LOGGER.info("Standard and error outputs are read from POPE server process.");
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	/**
	 * Stops the oracle process.
	 */
	public void stopPopeServer() {
		if (popeProcess != null) {
			popeProcess.destroy();
			popeProcess = null;
			LOGGER.info("POPE " + name + "-server process terminated.");
		}
	}
}
