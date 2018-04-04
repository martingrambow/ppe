package main.java.org.metadata.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An object of this class can communicate with a POPE ClientServer on a given
 * Port.
 */
public class POPECommunicator {

	/**
	 * Socket for communication.
	 */
	private Socket socket = null;
	/**
	 * Outgoing stream.
	 */
	private PrintWriter outStream;
	/**
	 * Ingoing Stream.
	 */
	private BufferedReader inStream;
	
	/**
	 * Logger of this class.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(POPECommunicator.class);

	/**
	 * Constructor, creates streams to the given Port.
	 * 
	 * @param port
	 *            . Port to communicate with
	 */
	public POPECommunicator(final int port) {
		try {
			socket = new Socket("localhost", port);

			outStream = new PrintWriter(socket.getOutputStream(), true);
			inStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));

		} catch (UnknownHostException e) {
			LOGGER.error("Unknown host:", e);
		} catch (IOException e) {
			LOGGER.error("IO problems: ", e);
			if (e.getMessage().contains("Verbindungsaufbau abgelehnt")) {
				LOGGER.error("Adress is localhost:" + port);
			}
		}
	}

	/**
	 * Sends the given value to the given Port.
	 * 
	 * @param value
	 *            - value to send
	 * @return Answer of service
	 */
	public String[] sendText(final String value) {
		String[] answer = new String[1];
		try {
			outStream.println("" + value + "\n");
			outStream.flush();

			answer[0] = inStream.readLine();
			if (answer[0] == null) {
				LOGGER.error("answer was null, sent text: " + value);
			}
			if (value.equals("s")) {
				return answer;
			}
			String line;
			ArrayList<String> lines = new ArrayList<>();
			if (answer[0].endsWith("elements:")) {
				String[] parts = answer[0].split(" ");
				int count = Integer.parseInt(parts[3]);
				if (count == 0) {
					return answer;
				}
				while (count > 0) {
					line = inStream.readLine();
					lines.add(line);
					count = count - 1;
				}
			}
			return lines.toArray(answer);
		} catch (IOException e) {
			System.out.println("IO problems");
			e.printStackTrace();
		}
		return answer;
	}

	/**
	 * Closes the socket.
	 */
	public void closeSocket() {
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
				LOGGER.error("Unable to close socket: ", e);
			}
		}
	}
}
