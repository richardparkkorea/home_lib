package home.lib.net.tms.mqtt3ssl.sup;

import home.lib.net.mq.MqBundle;

public interface IMySocketSupListener {

	public MqBundle actionPerformed(MySocketSup ms, MqBundle e);

	public void log(MySocketSup ms, Exception e) ;

	public void log(MySocketSup ms, int level, String s);

	public void connected(MySocketSup ms) ;

	public void disconnected(MySocketSup ms) ;

}
