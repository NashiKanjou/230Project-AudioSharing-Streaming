package Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

/*Sample Server code from: https://medium.com/martinomburajr/java-create-your-own-hello-world-server-2ca33b6957e*/
public class Server implements Runnable {
	public static int port = 9991;
	public static boolean running = false;
	private Thread thread;
	private Terminal terminal;
	private static ServerSocket serverSocket;
	public static HashMap<Socket, List<String>> output_map = new HashMap<Socket, List<String>>();

	private void start() throws IOException {
		if (running) {
			return;
		}
		serverSocket = new ServerSocket(port);
		running = true;
		terminal = new Terminal(this);
		terminal.start();
		thread = new Thread(this);
		thread.start();
	}

	public void stop() {
		if (!running) {
			return;
		}
		running = false;
		// System.out.println("Server Stopping..");
		try {
			// thread.join();
			serverSocket.close();
			Thread.sleep(500);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	public void run() {
		System.out.println("Server Running..");
		try {
			enableConnections();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		Server server = new Server();
		try {
			server.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// add thread

	}

	private static byte[] intToByteArray(int i, int bitdepth) throws IOException {
		int b = bitdepth / 8;
		byte[] result = new byte[b];
		for (int a = 0; a < b; a++) {
			result[a] = (byte) (i >> (b - 1 - a) * 8);
		}
		result[0] = (byte) (i >> 8);
		result[1] = (byte) (i /* >> 0 */);
		/*
		 * byte[] result = new byte[4];
		 * 
		 * result[0] = (byte) (i >> 24); result[1] = (byte) (i >> 16); result[2] =
		 * (byte) (i >> 8); result[3] = (byte) (i );
		 */
		return result;

	}

	public static void enableConnections() throws IOException {

		while (running) {
			Socket socket = serverSocket.accept();
			// Create Input&Outputstreams for the connection
			DataInputStream inputToServer = new DataInputStream(socket.getInputStream());
			DataOutputStream outputFromServer = new DataOutputStream(socket.getOutputStream());
			Scanner scanner = new Scanner(inputToServer, "UTF-8");
			PrintWriter serverPrintOut = new PrintWriter(new OutputStreamWriter(outputFromServer, "UTF-8"), true);
			serverPrintOut.println("Hello World! Enter Peace to exit.");
			output_map.put(socket, new ArrayList<String>());
			Thread ComRThread = new Thread("Receive Thread") {
				@Override
				public void run() {
					boolean done = false;
					while (!done) {
						if (scanner.hasNextLine()) {
							String line = scanner.nextLine();
							System.out.println("Client: " + line);
							output_map.get(socket).add("Echo from <Your Name Here> Server: " + line);
							// serverPrintOut.println("Echo from <Your Name Here> Server: " + line);

							if (running && line.toLowerCase().trim().equals("start")) {

								try {
									File file = new File("test.wav");
									WavFile wavFile = WavFile.openWavFile(file);
									int[] data = new int[3840000 / 4];
									wavFile.readFrames(data, 480000);
									byte[] output = new byte[3840000];
									int count = 0;
									for (int d : data) {
										for (byte b : intToByteArray(d,wavFile.getValidBits())) {
											output[count] = b;
											count++;
										}
									}

									String format = "AUDIOFORMAT=" + wavFile.getValidBits() + ","
											+ wavFile.getSampleRate() + "," + wavFile.getNumChannels() + "," + 1 + ","
											+ 1;

									serverPrintOut.println(format);
									serverPrintOut.println("AUDIODATASTART");
									outputFromServer.write(output);

								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								} catch (WavFileException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}

							if (running && line.toLowerCase().trim().equals("peace")) {
								System.out.println("closed");
								done = true;
							}
						}
					}
					scanner.close();
				}
			};
			Thread ComSThread = new Thread("Send Thread") {
				@Override
				public void run() {
					while (!socket.isClosed()) {
						if (output_map.get(socket).size() > 0) {
							// System.out.println(outputlist.get(0));
							serverPrintOut.println(output_map.get(socket).get(0));
							output_map.get(socket).remove(0);
						}
					}
				}
			};
			ComRThread.start();
			ComSThread.start();

			// socket.close();
		}
		serverSocket.close();
	}
}
