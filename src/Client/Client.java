package Client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

import audio.AudioPlayerException;

public class Client {
	public static final String host1 = "";
	public static final int host1_port = 9991;
	public static final String host2 = "";
	public static final int host2_port = 9991;

	private static final String audioFormatHeader = "AUDIOFORMAT=";
	private static final String audioDataHeader = "AUDIODATASTART";
	public static final int single_transfer_size = 960000;// testing

	public static final byte stop = 10;

	static boolean done = false;

	private static Socket server = null;
	private static DataInputStream inputToServer = null;
	private static DataOutputStream outputFromServer = null;
	private static PrintWriter serverPrintOut = null;

	public static int sampleRate = 8000;
	public static int bit = 16;
	public static int channels = 2;
	public static boolean bigEndian = true;
	public static boolean signed = true;

	public static void main(String[] args) {
		//try catch the whole main for reconnect to another server? 
		ClientAPI.connectServer(host1, host1_port);// this will try to connect to second server if the first one time out
		// connected

		Thread ComRThread = new Thread("Receive Thread") {
			@Override
			public void run() {
				Scanner server_send = new Scanner(inputToServer, "UTF-8");
				ClientAPI.createAudioBuffer(sampleRate, bit, channels, true, true);
				while (!done) {
					if (server_send.hasNextLine()) {
						String line = server_send.nextLine();
						if (line.startsWith(audioFormatHeader)) {
							line = line.substring(12);
							String[] f = line.split(",");
							bit = Integer.parseInt(f[0]);
							sampleRate = Integer.parseInt(f[1]) * 2;
							channels = Integer.parseInt(f[2]);
							if (Integer.parseInt(f[3]) == 1) {
								signed = true;
							} else {
								signed = false;
							}
							if (Integer.parseInt(f[4]) == 1) {
								bigEndian = true;
							} else {
								bigEndian = false;
							}
							/*
							 * System.out.println( "" + bit + "b " + sampleRate + "Hz " + channels + "c " +
							 * signed + " " + bigEndian);
							 */
							ClientAPI.createAudioBuffer(sampleRate, bit, channels, signed, bigEndian);
						} else if (line.startsWith(audioDataHeader)) {
							try {
								System.out.println("recieving..");
								byte[] data = ClientAPI.recieveAudioData(single_transfer_size);
								System.out.println("appending data to buffer.." + data.length);
								ClientAPI.appendAudioBuffer(data);
								/*
								 * will need to do more things about data and buffer, the code above is only for
								 * testing and have bugs..
								 * 
								 * 
								 */
								System.out.println("playing..");
								ClientAPI.playAudio();
							} catch (IOException e) {
								e.printStackTrace();
							} catch (AudioPlayerException e) {
								e.printStackTrace();
							}
						} else {

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

	public static Socket getServer() {
		return server;
	}

	public static void setServer(Socket server) throws IOException {
		Client.server = server;
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