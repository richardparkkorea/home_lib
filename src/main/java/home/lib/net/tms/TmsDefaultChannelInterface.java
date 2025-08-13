package home.lib.net.tms;

import home.lib.util.TimeUtil;

public class TmsDefaultChannelInterface implements TmsChannelInterface {

	@Override
	public void onLink(TmsChannel ch) {

		System.out.format("broker.onLink id(%s) pwd(%s) \r\n", ch.getIdName(), ch.getPwd());

		ch.setLoggedIn(true);

	}

	@Override
	public TmsItem onReceived(TmsChannel arg0, byte[] arg1, TmsItem e) {

	 
		return e;
	}

	@Override
	public Object onAccepteded(TmsChannel arg0) {

		return null;
	}

	@Override
	public boolean onConnected(TmsChannel arg0) {

		return true;
	}

	@Override
	public void onDisconnected(TmsChannel arg0) {

	}

	@Override
	public void onError(TmsChannel ch, byte[] rxd) {

		System.out.format("%s] path[%s] evt(%s) msg(%s) \r\n", TimeUtil.now(), ch.getPath(), "e", new String(rxd));

	}
}
