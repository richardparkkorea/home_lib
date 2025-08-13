package home.lib.net.mq;

 

public interface IMqSocketListener {

	public MqBundle actionPerformed(MqSocket ms, MqBundle e);

	public void log(MqSocket ms, Exception e) ;

	public void log(MqSocket ms, int level, String s);

	public void connected(MqSocket ms) ;

	public void disconnected(MqSocket ms) ;

}
