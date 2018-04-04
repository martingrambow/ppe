package main.java.org.efhe.client;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import main.java.BinDecConverter;
import main.java.TimeKeeper;

/**
 * Thread of EFHE server.
 */
public class EFHEClientThread extends Thread {

	/**
	 * Server socket of server.
	 */
	private ServerSocket client;

	/**
	 * Working folder of this thread.
	 */
	private File workingFolder;

	/**
	 * Number of bits in calculations.
	 */
	private int bits = 16;

	/**
	 * Client operator service communicator.
	 */
	private ClientOperatorServiceCommunicator clientOperatorService;

	/**
	 * Host of EFHE server.
	 */
	private String efheServerHost;
	
	/**
	 * Port number of EFHE server.
	 */
	private int efheServerPort;

	/**
	 * Tells whether this client should run.
	 */
	private boolean running = false;

	/**
	 * Logger of this class.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(EFHEClientThread.class);

	/**
	 * Starts Client on given port.
	 * 
	 * @param clientport
	 *            - Port on which the client should run
	 * @param clientOperatorServicePort
	 *            - Port of client operator service
	 *            @param efheServerHost - Host of EFHE server
	 * @param efheServerPort
	 *            - Port of EFHE server
	 * @param workingFolder
	 *            - working folder
	 * @param bits
	 *            - number of bits in calculation
	 */
	public EFHEClientThread(final int clientport, final int clientOperatorServicePort, final String efheServerHost, final int efheServerPort,
			final File workingFolder, final int bits) {
		this.workingFolder = workingFolder;
		this.bits = bits;
		this.efheServerHost = efheServerHost;
		this.efheServerPort = efheServerPort;
		try {
			client = new ServerSocket(clientport);
			client.setSoTimeout(3000);
			clientOperatorService = new ClientOperatorServiceCommunicator(clientOperatorServicePort);
		} catch (IOException e) {
			LOGGER.error("Error at creating server thread.", e);
		}
	}

	@Override
	public final void run() {
		running = true;
		while (running) {
			Socket clientSocket = null;
			try {
				clientSocket = client.accept();
				handleConnection(clientSocket);
			} catch (InterruptedIOException e1) {
			} catch (IOException e) {
				LOGGER.error("Error while accepting client.", e);
			} finally {
				if (clientSocket != null) {
					try {
						clientSocket.close();
					} catch (IOException e) {
						LOGGER.error("Error at closing client connection.", e);
					}
				}
			}
		}
	}

	/**
	 * Stops client thread.
	 */
	public void stopClient() {
		running = false;
		clientOperatorService.sendText("q");
	}

	/**
	 * Handles current connection.
	 * 
	 * @param client
	 *            - accepted client
	 * @throws IOException
	 *             - In case of Errors
	 */
	private void handleConnection(final Socket client) throws IOException {
		Scanner in = new Scanner(client.getInputStream());
		PrintWriter out = new PrintWriter(client.getOutputStream(), true);

		String request = in.nextLine();
		String[] command = request.split(" ");
		LOGGER.info("Request: " + request);
		switch (command[0]) {
		case "encrypt":
			// Encrypts given value
			handleEncrypt(request, out);
			break;
		case "decrypt":
			// Decrypts given value
			handleDecrypt(request, out);
			break;
		case "delete":
			handleDelete(request, out);
			// deletes given value
			break;
		case "add":
			// adds two values
			break;
		case "sub":
			// substracts two values
			break;
		case "mul":
			// multiplies two values
			break;
		case "div":
			// divides two values
			break;
		default:
			out.println("Client: Unable to handle " + command[0] + ".");
			break;
		}
		in.close();
		out.close();
	}

	/**
	 * Handles encryptions request.
	 * 
	 * @param request
	 *            - incoming request
	 * @param out
	 *            - outgoing stream
	 */
	private void handleEncrypt(final String request, final PrintWriter out) {
		String[] parts = request.split(" ");
		TimeKeeper tkOperation = new TimeKeeper();
		tkOperation.start();
		try {
			int number = Integer.parseInt(parts[1]);
			String name = parts[2];
			if (name.contains("#")) {
				out.println("# is not allowed in variable name.");
				return;
			}
			int[] bin = BinDecConverter.decToBin(number, bits);
			String binString = "";
			int opTime = 0;
			int parseTime = 1;
			for (int i = 0; i < bin.length; i++) {
				binString = bin[i] + binString;
				String answer = clientOperatorService.sendText("e " + bin[i] + " " + name + "#" + i);
				if (!answer.startsWith("Done")) {
					LOGGER.error("Unable to encrypt.");
					out.println("Unable to encrypt.");
					return;
				} else {
					String[] aparts = answer.split(":");
					opTime = opTime + Integer.parseInt(aparts[2]);
					parseTime = parseTime + Integer.parseInt(aparts[3]) - Integer.parseInt(aparts[2]);
				}
				storeValueOnOperationServer(name + "#" + i);
			}
			tkOperation.stop();
			out.println("done (" + binString + ")" + " in " + tkOperation.getDuration() + "ms (" + opTime + ":"
					+ parseTime + ")");
		} catch (NumberFormatException ne) {
			out.println("Unable to parse " + parts[1] + " to integer.");
		}
	}

	/**
	 * Handles decryptions request.
	 * 
	 * @param request
	 *            - incoming request
	 * @param out
	 *            - outgoing stream
	 */
	private void handleDecrypt(final String request, final PrintWriter out) {
		String[] parts = request.split(" ");
		TimeKeeper tkOperation = new TimeKeeper();
		tkOperation.start();

		String name = parts[1];
		if (name.contains("#")) {
			out.println("# is not allowed in variable name.");
			return;
		}
		int opTime = 0;
		int parseTime = 0;
		int[] bin = new int[bits];
		for (int i = 0; i < bin.length; i++) {
			if (!getValueFromEFHEServer(name + "#" + i)) {
				out.println("Unable to decrypt, request was " + request);
				return;
			}
			String answer = clientOperatorService.sendText("d " + name + "#" + i);
			try {
				String[] aparts = answer.split(":");
				int bit = Integer.parseInt(aparts[0]);
				bin[i] = bit;
				opTime = opTime + Integer.parseInt(aparts[3]);
				parseTime = parseTime + (Integer.parseInt(aparts[4]) - Integer.parseInt(aparts[3]));
			} catch (NumberFormatException ne) {
				out.println("Unable to parse " + answer + " to integer.");
			}
			deleteFile(name + "#" + i);
		}
		String binString = "";
		for (int i = 0; i < bits; i++) {
			binString = bin[i] + binString;
		}
		tkOperation.stop();
		out.println(BinDecConverter.binToDec(bin) + " (" + binString + ")" + " in " + tkOperation.getDuration() + "ms (" + opTime + ":" + parseTime + ")");
	}

	/**
	 * Handles deletion request.
	 * 
	 * @param request
	 *            - incoming request
	 * @param out
	 *            - outgoing stream
	 */
	private void handleDelete(final String request, final PrintWriter out) {
		String[] parts = request.split(" ");
		String name = parts[1];

		try {
			Socket efheServer = new Socket(efheServerHost, efheServerPort);
			PrintWriter outStream = new PrintWriter(efheServer.getOutputStream(), true);
			BufferedReader inStream = new BufferedReader(new InputStreamReader(efheServer.getInputStream()));
			outStream.println("delete " + name);
			String answer = inStream.readLine();
			if (!"done.".equals(answer)) {
				LOGGER.error("Unable to delete " + name + ". Answer from EFHE server was " + answer);
				out.println("Unable to delete " + name + ". Answer from EFHE server was " + answer);
			} else {
				out.println("done.");
			}
			inStream.close();
			outStream.close();
			efheServer.close();
		} catch (IOException e) {
			LOGGER.error("Unable to delete " + name + ". Request was " + request);
		}
	}

	/**
	 * Stores a given value on EFHE server.
	 * 
	 * @param value
	 *            - value to store
	 */
	private void storeValueOnOperationServer(final String value) {
		try {
			Socket efheServer = new Socket(efheServerHost, efheServerPort);
			PrintWriter outStream = new PrintWriter(efheServer.getOutputStream(), true);
			BufferedReader inStream = new BufferedReader(new InputStreamReader(efheServer.getInputStream()));

			File valueFile = new File(workingFolder.getAbsolutePath() + "/files_c/ctx" + value + ".bin");
			long fileLength = valueFile.length();
			outStream.println("store " + value + " " + fileLength);

			FileInputStream fileInputStream = new FileInputStream(valueFile);
			BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
			byte[] buffer = new byte[(int) fileLength];
			bufferedInputStream.read(buffer, 0, buffer.length);
			bufferedInputStream.close();
			
			DataOutputStream dataOut = new DataOutputStream(efheServer.getOutputStream());
			dataOut.write(buffer);
			dataOut.flush();
			
			String answer = inStream.readLine();
			if (!answer.startsWith("done")) {
				LOGGER.error("Unable to store value " + value + ": " + answer);
			} else {
				// Delete file on client if successful
				valueFile.delete();
			}
			efheServer.close();
			return;
		} catch (IOException e) {
			LOGGER.error("Unable to connect to port " + efheServerPort);
			return;
		}
	}

	/**
	 * Gets a given value from EFHE server.
	 * 
	 * @param value
	 *            - value to get
	 * @return true, if successful
	 */
	private boolean getValueFromEFHEServer(final String value) {
		try {
			Socket efheServer = new Socket(efheServerHost, efheServerPort);
			PrintWriter outStream = new PrintWriter(efheServer.getOutputStream(), true);
			InputStream inStream = efheServer.getInputStream();

			outStream.println("get " + value);
			File valueFile = new File(workingFolder.getAbsolutePath() + "/files_c/ctx" + value + ".bin");
			try {
				String size = "";
				boolean end = false;
				while (!end) {
					char c = (char) inStream.read();
					if (c != '\n') {
						size = size + c;
					} else {
						end = true;
					}
				}
				int fileSize = Integer.parseInt(size);
				valueFile.createNewFile();
				FileOutputStream fileOutputStream = new FileOutputStream(valueFile, false);
				byte[] buffer = new byte[fileSize];
				DataInputStream dataIn = new DataInputStream(inStream);
				dataIn.readFully(buffer);
				fileOutputStream.write(buffer, 0, fileSize);
				fileOutputStream.flush();
				fileOutputStream.close();
				efheServer.close();
			} catch (IOException e) {
				LOGGER.error("Error at storing file ", e);
				return false;
			}
		} catch (IOException e) {
			LOGGER.error("Unable to connect to port " + efheServerPort);
			return false;
		}
		return true;
	}

	/**
	 * Deletes a given file.
	 * 
	 * @param name
	 *            - name of file
	 */
	private void deleteFile(final String name) {
		File valueFile = new File(workingFolder.getAbsolutePath() + "/files_c/ctx" + name + ".bin");
		valueFile.delete();
	}

}
