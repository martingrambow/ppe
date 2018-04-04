package main.java.org.efhe.client;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.Socket;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts and stops an EFHE client.
 */
public class EFHEClient {

	/**
	 * Port of client operation service.
	 */
	private int clientOperationServicePort;

	/**
	 * Port of EFHE server.
	 */
	private int efheServerPort;

	/**
	 * Location/folder of operation server.
	 */
	private File workingFolder;

	/**
	 * Number of bits in calculations.
	 */
	private int bits = 16;

	/**
	 * Host of EFHE server.
	 */
	private String efheServerHost;

	/**
	 * Port on which this EFHE client should start.
	 */
	private int efhePort;

	/**
	 * Thread of server.
	 */
	private EFHEClientThread thread;

	/**
	 * Logger of this class.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(EFHEClient.class);

	/**
	 * Starts this server and an operation service.
	 * 
	 * @param args
	 *            - arguments of program
	 */
	public static void main(final String[] args) {
		File folder = new File("../resources/FHE/FHEW/srv");
		int port = Integer.parseInt(args[0]);
		int servicePort = port + 1;
		String efheServerhost = args[1];
		int efheServerPort = Integer.parseInt(args[2]);
		int bits = Integer.parseInt(args[3]);

		EFHEClient client = new EFHEClient(folder, port, servicePort, efheServerhost, efheServerPort, bits);

		client.startOperationService(true);
		client.waitForOperationService(120);
		client.startClient(true);

		LOGGER.info("EFHE client available on port " + port + ".");
		LOGGER.info("Press Enter to quit EFHE client.");

		Scanner in = new Scanner(System.in);
		in.nextLine();
		in.close();
		client.stopClient();
	}

	/**
	 * Constructor.
	 * 
	 * @param workingFolder
	 *            - Folder of EFHE_Operation server
	 * @param efhePort
	 *            - Port on which this client should run
	 * @param servicePort
	 *            - Port on which the operation service should run
	 * @param efheServerHost
	 *            - host of EFHE server
	 * @param efheServerPort
	 *            - Port of EFHE server
	 * @param bits
	 *            - number of bits in calculations
	 */
	public EFHEClient(final File workingFolder, final int efhePort, final int servicePort, final String efheServerHost,
			final int efheServerPort, final int bits) {
		this.workingFolder = workingFolder;
		this.efhePort = efhePort;
		this.clientOperationServicePort = servicePort;
		this.efheServerHost = efheServerHost;
		this.efheServerPort = efheServerPort;
		this.bits = bits;
	}

	/**
	 * Starts client thread.
	 * 
	 * @param debug
	 *            true, to print debugging information
	 */
	public void startClient(final boolean debug) {
		thread = new EFHEClientThread(efhePort, clientOperationServicePort, efheServerHost, efheServerPort,
				workingFolder, bits);
		thread.start();
	}

	/**
	 * Stops client.
	 */
	public void stopClient() {
		if (thread != null) {
			thread.stopClient();
		}
	}

	/**
	 * Starts encryption service.
	 * 
	 * @param debug
	 *            - true, to print detailed information
	 */
	private void startOperationService(final boolean debug) {
		String programm = "FHEW_ClientOperator.sh";
		String programmPath = workingFolder.getAbsolutePath();

		String[] command = new String[5];
		command[0] = "/bin/bash";
		command[1] = programmPath + "/" + programm;
		command[2] = "" + clientOperationServicePort;
		command[3] = "secKey.bin";
		command[4] = "0";
		if (debug) {
			command[4] = "1";
		}
		ProcessBuilder builder = new ProcessBuilder(command);

		if (!workingFolder.exists()) {
			LOGGER.error("resouce folder " + workingFolder.getAbsolutePath() + " not found.");
		}
		builder.directory(workingFolder);
		builder.redirectOutput(Redirect.INHERIT);
		builder.redirectError(Redirect.INHERIT);
		try {
			builder.start();
			LOGGER.info(
					"Client operator service process started, service listening on port " + clientOperationServicePort);
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	/**
	 * Waits for operation service to startup.
	 * 
	 * @param timeoutSec
	 *            - timeout in seconds
	 * @return true, if available
	 */
	private boolean waitForOperationService(final int timeoutSec) {

		int tries = 0;
		while (tries < timeoutSec) {
			try {
				Thread.sleep(1000);
				Socket socket = new Socket("localhost", clientOperationServicePort);
				socket.close();
				return true;
			} catch (IOException e1) {
				tries++;
			} catch (InterruptedException e2) {
				LOGGER.info("");
				e2.printStackTrace();
			}
		}
		return false;
	}
}
