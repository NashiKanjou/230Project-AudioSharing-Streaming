package Client;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class ClientAPI {

	public static final String host1 = "127.0.0.1";
	public static final int host1_port = 65500;
	public static final String host2 = "127.0.0.1";
	public static final int host2_port = 65510;

	public static ByteArrayOutputStream buffer;

	public static Thread recieveThread;
	public static Thread sendThread;
	public static Random rand = new Random();
	private static Socket server = null;
	private static DataInputStream inputFromServer = null;
	private static DataOutputStream outputFromServer = null;
	private static PrintWriter serverPrintOut = null;

	private static boolean done = false;

	public static List<String> list_files = new ArrayList<String>();

	public static boolean isPlaying = false;

	public static void downloadFile(FileOutputStream output, int chunkcount, String filename, int piece)
			throws IOException {
		Map<Integer, byte[]> buffer = new HashMap<Integer, byte[]>();// save data that arrived that is not in order
		ClientAPI.sendMessage(filename + chunkcount);
		try {
			DataInputStream input = ClientAPI.getDataInputStream();
			while (chunkcount < piece) {
				// System.out.println("test");
				if (buffer.containsKey(chunkcount)) {
					System.out.println("from buffer:" + chunkcount);
					output.write(buffer.get(chunkcount));
					buffer.remove(chunkcount);
					chunkcount++;
					if (chunkcount < piece) {
						ClientAPI.sendMessage(filename + chunkcount);
					}
					continue;
				}
				int stamp = -1;
				byte[] chunk = new byte[2052];
				// System.out.println("read");
				int i = input.read(chunk);
//System.out.println(i);
				try {
					for (int x = i - 4; x < i; x++) {
						byte b = chunk[x];
						stamp = (stamp << 8) + (b & 0xFF);
					}
				} catch (Exception e) {
				}
				// stamp = input.readInt();
				System.out.println("recieved:" + stamp);
				if (stamp == chunkcount) {
					System.out.println("write:" + stamp);
					output.write(chunk, 0, i - 4);
					chunkcount++;
					if (chunkcount < piece) {

						int r = chunkcount + rand.nextInt(10) - 5;
						System.out.println("rand:" + r);

						int rand_int = Math.min(r, piece);
						ClientAPI.sendMessage(filename + rand_int);
					}
				} else {
					System.out.println("buffered:" + stamp);
					buffer.put(stamp, chunk);

					ClientAPI.sendMessage(filename + chunkcount);
				}
			}
			System.out.println("end");
		} catch (Exception e) {
			start();
			downloadFile(output, chunkcount, filename, piece);
		} finally {
			if (chunkcount == piece) {
				System.out.println("close");
				output.close();
			}
		}
	}

	public static void downloadFile(String filename, int piece) throws IOException {// untested
		FileOutputStream output = new FileOutputStream(filename, true);
		Map<Integer, byte[]> buffer = new HashMap<Integer, byte[]>();// save data that arrived that is not in order
		int chunkcount = 0;
		ClientAPI.sendMessage(filename + chunkcount);
		try {
			DataInputStream input = ClientAPI.getDataInputStream();
			while (chunkcount < piece) {
				System.out.println("");

				int stamp = -1;
				byte[] chunk = new byte[2052];
				// System.out.println("read");
				int i = input.read(chunk);
//System.out.println(i);
				try {
					for (int x = i - 4; x < i; x++) {
						byte b = chunk[x];
						stamp = (stamp << 8) + (b & 0xFF);
					}
				} catch (Exception e) {
				}
				// stamp = input.readInt();
				System.out.println("recieved:" + stamp);
				if (stamp == chunkcount) {
					System.out.println("write:" + stamp);
					output.write(chunk, 0, i - 4);
					chunkcount++;
					if (chunkcount < piece) {
						int r = chunkcount;
						if (rand.nextInt(10) > 2) {
							r = chunkcount + rand.nextInt(5);
							// System.out.println("rand:" + r);
						}
						int rand_int = Math.min(r, piece - 1);
						ClientAPI.sendMessage(filename + rand_int);
					}
				} else if (stamp > chunkcount) {
					System.out.println("buffered:" + stamp);
					byte[] data = new byte[i - 4];
					for (int a = 0; a < i - 4; a++) {
						data[a] = chunk[a];
					}
					buffer.put(stamp, data);

					ClientAPI.sendMessage(filename + chunkcount);
				} else {
					ClientAPI.sendMessage(filename + chunkcount);
				}
				while (chunkcount < piece && buffer.containsKey(chunkcount)) {
					System.out.println("from buffer:" + chunkcount);
					output.write(buffer.get(chunkcount), 0, buffer.get(chunkcount).length);
					buffer.remove(chunkcount);
					chunkcount++;
					/*
					 * if (chunkcount < piece) { ClientAPI.sendMessage(filename + chunkcount); }
					 */
					continue;
				}
			}
			System.out.println("end");
		} catch (Exception e) {
			downloadFile(output, chunkcount, filename, piece);
		} finally {
			if (chunkcount == piece) {
				System.out.println("close");
				output.close();
			}
		}

	}

	public static void uploadFile() {

	}

	public static void sendMessage(String str) {
		getPrintWriter().println(str);
		System.out.println("send " + str);
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

	public static void start() {
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
			return;
		}
		recieveThread = new Thread("Receive Thread") {
			@Override
			public void run() {
				DataInputStream d = new DataInputStream(inputFromServer);
				BufferedReader b = new BufferedReader(new InputStreamReader(d));
				long start = System.currentTimeMillis();
				
				while (!done) {
					// System.out.println("loop");
					try {
						String line = "";

						line = b.readLine();
						System.out.println("R:" + line);

						// System.out.println("line");
						// line = server_send.nextLine();

						if (line.equalsIgnoreCase("liststart")) {
							System.out.println("playlist updating");
							list_files.clear();
							line = b.readLine();
							while (!line.equalsIgnoreCase("listend")) {
								list_files.add(line);
								// System.out.println(line);
								line = b.readLine();
							}
							System.out.println("playlist updated");
							// output to GUI?
						} else if (line.equalsIgnoreCase("stream")) {
							try {
								InputStream in = new BufferedInputStream(server.getInputStream());
								play(in);
							} catch (Exception e) {
							}

						} else if (line.startsWith("download")) {
							// System.out.println("R:"+line);
							int piece = 0;
							byte[] data = new byte[4];
							try {
								inputFromServer.read(data);
								// System.out.println("recieve");

								for (byte by : data) {
									piece = (piece << 8) + (by & 0xFF);
								}
								System.out.println("piece:" + piece);
							} catch (IOException e1) {
								e1.printStackTrace();
							}

							try {
								downloadFile(line.substring(9), piece);
							} catch (IOException e) {
								e.printStackTrace();
							}

						} else {
							System.out.println(line);
							try {
								InputStream in = new BufferedInputStream(server.getInputStream());
								play(in);
							} catch (Exception e) {
							}
						}

					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				long end = System.currentTimeMillis();
				System.out.println(end - start);
				// server_send.close();
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
						if (line.equals("stop")) {
							ClientAPI.stop();
						} else if (line.equals("pause")) {
							ClientAPI.pause();
						} else if (line.startsWith("stream")) {
							System.out.println("wav");
							line = line.substring(7);
							ClientAPI.sendMessage(line);

						} else if (line.startsWith("download")) {
							System.out.println("download");
							// line=line.substring(9);
							ClientAPI.sendMessage(line);

						} else if (line.equals("showlist")) {
							for (String str : list_files) {
								System.out.println(str);
							}
						} else {
							ClientAPI.sendMessage(line);
						}

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
		inputFromServer = new DataInputStream(server.getInputStream());
		outputFromServer = new DataOutputStream(server.getOutputStream());
		serverPrintOut = new PrintWriter(new OutputStreamWriter(outputFromServer, "UTF-8"), true);
	}

	public static DataInputStream getDataInputStream() {
		return inputFromServer;
	}

	public static DataOutputStream getDataOutputStream() {
		return outputFromServer;
	}

	public static PrintWriter getPrintWriter() {
		return serverPrintOut;
	}

	public static Clip clip;
	public static Thread playthread;

	private static synchronized void play(final InputStream in) throws Exception {
		if (clip != null) {
			clip.stop();
		}
		AudioInputStream ais = AudioSystem.getAudioInputStream(in);
		try {
			clip = AudioSystem.getClip();
			clip.open(ais);
			clip.start();
			isPlaying = true;
			Thread.sleep(200); // given clip.drain a chance to start
			clip.drain();
		} catch (Exception e) {
			e.printStackTrace();
		}
		/*
		 * if (playthread != null) { playthread.join(); } playthread = new
		 * Thread("play Thread") {
		 * 
		 * @Override public void run() { try { clip = AudioSystem.getClip();
		 * clip.open(ais); clip.start(); isPlaying = true; Thread.sleep(100); // given
		 * clip.drain a chance to start clip.drain(); } catch (Exception e) {
		 * e.printStackTrace(); }
		 * 
		 * } }; playthread.start();
		 */
	}

	private static void stop() {
		if (clip != null) {
			clip.stop();
		}
	}

	private static void pause() {
		if (clip != null) {
			if (isPlaying) {
				clip.stop();
				isPlaying = false;
			} else {
				clip.start();
				isPlaying = true;
			}
		}
	}
}
