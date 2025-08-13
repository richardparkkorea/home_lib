package org.xcopy.net;

import home.lib.net.mq.MqBundle;

public interface IMySocketListener {

	public MqBundle actionPerformed(MySocket ms, MqBundle e);

	public void log(MySocket ms, Exception e) ;

	public void log(MySocket ms, int level, String s);

	public void connected(MySocket ms) ;

	public void disconnected(MySocket ms) ;

}
