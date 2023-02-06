package Client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
	public static final String host1 = "";
	public static final int host1_port = 9991;
	public static final String host2 = "";
	public static final int host2_port = 9991;

	static boolean done = false;

	private static Socket server = null;
	private static DataInputStream inputToServer = null;
	private static DataOutputStream outputFromServer = null;
	private static PrintWriter serverPrintOut = null;

	public static Socket getServer() {
		return server;
	}

	static void setServer(Socket server) throws IOException {
		Client.server = server;
		inputToServer = new DataInputStream(server.getInputStream());
		outputFromServer = new DataOutputStream(server.getOutputStream());
		serverPrintOut = new PrintWriter(new OutputStreamWriter(outputFromServer, "UTF-8"), true);
	}

	static DataInputStream getDataInputStream() {
		return inputToServer;
	}

	static DataOutputStream getDataOutputStream() {
		return outputFromServer;
	}

	static PrintWriter getPrintWriter() {
		return serverPrintOut;
	}

	public static void main(String[] args) throws Exception {
		ClientAPI.connectServer(host1, host1_port);
		// connected

		Thread ComRThread = new Thread("Receive Thread") {
			@Override
			public void run() {
				Scanner server_send = new Scanner(inputToServer, "UTF-8");
				while (!done) {
					if (server_send.hasNextLine()) {
						String line = server_send.nextLine();
						System.out.println("Server: " + line);
					}
				}
				server_send.close();
				try {
					ClientAPI.disconnect();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		Thread ComSThread = new Thread("Send Thread") {
			@Override
			public void run() {
				Scanner keyboard = new Scanner(System.in, "UTF-8");
				while (!done) {
					if (keyboard.hasNextLine()) {
						String line = keyboard.nextLine();
						ClientAPI.sendMessage(line);
					}
				}
				keyboard.close();
			}
		};
		ComRThread.start();
		ComSThread.start();

	}

}