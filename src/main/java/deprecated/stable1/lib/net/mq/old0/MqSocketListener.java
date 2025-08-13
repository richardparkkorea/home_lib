package deprecated.stable1.lib.net.mq.old0;

 

public interface MqSocketListener {

	public MqBundle actionPerformed(MqSocket ms, MqBundle e);

	public void log(MqSocket ms, Exception e) ;

	public void log(MqSocket ms, int level, String s);

	public void connected(MqSocket ms) ;

	public void disconnected(MqSocket ms) ;

}
