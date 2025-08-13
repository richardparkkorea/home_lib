package home.lib.log;

import java.net.DatagramPacket;



public interface MyLoggerListener

{

	//public void ulogReceive(DatagramPacket dp, ULoggerData s);
	
	public void ulogReceive(Object owner, String when, String form,int level, String str);
	
	public void ulogClosed(Object owner);
	
	 

}
