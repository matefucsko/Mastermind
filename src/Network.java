abstract class Network {

	protected Control ctrl;

	Network(Control c) {
		ctrl = c;
	}

	abstract void connect(String ip);

	abstract void disconnect();

	abstract void send(int[] pack);
}
