package deprecated.lib.net.mq2.dev_old;

import java.nio.channels.SocketChannel;

/**
 * 
 * @author richard
 *
 */
class MqChangeRequest {
	public static final int REGISTER = 1;
	public static final int CHANGEOPS = 2;
	public static final int REMOVE = 3;

	public SocketChannel socket;
	public int type;
	public int ops;

	public MqChangeRequest(SocketChannel socket, int type, int ops) {
		this.socket = socket;
		this.type = type;
		this.ops = ops;
	}
}
