package home.lib.net.tms;

public interface TmsChannelInterface {

	public TmsItem onReceived(TmsChannel ch, byte[] rxd,TmsItem item);

	public void onDisconnected(TmsChannel ch);

	public Object onAccepteded(TmsChannel ch);

	public boolean onConnected(TmsChannel channel);
	
	public void onError(TmsChannel ch, byte[] err);
	
	public void onLink(TmsChannel ch); 
	
}
