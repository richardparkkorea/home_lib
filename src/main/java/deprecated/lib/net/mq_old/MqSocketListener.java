package deprecated.lib.net.mq_old;

 

public interface MqSocketListener {

	public MqBundle actionPerformed(MqSocket ms, MqBundle e);

	public void log(MqSocket ms, Exception e) ;

	public void log(MqSocket ms, int level, String s);

	public void connected(MqSocket ms) ;

	public void disconnected(MqSocket ms) ;

}
