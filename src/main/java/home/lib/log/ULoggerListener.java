package home.lib.log;

import java.net.DatagramPacket;


@Deprecated 
public interface ULoggerListener

{

	//public void ulogReceive(DatagramPacket dp, ULoggerData s);
	
	public void ulogReceive(Object owner, String when, String form,int level, String str);
	
	public void ulogClosed(Object owner);
	
	 

}
