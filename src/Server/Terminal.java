package Server;

import java.util.Scanner;

/*Sample Server code from: https://medium.com/martinomburajr/java-create-your-own-hello-world-server-2ca33b6957e*/
public class Terminal implements Runnable {
	public static boolean running = false;
	private Thread thread;
	private Server server;

	public Terminal(Server server) {
		this.server = server;
	}

	public void start() {
		if (running) {
			return;
		}
		running = true;
		thread = new Thread(this);
		thread.start();
	}

	private void stop() {
		if (!running) {
			return;
		}
		running = false;
		//System.out.println("Stopping..");
		try {
			server.stop();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		System.out.println("Server Stopped");
		try {
			Thread.sleep(500);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		System.out.println("Terminal Stopped");
		System.exit(0);
	}

	public void run() {
		System.out.println("Terminal Running...");
		Scanner scanner = new Scanner(System.in);
		boolean done = false;
		while (!done && scanner.hasNext()) {
			String str = scanner.next();
			if (str.equalsIgnoreCase("exit")) {
				done = true;
				stop();
			}
		}
		scanner.close();
	}

}
