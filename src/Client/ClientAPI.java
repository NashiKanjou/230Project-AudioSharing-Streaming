package Client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import audio.AudioBuffer;
import audio.AudioPlayer;
import audio.AudioPlayerException;

public class ClientAPI {

	public static final String host1 = "";
	public static final int host1_port = 9991;
	public static final String host2 = "114.33.193.52";
	public static final int host2_port = 9991;

	public static AudioBuffer buffer;
	// private static boolean isPlaying;
	public static AudioPlayer player;

	public static Thread recieveThread;
	public static Thread sendThread;

	private static final String audioFormatHeader = "AUDIOFORMAT=";
	private static final String audioDataHeader = "AUDIODATASTART";
	public static final int single_transfer_size = 480000;// testing

	public static int sampleRate = 8000;
	public static int bit = 16;
	public static int channels = 2;
	public static boolean bigEndian = true;
	public static boolean signed = true;

	private static Socket server = null;
	private static DataInputStream inputToServer = null;
	private static DataOutputStream outputFromServer = null;
	private static PrintWriter serverPrintOut = null;

	private static boolean done = false;

	public static byte[] recieveAudioData(int frames) throws IOException {
		byte[] data = new byte[frames];
		DataInputStream input = ClientAPI.getDataInputStream();
		int count = 0;
		while (count < frames) {
			data[count] = input.readByte();
			count++;
		}
		return data;
	}

	public static void playAudio() throws AudioPlayerException {
		// player = new AudioPlayer(buffer);
		// player.playAudio();
		Clip clip;
		AudioInputStream inputStream;
		try {
			inputStream = buffer.toAudioInputStream();
			try {
				clip = AudioSystem.getClip();
				clip.open(inputStream);
				clip.start();
				/*
				 * might need a new thread for this, please check!
				 * 
				 * 
				 * 
				 */
			} catch (LineUnavailableException e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void stopAudio() throws AudioPlayerException {
		player.stop();
	}

	/**
	 * will create new buffer in this API with data
	 * 
	 * @param sampleRate is the sample rate of data
	 * @param channels   is number of channels of the data
	 * @param bitdepth   is bit depth of data (usually will be 16 or 24)
	 * @param signed     is the data signed (usually signed)
	 * @param bigEndian  is the data bigEndian or not (usually true)
	 */
	public static void createAudioBuffer(int sampleRate, int bitdepth, int channels, boolean signed, boolean bigEndian,
			byte[] data) {
		AudioFormat format = new AudioFormat(sampleRate, bitdepth, channels, signed, bigEndian);
		buffer = new AudioBuffer(format, data);
	}

	/**
	 * will create new buffer in this API
	 * 
	 * @param sampleRate is the sample rate of data
	 * @param channels   is number of channels of the data
	 * @param bitdepth   is bit depth of data (usually will be 16 or 24)
	 * @param signed     is the data signed (usually signed)
	 * @param bigEndian  is the data bigEndian or not (usually true)
	 */
	public static void createAudioBuffer(int sampleRate, int bitdepth, int channels, boolean signed,
			boolean bigEndian) {
		AudioFormat format = new AudioFormat(sampleRate, bitdepth, channels, signed, bigEndian);
		System.out.println("number of channel(s): " + format.getChannels());
		System.out.println("bit depth: " + format.getSampleSizeInBits());
		System.out.println("sample rate: " + format.getSampleRate());
		buffer = new AudioBuffer(format);
	}

	public static void appendAudioBuffer(byte[] data) {
		buffer.write(data, 0, data.length);
	}

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
			System.out.println("connecting to "+ host);
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
				ClientAPI.createAudioBuffer(sampleRate, bit, channels, true, true);
				while (!done) {
					if (server_send.hasNextLine()) {
						String line = server_send.nextLine();
						if (line.startsWith(audioFormatHeader)) {
							line = line.substring(12);
							String[] f = line.split(",");
							bit = Integer.parseInt(f[0]);
							sampleRate = Integer.parseInt(f[1]);
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
