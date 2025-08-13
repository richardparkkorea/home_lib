package deprecated.lib.net.mq.and;

import deprecated.lib.net.mq.MqBundle;

public interface MqASocketListener {

	public MqBundle actionPerformed(MqASocket ms, MqBundle e);

	public void log(MqASocket ms, Exception e) ;

	public void log(MqASocket ms, int level, String s);

	public void connected(MqASocket ms) ;

	public void disconnected(MqASocket ms) ;

}
