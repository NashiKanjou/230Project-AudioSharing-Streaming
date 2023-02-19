package Client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import audio.AudioBuffer;
import audio.AudioPlayer;
import audio.AudioPlayerException;

public class ClientAPI {
	public static AudioBuffer buffer;
	// private static boolean isPlaying;
	public static AudioPlayer player;

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
			} catch (LineUnavailableException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
		System.out.println(format.getChannels());
		System.out.println(format.getSampleSizeInBits());
		System.out.println(format.getSampleRate());
		buffer = new AudioBuffer(format);
	}

	public static void appendAudioBuffer(byte[] data) {
		buffer.write(data, 0, data.length);
	}

	public static void downloadFile() {

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

	public static void connectServer(String host, int port) {
		try {
			Socket server = new Socket(host, port);
			Client.setServer(server);
		} catch (Exception e) {
			System.out.println(e.toString());
			if (host.equals(Client.host1)) {
				connectServer(Client.host2, Client.host2_port);
			} else {
				connectServer(Client.host1, Client.host1_port);
			}
		}
	}

	public static Socket getServerSocket() {
		return Client.getServer();
	}

	public static DataInputStream getDataInputStream() {
		return Client.getDataInputStream();
	}

	public static DataOutputStream getDataOutputStream() {
		return Client.getDataOutputStream();
	}

	public static PrintWriter getPrintWriter() {
		return Client.getPrintWriter();
	}
}
