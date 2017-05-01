

import java.awt.Point;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SerialServer extends Network {

	private ServerSocket serverSocket = null;
	private Socket clientSocket = null;
	private ObjectOutputStream out = null;
	private ObjectInputStream in = null;

	SerialServer(Control c) {
		super(c);
	}

	private class ReceiverThread implements Runnable {

		public void run() {
			try {
				System.out.println("Waiting for Client");
				clientSocket = serverSocket.accept();
				System.out.println("Client connected.");
			} catch (IOException e) {
				System.err.println("Accept failed.");
				System.out.println(e.getMessage());
				disconnect();
				return;
			}

			try {
				out = new ObjectOutputStream(clientSocket.getOutputStream());
				in = new ObjectInputStream(clientSocket.getInputStream());
				out.flush();
//				ctrl.ClientConnected();
			} catch (IOException e) {
				System.err.println("Error while getting streams.");
				System.out.println(e.getMessage());
				disconnect();
				return;
			}

			try {
				ctrl.ClientConnected();				
				while (true) {
					int[] received = (int[]) in.readObject();
					System.out.println("\nColourNum = "+received[4]+"\n(0, if sending score)");
					ctrl.ReceivedScore(received);
					if(received[4]==0&&ctrl.MyScoreSent==true) break;
				}
			} catch (Exception ex) {
				System.out.println(ex.getMessage());
				System.err.println("Client disconnected! at server score reciever in thread");
			} finally {
				disconnect();
			}
		}
	}

	@Override
	void connect(String ip) {
		disconnect();
		try {
			serverSocket = new ServerSocket(10007);

			Thread rec = new Thread(new ReceiverThread());
			rec.start();
		} catch (IOException e) {
			System.err.println("Could not listen on port: 10007.");
			System.out.println(e.getMessage());
		}
	}

	@Override
	void disconnect() {
		try {
			if (out != null)
				out.close();
			if (in != null)
				in.close();
			if (clientSocket != null)
				clientSocket.close();
			if (serverSocket != null)
				serverSocket.close();
		} catch (IOException ex) {
			Logger.getLogger(SerialServer.class.getName()).log(Level.SEVERE,
					null, ex);
		}
	}

	@Override
	void send(int[] pack) {
		if (out == null)
			return;
		System.out.println("Sending to Client: "+pack[4]+"\n");//Ha nem 0, akkor feladatot kuld, ha 0 eredmenyt.
		try {
			out.writeObject(pack);
			out.flush();
		} catch (IOException ex) {
			System.out.println(ex.getMessage());
			System.err.println("Send error-problem to client");
		}
	}

}
