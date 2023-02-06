package Client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientAPI {

	public static void playAudio() {

	}

	public static void downloadFile() {

	}

	public static void uploadFile() {

	}

	public static void sendMessage(String str) {
		getPrintWriter().println(str);
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
