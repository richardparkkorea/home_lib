package home.lib.net.tms;

public class TmsEvent {

	// e - error
	// a - accept
	// c - connect
	// d - disconnect(connecter)
	// x - disconnect(accepter)
	// r - recevice
	// t - return
	// l = log in
	
	public char event;

	public byte[] rxd;

	public TmsChannel ch;// channel
	
	public TmsItem item;
	
	

	public TmsEvent(TmsChannel c, char e, byte[] rx,TmsItem tm) {
		ch = c;
		rxd = rx;
		event = e;
		item=tm;
	}

}
