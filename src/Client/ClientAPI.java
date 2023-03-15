package Client;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import audio.AudioPlayer;
import audio.AudioPlayerException;

public class ClientAPI {

	public static final String host1 = "";
	public static final int host1_port = 9991;
	public static final String host2 = "169.234.41.69";
	public static final int host2_port = 65400;

	public static ByteArrayOutputStream buffer;
	// private static boolean isPlaying;
	public static AudioPlayer player;

	public static Thread recieveThread;
	public static Thread sendThread;

	private static Socket server = null;
	private static DataInputStream inputToServer = null;
	private static DataOutputStream outputFromServer = null;
	private static PrintWriter serverPrintOut = null;

	private static boolean done = false;

	public static List<String> list_files = new ArrayList<String>();

	public static boolean isPlaying = false;

	public static void downloadFile(String filename, int piece, int chunksize) throws IOException {// untested
		FileOutputStream output = new FileOutputStream(filename, true);
		Map<Integer, byte[]> buffer = new HashMap<Integer, byte[]>();// save data that arrived that is not in order

		try {
			DataInputStream input = ClientAPI.getDataInputStream();
			int chunkcount = 0;
			while (chunkcount < piece) {
				if (buffer.containsKey(chunkcount)) {
					output.write(buffer.get(chunkcount));
					buffer.remove(chunkcount);
					chunkcount++;
					/*
					 * Request new chunk with the label chunkcount
					 * 
					 * 
					 * 
					 * 
					 * 
					 * 
					 * 
					 * 
					 * 
					 * 
					 * 
					 * 
					 */
					continue;
				}
				int count = 0;
				int stamp = -1;
				byte[] chunk = new byte[chunksize];
				while (count < chunksize) {
					chunk[count] = input.readByte();
					count++;
				}
				stamp = input.readInt();
				if (stamp == chunkcount) {
					output.write(chunk);
					chunkcount++;
				} else {
					buffer.put(stamp, chunk);
					/*
					 * Request new chunk with the label chunkcount
					 * 
					 * 
					 * 
					 * 
					 * 
					 * 
					 * 
					 * 
					 * 
					 * 
					 * 
					 * 
					 */
				}
			}
		} finally {
			output.close();
		}

	}

	public static void uploadFile() {

	}

	public static void sendMessage(String str) {
		getPrintWriter().println(str);
	}

	public static void sendMesssage(byte b[]) {
		try {
			getDataOutputStream().write(b);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void disconnect() throws IOException {
		getDataInputStream().close();
		getPrintWriter().close();
		getDataOutputStream().close();
		getServerSocket().close();
	}

	public static void connectServer() {
		connectServer(host1, host1_port);
	}

	public static void connectServer(String host, int port) {
		try {
			System.out.println("connecting to " + host);
			Socket server = new Socket(host, port);
			setServer(server);
			System.out.println("connected");
		} catch (Exception e) {
			System.out.println(e.toString());
			if (host.equals(host1)) {
				connectServer(host2, host2_port);
			} else {
				connectServer(host1, host1_port);
			}
		}
		recieveThread = new Thread("Receive Thread") {
			@Override
			public void run() {
				Scanner server_send = new Scanner(inputToServer, "UTF-8");
				while (!done) {
					if (server_send.hasNextLine()) {
						String line = server_send.nextLine();
						if (line.equalsIgnoreCase("filelistheader")) {
							list_files.clear();
							line = server_send.nextLine();
							while (!line.equalsIgnoreCase("filelistend")) {
								list_files.add(line);
								// System.out.println(line);
								line = server_send.nextLine();
							}
							// output to GUI?
						} else {
							// System.out.println(line);
						}
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
		sendThread = new Thread("Send Thread") {
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
		recieveThread.start();
		sendThread.start();
	}

	public static Socket getServerSocket() {
		return server;
	}

	public static void setServer(Socket server) throws IOException {
		ClientAPI.server = server;
		inputToServer = new DataInputStream(server.getInputStream());
		outputFromServer = new DataOutputStream(server.getOutputStream());
		serverPrintOut = new PrintWriter(new OutputStreamWriter(outputFromServer, "UTF-8"), true);
	}

	public static DataInputStream getDataInputStream() {
		return inputToServer;
	}

	public static DataOutputStream getDataOutputStream() {
		return outputFromServer;
	}

	public static PrintWriter getPrintWriter() {
		return serverPrintOut;
	}

}
