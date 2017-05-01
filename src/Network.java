/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import java.awt.Point;

/**
 *
 * @author Predi
 */
abstract class Network {

	protected Control ctrl;

	Network(Control c) {
		ctrl = c;
	}

	abstract void connect(String ip);

	abstract void disconnect();

	abstract void send(int[] pack);
}
