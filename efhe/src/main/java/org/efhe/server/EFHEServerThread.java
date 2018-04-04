package main.java.org.efhe.server;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import main.java.TimeKeeper;

/**
 * Thread of EFHE server.
 */
public class EFHEServerThread extends Thread {

	/**
	 * Server socket of server.
	 */
	private ServerSocket server;

	/**
	 * Working folder of this thread.
	 */
	private File workingFolder;

	/**
	 * Communicator to operation server.
	 */
	private ServerOperatorServiceCommunicator serverOperatorService;

	/**
	 * Number of bits to calculate.
	 */
	private int bits;

	/**
	 * Tells whether this server should run.
	 */
	private boolean running = false;

	/**
	 * Logger of this class.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(EFHEServerThread.class);	
	
	/**
	 * Starts Server on given port.
	 * 
	 * @param serverport
	 *            - port on which this server should listen
	 * @param serverOperatorPort
	 *            - port of operator service
	 * @param workingFolder
	 *            - working folder
	 * @param bits
	 *            - number of bits to calculate
	 */
	public EFHEServerThread(final int serverport, final int serverOperatorPort, final File workingFolder,
			final int bits) {
		this.workingFolder = workingFolder;
		this.bits = bits;
		try {
			server = new ServerSocket(serverport);
			server.setSoTimeout(3000);
			serverOperatorService = new ServerOperatorServiceCommunicator(serverOperatorPort);
		} catch (IOException e) {
			LOGGER.error("Error at creating server thread.", e);
		}
	}

	@Override
	public final void run() {
		deleteTmpFiles();
		running = true;
		while (running) {
			Socket client = null;
			try {
				client = server.accept();
				handleConnection(client);
			} catch (InterruptedIOException e1) {
			} catch (IOException e) {
				LOGGER.error("Error while accepting client.", e);
			} finally {
				if (client != null) {
					try {
						client.close();
					} catch (IOException e) {
						LOGGER.error("Error at closing client connection.", e);
					}
				}
			}
		}
	}

	/**
	 * Stops server thread.
	 */
	public void stopServer() {
		serverOperatorService.sendText("q");
		running = false;
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
		InputStream inputStream = client.getInputStream();
		PrintWriter out = new PrintWriter(client.getOutputStream(), true);

		String request = "";
		boolean end = false;
		while (!end) {
			char c = (char) inputStream.read();
			if (c != '\n') {
				request = request + c;
			} else {
				end = true;
			}
		}

		String command = request;
		if (request.contains(" ")) {
			command = request.substring(0, request.indexOf(" "));
		}
		LOGGER.info("Request: " + request);
		String answer = "";
		switch (command) {
		case "store":
			// Store a cipher text
			answer = handleStoreCommand(request, client.getInputStream());
			break;
		case "get":
			// Gets a cipher text
			handleGetCommand(request, out, client.getOutputStream());
			break;
		case "list":
			// lists all available cipher text
			handleListCommand(request, out);
			break;
		case "stats":
			// lists stats
			serverOperatorService.printStats(out);
			break;
		case "reset":
			// lists stats
			serverOperatorService.resetStats();
			break;
		case "copy":
			// copies a cipher text
			break;
		case "delete":
			// deletes a cipher text
			answer = handleDeleteCommand(request);
			break;
		case "add":
			// adds two cipher texts
			answer = handleAddCommand(request);
			break;
		case "sub":
			// subtracts two cipher texts
			answer = handleSubCommand(request, true);
			break;
		case "mul":
			// multiplies two cipher texts
			answer = handleMulCommand(request);
			break;
		case "div":
			// divides two cipher texts
			answer = handleDivCommand(request);
			break;
		default:
			answer = "Server: Unable to handle " + command;
			break;
		}
		if (answer.length() > 0) {
			out.println(answer);
		}
		inputStream.close();
		out.close();
	}

	/**
	 * Handles storage command.
	 * 
	 * @param request
	 *            - first request line
	 * @param in
	 *            - incoming connection
	 * @return - answer (success or error report)
	 */
	private String handleStoreCommand(final String request, final InputStream in) {
		TimeKeeper tkOperation = new TimeKeeper();
		tkOperation.start();
		String[] parts = request.split(" ");
		String fileName = parts[1];
		int fileSize = Integer.parseInt(parts[2]);

		File file = new File(workingFolder + "/files_s/", "ctx" + fileName + ".bin");
		try {
			file.createNewFile();
			FileOutputStream fileOutputStream = new FileOutputStream(file, false);
			
			DataInputStream dataIn = new DataInputStream(in);
			byte[] buffer = new byte[fileSize];
			dataIn.readFully(buffer);
			fileOutputStream.write(buffer, 0, fileSize);
			fileOutputStream.flush();
			fileOutputStream.close();
		} catch (IOException e) {
			LOGGER.error("Error at storing file ", e);
			return "Error at storing file";
		}
		tkOperation.stop();
		return "done in " + tkOperation.getDuration() + "ms.";
	}

	/**
	 * Handles get command.
	 * 
	 * @param request
	 *            - request line
	 * @param out
	 *            - outgoing connection
	 * @param outStream
	 *            - outgoing stream
	 */
	private void handleGetCommand(final String request, final PrintWriter out, final OutputStream outStream) {
		LOGGER.info("Try to handle get request:" + request);
		String[] parts = request.split(" ");
		String fileName = parts[1];

		try {
			File valueFile = new File(workingFolder.getAbsolutePath() + "/files_s/ctx" + fileName + ".bin");
			boolean exist = valueFile.exists();
			if (exist) {
				int fileLength = (int) valueFile.length();
				out.println("" + fileLength);
				FileInputStream fileInputStream = new FileInputStream(valueFile);
				BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
				byte[] buffer = new byte[fileLength];
				bufferedInputStream.read(buffer, 0, buffer.length);
				bufferedInputStream.close();
				
				DataOutputStream dataOut = new DataOutputStream(outStream);
				dataOut.write(buffer);
				dataOut.flush();
			} else {
				LOGGER.error("File " + valueFile.getAbsolutePath() + " does not exist.");
				out.println("Not available");
			}
		} catch (IOException e) {
			LOGGER.error("Error at sending file ", e);
			return;
		}
	}

	/**
	 * Handles get command.
	 * 
	 * @param request
	 *            - deletion request
	 * @return - answer (success or error report)
	 */
	private String handleDeleteCommand(final String request) {
		LOGGER.info("Try to handle deletion request:" + request);
		String[] parts = request.split(" ");
		String fileName = parts[1];

		if (fileName.contains("#")) {
			File file = new File(workingFolder + "/files_s/", "ctx" + fileName + ".bin");
			if (file.exists()) {
				file.delete();
			}
		} else {
			boolean available = true;
			int i = 0;
			while (available) {
				File file = new File(workingFolder + "/files_s/", "ctx" + fileName + "#" + i + ".bin");
				if (file.exists()) {
					file.delete();
					i++;
				} else {
					available = false;
				}
			}
		}
		return "done.";
	}

	/**
	 * Handles list command.
	 * 
	 * @param request
	 *            - list request
	 * @param out
	 *            - outgoing connection
	 */
	private void handleListCommand(final String request, final PrintWriter out) {
		LOGGER.info("Try to handle list request:" + request);
		ArrayList<String> names = getAvailableNames();
		for (String name : names) {
			out.println(name);
		}
	}

	/**
	 * Handles add command.
	 * 
	 * @param request
	 *            - addition request
	 * @return - answer (success or error report)
	 */
	private String handleAddCommand(final String request) {
		LOGGER.info("Try to handle addition request:" + request);
		TimeKeeper tkOperation = new TimeKeeper();
		tkOperation.start();
		String[] parts = request.split(" ");
		if (parts.length != 4) {
			return "Unable to add, check parameters.";
		}
		String first = parts[1];
		String second = parts[2];
		String result = parts[3];

		if (first.equals(result) || second.equals(result)) {
			return "Unable to add, result variable must be different from given variables.";
		}

		if (!isNameAvailable(first)) {
			return "Unable to add, " + first + " is not available on server.";
		}

		if (!isNameAvailable(second)) {
			return "Unable to add, " + second + " is not available on server.";
		}

		if (isNameAvailable(result)) {
			return "Unable to add, " + result + " already available on server, delete it first.";
		}
		// Perform bitwise addition
		for (int i = 0; i < bits; i++) {
			LOGGER.debug("Process: " + (i + 1) + "/" + bits + " bits");
			if (i == 0) {
				// Output
				serverOperatorService
						.sendText("XOR " + first + "#" + i + " " + second + "#" + i + " " + result + "#" + i);
				// Carry
				serverOperatorService.sendText("AND " + first + "#" + i + " " + second + "#" + i + " " + "tmp#0");
			} else {
				// Output 1. Step
				serverOperatorService
						.sendText("XOR " + first + "#" + i + " " + second + "#" + i + " " + result + "#" + i);
				// Carry 1.Step
				serverOperatorService.sendText("AND " + result + "#" + i + " " + "tmp#0" + " " + "tmp#1");
				// Output 2.Step
				serverOperatorService.sendText("XOR " + result + "#" + i + " " + "tmp#0" + " " + result + "#" + i);
				// Carry 2. Step
				serverOperatorService.sendText("AND " + first + "#" + i + " " + second + "#" + i + " " + "tmp#2");
				serverOperatorService.sendText("OR " + "tmp#1" + " " + "tmp#2" + " " + "tmp#0");
			}
		}
		deleteTmpFiles();
		tkOperation.stop();
		return "done in " + tkOperation.getDuration() + "ms.";
	}

	/**
	 * Handles sub(traction) command.
	 * 
	 * @param request
	 *            if (isNameAvailable(result)) { return "Unable to add, " +
	 *            result + " already available on server, delete it first."; }
	 *            // Perform bitwise addition - subtraction request
	 * @param deleteTmpAfterwards
	 *            - true, if tmp files should be deleted afterwards
	 * @return - answer (success or error report)
	 */
	private String handleSubCommand(final String request, final boolean deleteTmpAfterwards) {
		LOGGER.info("Try to handle subtraction request:" + request);
		TimeKeeper tkOperation = new TimeKeeper();
		tkOperation.start();
		String[] parts = request.split(" ");

		if (parts.length != 4) {
			return "Unable to subtract, check parameters.";
		}

		String first = parts[1];
		String second = parts[2];
		String result = parts[3];

		if (first.equals(result) || second.equals(result)) {
			return "Unable to subtract, result variable must be different from given variables.";
		}

		if (!isNameAvailable(first)) {
			return "Unable to subtract, " + first + " is not available on server.";
		}

		if (!isNameAvailable(second)) {
			return "Unable to subtract, " + second + " is not available on server.";
		}

		if (isNameAvailable(result)) {
			return "Unable to subtract, " + result + " already available on server, delete it first.";
		}
		// Perform bitwise subtraction
		for (int i = 0; i < bits; i++) {
			LOGGER.debug("Process: " + (i + 1) + "/" + bits + " bits");
			if (i == 0) {
				// Output
				serverOperatorService
						.sendText("XOR " + first + "#" + i + " " + second + "#" + i + " " + result + "#" + i);
				// Carry
				serverOperatorService.sendText("NOT " + first + "#" + i + " " + "tmp#0");
				serverOperatorService.sendText("AND " + second + "#" + i + " " + "tmp#0" + " " + "tmp#0");
			} else {
				// Output 1. step
				serverOperatorService
						.sendText("XOR " + first + "#" + i + " " + second + "#" + i + " " + result + "#" + i);
				// Carry 1. Step
				serverOperatorService.sendText("NOT " + result + "#" + i + " " + "tmp#1");
				serverOperatorService.sendText("AND " + "tmp#1" + " " + "tmp#0" + " " + "tmp#1");
				// Output 2. step
				serverOperatorService.sendText("XOR " + result + "#" + i + " " + "tmp#0" + " " + result + "#" + i);
				// Carry 2. step
				serverOperatorService.sendText("NOT " + first + "#" + i + " " + "tmp#2");
				serverOperatorService.sendText("AND " + "tmp#2" + " " + second + "#" + i + " " + "tmp#2");
				serverOperatorService.sendText("OR " + "tmp#1" + " " + "tmp#2" + " " + "tmp#0");
			}
		}
		if (deleteTmpAfterwards) {
			deleteTmpFiles();
		}
		tkOperation.stop();
		return "done in " + tkOperation.getDuration() + "ms.";
	}

	/**
	 * Handles mul(tiplication) command.
	 * 
	 * @param request
	 *            - multiplication request
	 * @return - answer (success or error report)
	 */
	private String handleMulCommand(final String request) {
		LOGGER.info("Try to handle multiplication request:" + request);
		TimeKeeper tkOperation = new TimeKeeper();
		tkOperation.start();
		String[] parts = request.split(" ");

		if (parts.length != 4) {
			return "Unable to multiplicate, check parameters.";
		}

		String first = parts[1];
		String second = parts[2];
		String result = parts[3];

		if (first.equals(result) || second.equals(result)) {
			return "Unable to multiplicate, result variable must be different from given variables.";
		}

		if (!isNameAvailable(first)) {
			return "Unable to multiplicate, " + first + " is not available on server.";
		}

		if (!isNameAvailable(second)) {
			return "Unable to multiplicate, " + second + " is not available on server.";
		}

		if (isNameAvailable(result)) {
			return "Unable to multiplicate, " + result + " already available on server, delete it first.";
		}
		// Perform bitwise multiplication

		for (int i = 0; i < bits; i++) {
			LOGGER.debug("Process: " + (i + 1) + "/" + bits + " bits");
			if (i == 0) {
				for (int j = 0; (j + i) < bits; j++) {
					serverOperatorService
							.sendText("AND " + first + "#" + j + " " + second + "#" + i + " " + result + "#" + j);
				}
			} else {
				for (int j = 0; (j + i) < bits; j++) {
					serverOperatorService
							.sendText("AND " + first + "#" + j + " " + second + "#" + i + " " + "tmp#" + (j + i));
					// Add to current result
					if (j == 0) {
						// carry
						serverOperatorService
								.sendText("AND " + result + "#" + (i + j) + " " + "tmp#" + (i + j) + " " + "tmp#c0");
						// Out
						serverOperatorService
								.sendText("XOR " + result + "#" + i + " " + "tmp#" + (i + j) + " " + result + "#" + i);
					} else {
						// carry
						serverOperatorService.sendText("XOR " + result + "#" + (i + j) + " " + "tmp#" + (i + j) + " " + "tmp#c1");
						serverOperatorService.sendText("AND " + "tmp#c1" + " " + "tmp#c0" + " " + "tmp#c1");

						serverOperatorService.sendText("AND " + result + "#" + (i + j) + " " + "tmp#" + (i + j) + " " + "tmp#c2");
						//EVTL OR?
						serverOperatorService.sendText("OR " + "tmp#c1" + " " + "tmp#c2" + " " + "tmp#c1");
						// Carry is in tmp#c1

						// Out
						serverOperatorService.sendText("XOR " + result + "#" + (i + j) + " " + "tmp#" + (i + j) + " " + result + "#" + (i + j));
						serverOperatorService.sendText("XOR " + result + "#" + (i + j) + " " + "tmp#c0" + " " + result + "#" + (i + j));

						// Copy carry to tmp#c0
						serverOperatorService.sendText("CPY " + "tmp#c1" + " " + "tmp#c0");
					}
				}
			}
		}
		deleteTmpFiles();
		tkOperation.stop();
		return "done in " + tkOperation.getDuration() + "ms.";
	}

	/**
	 * Handles div(ision) command.
	 * 
	 * @param request
	 *            - division request
	 * @return - answer (success or error report)
	 */
	private String handleDivCommand(final String request) {
		LOGGER.info("Try to handle division request:" + request);
		TimeKeeper tkOperation = new TimeKeeper();
		tkOperation.start();
		String[] parts = request.split(" ");

		if (parts.length != 5) {
			return "Unable to divide, check parameters.";
		}

		String first = parts[1];
		String second = parts[2];
		String result = parts[3];
		String remainder = parts[4];

		if (first.equals(result) || second.equals(result)) {
			return "Unable to divide, result variable must be different from given variables.";
		}

		if (first.equals(remainder) || second.equals(remainder)) {
			return "Unable to divide, remainder variable must be different from given variables.";
		}

		if (result.equals(remainder)) {
			return "Unable to divide, remainder variable must be different from result variable.";
		}

		if (!isNameAvailable(first)) {
			return "Unable to divide, " + first + " is not available on server.";
		}

		if (!isNameAvailable(second)) {
			return "Unable to divide, " + second + " is not available on server.";
		}

		if (isNameAvailable(result)) {
			return "Unable to divide, " + result + " already available on server, delete it first.";
		}

		if (isNameAvailable(remainder)) {
			return "Unable to divide, " + remainder + " already available on server, delete it first.";
		}
		// Perform bitwise division

		// Copy first to tmpx
		for (int i = 0; i < bits; i++) {
			serverOperatorService.sendText("CPY " + first + "#" + i + " " + "tmpx#" + i);
		}
		LOGGER.debug("-->tmpx generated.");

		// Extent tmpx with (bits-1) 0's to the left to be able to calculate
		// Generate encrypted 0
		serverOperatorService.sendText("CPY " + first + "#" + 0 + " " + "tmp#zero");
		serverOperatorService.sendText("XOR " + first + "#" + 0 + "tmp#zero" + " " + "tmp#zero");
		for (int i = bits; i < ((2 * bits) - 1); i++) {
			serverOperatorService.sendText("CPY " + "tmp#zero" + " " + "tmpx#" + i);
		}
		LOGGER.debug("-->tmpx extented.");

		for (int i = 0; i < bits; i++) {
			LOGGER.debug("Process: " + (i + 1) + "/" + bits + " bits");
			for (int j = 0; j < bits; j++) {
				// Generate numbers to compare by subtracting
				serverOperatorService.sendText("CPY " + "tmpx#" + (bits - 1 - i + j) + " " + "tmpaa#" + j);
				serverOperatorService.sendText("CPY " + second + "#" + j + " " + "tmpbb#" + j);
			}
			LOGGER.debug("-->numbers prepared for comparision.");

			// substract aa - bb, but do not delete tmp files
			String answer = handleSubCommand("sub tmpaa tmpbb tmpcc", false);
			if (!answer.startsWith("done")) {
				return "Unable to divide, comparision failed (" + answer + ").";
			}
			// Result is stored in tmp#0, copy it to tmpc1#0
			serverOperatorService.sendText("CPY " + "tmp#0" + " " + "tmpc1#0");

			// (tmpc1#0 == 1) -> aa < bb, else aa >= bb
			// neg(tmpc1#0) => result[bits-1]
			serverOperatorService.sendText("NOT tmpc1#0 tmpc1#0");
			serverOperatorService.sendText("CPY " + "tmpc1#0" + " " + result + "#" + (bits - 1 - i));
			LOGGER.debug("-->Comparison done, result saved.");

			// Multiply tmpc1#0 with second, store it in tmpy
			for (int j = 0; j < bits; j++) {
				serverOperatorService.sendText("AND " + second + "#" + j + " " + "tmpc1#0" + " " + "tmpy#" + j);
			}
			LOGGER.debug("-->tmpy generated.");

			// Subtract tmpaa - tmpy to generate the next tmpx for next
			// iteration
			answer = handleSubCommand("sub tmpaa tmpy tmpdd", false);
			if (!answer.startsWith("done")) {
				return "Unable to divide, comparision failed (" + answer + ").";
			}
			LOGGER.debug("-->tmpdd = tmpaa - tmpy done.");

			// Copy tmpdd to the correct tmpx positions
			for (int j = 0; j < bits; j++) {
				serverOperatorService.sendText("CPY " + "tmpdd#" + j + " " + "tmpx#" + (bits - 1 - i + j));
			}
			LOGGER.debug("-->Wrote tmpdd to tmpx.");

			// Delete tmpaa, tmpbb, tmpcc and tmpdd for next iteration
			handleDeleteCommand("delete tmpaa");
			handleDeleteCommand("delete tmpbb");
			handleDeleteCommand("delete tmpcc");
			handleDeleteCommand("delete tmpdd");
		}
		// Result is calculated, remainder is stored in tmpx
		for (int i = 0; i < bits; i++) {
			serverOperatorService.sendText("CPY " + "tmpx#" + i + " " + remainder + "#" + i);
		}

		deleteTmpFiles();
		tkOperation.stop();
		return "done in " + tkOperation.getDuration() + "ms.";
	}

	/**
	 * Provides all available variable names.
	 * 
	 * @return all available variable names
	 */
	private ArrayList<String> getAvailableNames() {
		ArrayList<String> names = new ArrayList<String>();
		File folder = new File(workingFolder + "/files_s/");
		for (File f : folder.listFiles()) {
			if (f.getName().contains("#")) {
				String name = f.getName().substring(3, f.getName().indexOf("#"));
				boolean found = false;
				for (String n : names) {
					if (n.equals(name)) {
						found = true;
					}
				}
				if (!found) {
					names.add(name);
				}
			}
		}
		return names;
	}

	/**
	 * Tells whether a variable name is available on server.
	 * 
	 * @param name
	 *            - given name
	 * @return true, if existing
	 */
	private boolean isNameAvailable(final String name) {
		for (String n : getAvailableNames()) {
			if (n.equals(name)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Deletes all tmp files in working folder.
	 */
	private void deleteTmpFiles() {
		File folder = new File(workingFolder + "/files_s/");
		for (File f : folder.listFiles()) {
			if (f.getName().startsWith("ctxtmp")) {
				f.delete();
			}
		}
	}
}
