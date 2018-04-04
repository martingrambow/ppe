package main.java.org.efhe.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An object of this class can communicate with a EFHE encryption server.
 */
public class ClientOperatorServiceCommunicator {

	/**
	 * Port for communication.
	 */
	private int port;
	
	/**
	 * Logger of this class.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(ClientOperatorServiceCommunicator.class);

	/**
	 * Constructor, creates streams to the given Port.
	 * 
	 * @param port
	 *            - Port to communicate with
	 */
	public ClientOperatorServiceCommunicator(final int port) {
		this.port = port;
	}

	/**
	 * Sends the given text to the given Port.
	 * 
	 * @param value
	 *            - value to send
	 * @return Answer of service
	 */
	public String sendText(final String value) {
		String answer = "error";
		try {
			Socket socket = new Socket("localhost", port);
			PrintWriter outStream = new PrintWriter(socket.getOutputStream(), true);
			BufferedReader inStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
						
			try {
				outStream.println("" + value);
				outStream.flush();
				answer = inStream.readLine();
				
				if (socket != null) {
					try {
						socket.close();
					} catch (IOException e) {
						LOGGER.error("Unable to close socket: ", e);
					}
				}
				
				if (answer == null) {
					LOGGER.error("answer was null, sent text: " + value);
					return "error";
				}
			} catch (IOException e) {
				LOGGER.error("IO Problems: ", e);
			}
		} catch (UnknownHostException e) {
			LOGGER.error("Unknown host:", e);
		} catch (IOException e) {
			LOGGER.error("IO problems: ", e);
			if (e.getMessage().contains("Verbindungsaufbau abgelehnt")) {
				LOGGER.error("Adress is localhost:" + port);
			}
		}
		return answer;
	}
}
