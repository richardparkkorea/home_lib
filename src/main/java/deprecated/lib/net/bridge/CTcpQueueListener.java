package deprecated.lib.net.bridge;

import java.nio.channels.SocketChannel;

public class CTcpQueueListener {
	public void addSocket(SocketChannel sk) {
	}

	public boolean addClientId(String id, String pwd, SocketChannel sk) {
		return true;//true is allow , false is refuse
	}

	public void removeSocket(SocketChannel sk) {
	}

	/*
	 * if the result is null of actionPerformed, then the packet is no longer
	 * proceed.
	 * 
	 * null => block otherwise => processing
	 * 
	 * @param e
	 * @return
	 */
	public CBundle actionPerformed(CBundle e) {
		return e;
	}

	/*
	 * if you use Queue without ProtocolOption then use the below fucntion.
	 * 
	 * @param sk
	 * @param b
	 * @return
	 */
	public byte[] actionPerformed(SocketChannel sk, byte[] b) {
		return b;
	}// for none protocol

	public void log(Exception e) {
		e.printStackTrace();
	}

	public void log(int level, String s) {
	}

	public void startUp() {
	}

	public void finishUp() {
	}
}
