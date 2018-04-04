package main.java.org.metadata.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts and stops a Client-Server.
 */
public class POPEClientServer {

	/**
	 * Port of Client Server.
	 */
	private int port;

	/**
	 * Port of POPE-Server.
	 */
	private int popePort;

	/**
	 * Host of POPE-Server.
	 */
	private String popeHost;

	/**
	 * Password to genrate AES encryption key.
	 */
	private String password;

	/**
	 * Logger of this class.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(POPEClientServer.class);

	/**
	 * Process of this Client Server.
	 */
	private static Process clientServerProcess = null;

	/**
	 * Constructor.
	 * 
	 * @param popeHost
	 *            - hostadress on which the POPE Server is running.
	 * @param popePort
	 *            - port of POPE server
	 * @param port
	 *            - port on which oracle should be started
	 * @param password
	 *            - password to generate aes encryption key
	 */
	public POPEClientServer(final String popeHost, final int popePort, final int port, final String password) {
		this.popeHost = popeHost;
		this.popePort = popePort;
		this.port = port;
		this.password = password;
	}

	/**
	 * Starts Oracle.
	 */
	public void startClientServer() {
		String[] command = new String[6];
		command[0] = "python3";
		command[1] = "popeClientServer.py";
		command[2] = popeHost;
		command[3] = "" + popePort;
		command[4] = password;
		command[5] = "" + port;

		ProcessBuilder builder = new ProcessBuilder(command);

		File resourceFolder = new File("../resources/ope/pope/servers");
		if (!resourceFolder.exists()) {
			LOGGER.error("resouce folder " + resourceFolder.getAbsolutePath() + " not found.");
		}

		builder.directory(resourceFolder);
		try {
			clientServerProcess = builder.start();
			LOGGER.info("ClientServer process started, Client-Server ist listening on port " + port
					+ " and sends requests to " + popeHost + ":" + popePort + ".");
			Thread readOutpus1 = new Thread(new Runnable() {

				@Override
				public void run() {
					BufferedReader readerIn = new BufferedReader(
							new InputStreamReader(clientServerProcess.getInputStream()));
					String line = null;
					try {
						while ((line = readerIn.readLine()) != null) {
							switch (line) {
							case "waiting for a connection":
								break;
							case "got a connection":
								break;
							case "insert received":
								break;
							case "insert integer received":
								break;
							case "integer query received":
								break;
							case "range search received":
								break;
							case "query received":
								break;
							case "size request received":
								break;
							case "":
								break;
							default:
								if (line.contains("starting up on")) {
									break;
								}
								LOGGER.info(line);
								break;
							}
						}
					} catch (IOException e) {
						if (!e.getMessage().contains("Stream closed")) {
							LOGGER.error(e.getLocalizedMessage(), e);
						}
					} catch (Exception e) {
						LOGGER.error(e.getLocalizedMessage(), e);
					}
				}
			});
			readOutpus1.start();
			Thread readOutpus2 = new Thread(new Runnable() {

				@Override
				public void run() {
					BufferedReader readerErr = new BufferedReader(
							new InputStreamReader(clientServerProcess.getErrorStream()));
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
			LOGGER.info("Standard and error outputs are read from ClientServer process.");
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	/**
	 * Stops the process.
	 */
	public void stopClientServer() {
		if (clientServerProcess != null) {
			clientServerProcess.destroy();
			clientServerProcess = null;
			LOGGER.info("ClientServer process terminated.");
		}
	}
}
