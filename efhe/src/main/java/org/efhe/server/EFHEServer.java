package main.java.org.efhe.server;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.Socket;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts and stops an EFHE server.
 */
public class EFHEServer {

	/**
	 * Port of EFHE Server operator service.
	 */
	private int serverOperatorPort;

	/**
	 * Location/folder of operation server.
	 */
	private File workingFolder;

	/**
	 * Port on which this EFHE server should start.
	 */
	private int efhePort;

	/**
	 * Bits to calculate.
	 */
	private int bits;

	/**
	 * Thread of server.
	 */
	private EFHEServerThread thread;

	/**
	 * Logger of this class.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(EFHEServer.class);

	/**
	 * Starts this server and a operation server.
	 * 
	 * @param args
	 *            - args for program
	 */
	public static void main(final String[] args) {
		File folder = new File("../resources/FHE/FHEW/srv");
		int port = Integer.parseInt(args[0]);
		int serverOperatorPort = port + 1;
		int bits = Integer.parseInt(args[1]);

		EFHEServer server = new EFHEServer(folder, port, serverOperatorPort, bits);

		server.startServerOperatorService(true);

		boolean connected = server.waitForServerOperatorService(120);
		if (connected) {
			server.startServer();
		} else {
			LOGGER.info("unable to connect to operation server.");
		}

		Scanner in = new Scanner(System.in);
		in.nextLine();
		in.close();
		server.stopServer();
	}

	/**
	 * Constructor.
	 * 
	 * @param workingFolder
	 *            - Folder of EFHE_Operation server
	 * @param efhePort
	 *            - Port on which the efhe server should run
	 * @param serverOperatorPort
	 *            - Port on which the operator service should run
	 * @param bits
	 *            - bits to calculate
	 */
	public EFHEServer(final File workingFolder, final int efhePort, final int serverOperatorPort, final int bits) {
		this.workingFolder = workingFolder;
		this.efhePort = efhePort;
		this.serverOperatorPort = serverOperatorPort;
		this.bits = bits;
	}

	/**
	 * Starts server thread.
	 */
	public void startServer() {
		thread = new EFHEServerThread(efhePort, serverOperatorPort, workingFolder, bits);
		thread.start();
	}

	/**
	 * Stops server.
	 */
	public void stopServer() {
		if (thread != null) {
			thread.stopServer();
		}
	}

	/**
	 * Starts operation server Server.
	 * 
	 * @param debug
	 *            - true, to print detailed information
	 */
	private void startServerOperatorService(final boolean debug) {
		String programm = "FHEW_ServerOperator.sh";
		String programmPath = workingFolder.getAbsolutePath();

		String[] command = new String[5];
		command[0] = "/bin/bash";
		command[1] = programmPath + "/" + programm;
		command[2] = "" + serverOperatorPort;
		command[3] = "evalKey.bin";
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
			LOGGER.info("Server operator service process started, service listening on port " + serverOperatorPort);
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	/**
	 * Waits for operator service to startup.
	 * 
	 * @param timeoutSec
	 *            - timeout in seconds
	 * @return true, if available
	 */
	private boolean waitForServerOperatorService(final int timeoutSec) {

		int tries = 0;
		while (tries < timeoutSec) {
			try {
				Thread.sleep(1000);
				Socket socket = new Socket("localhost", serverOperatorPort);
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
