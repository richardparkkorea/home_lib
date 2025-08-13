package home.lib.net.tms;

import home.lib.net.tms.TmsChannel.Qos;
import home.lib.util.StringUtil;
import home.lib.util.TimeUtil;

public class TmsExample1 {

	static int txCount = 0;
	static int rxCount = 0;
	static int serverRxCount = 0;

	public static void main(String[]args) {
		try {

			String ip = "127.0.0.1";
			int port = 12345;
			TmsBroker tf = new TmsBroker("local", "0.0.0.0", port,null, new ITmsChannel() {

				@Override
				public void onLink(TmsChannel ch) {
					
					System.out.format("broker.onLink id(%s) pwd(%s) \r\n",  ch.getIdName(), ch.getPwd() );

					ch.setLoggedIn(true);

				}

				@Override
				public TmsItem onReceived(TmsChannel arg0, byte[] arg1, TmsItem e) {

					serverRxCount++;
					return e;
				}

			}
			).waitBind(10);
			// tf.jobOpt(8, 6000);
			// tf.jobPeekCount(8);
			// tf.setReceiveTimeoutSec(60);
			//tf.setChannelInterface();

			TmsChannel tx = tf.connectw(ip, port, true, "/tms/tx", null, "id", "pwd", 10000).get();

			TmsChannel rx = tf.connectw(ip, port, true, "/tms/rx", null, "id", "pwd", new ITmsChannel() {

				@Override
				public TmsItem onReceived(TmsChannel tc, byte[] r, TmsItem e) {

					try {
						tc.doMethod(this, r, e);
					} catch (Exception e1) {
						e1.printStackTrace();
					}

					// System.out.println("rx:" + new String(arg1));
					rxCount++;

					return null;
				}
			}, 10000).get();

			for (int i = 0; i < 10; i++) {
				tf.connectw(ip, port, true, "/tms/tx/" + i, null, "id", "pwd", new ITmsChannel() {

					@Override
					public TmsItem onReceived(TmsChannel tc, byte[] r, TmsItem e) {

						// System.out.println("rx:" + new String(arg1));
						rxCount++;

						return null;
					}
				}, 10000).get();
			}

			TimeUtil t0 = new TimeUtil();
			TimeUtil t1 = new TimeUtil();

			while (tf.isAlive()) {

//				TmsEvent e;
//				while ((e = tf.pollEvent()) != null) {
//
//					try {
//
//						e.ch.setLoggedIn(true);
//
//						if (e.event == 'e') { // login
//
//							// e.ch.setLoggedIn(true);
//
//							System.out.format("%s] path[%s] evt(%s) msg(%s) \r\n", TimeUtil.now(), e.ch.getPath(),
//									e.event, new String(e.rxd));
//
//						} else if (e.event == 'l') { // login
//
//							e.ch.setLoggedIn(true);
//
//							System.out.format("%s] path[%s]  evt(%s) idname(%s) pwd(%s)     \r\n", TimeUtil.now(),
//									e.ch.getPath(), e.event, e.ch.getIdName(), e.ch.getPwd());
//
//						} else if (e.event == 'r') {
//
//							if (e.item.isAskReturn()) {
//
//								// e.ch.sendReturn(e.item, TimeUtil.now().getBytes());
//							}
//
//							// String str = ("res>" + new String(e.rxd));
//							// tf.send("/tms/con2", "*", str.getBytes());
//							System.out.format("%s] path[%s] evt(%s) msg(%s) \r\n", TimeUtil.now(), e.ch.getPath(),
//									e.event, new String(e.rxd));
//
//						} else
//							System.out.format("%s] path[%s]  evt(%s)      \r\n", TimeUtil.now(), e.ch.getPath(),
//									e.event);
//
//					} catch (Exception exx) {
//						exx.printStackTrace();
//					}
//				}

				if (tf.getEventCount() == 0) {
					Thread.sleep(1);
				}

				if (t0.end_ms() > 1) {
					t0.start();
					tx.send("*", ("send time" + TimeUtil.now()).getBytes(), Qos.AtLeastOnce);
					// tx.send("*", ("send time" + TimeUtil.now()).getBytes());

					

					txCount++;

				}

				if (t1.end_sec() > 3) {
					t1.start();
					System.out.format(" tx: %s rx: %s  severRx: %s  brk(%s) \r\n", StringUtil.formatCount(txCount),
							StringUtil.formatCount(rxCount), StringUtil.formatCount(serverRxCount) , tf.toString() );
					
					TimeUtil t = new TimeUtil();
					Object r = tx.callMethod( "/tms/rx" ,10000, "print", "say hello . " + TimeUtil.now());
					if (r != null) {
						System.out.println(" ret=" + r + "  " + t.end_ms());
					}
				}

				TimeUtil.sleep(1);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}

/**
 * 
 * @author richard
 *
 */
class ITmsChannel implements TmsChannelInterface {

	public String print(String s) {
		System.out.println(s);
		return "<" + s + ">";
	}

	@Override
	public Object onAccepteded(TmsChannel arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean onConnected(TmsChannel arg0) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void onDisconnected(TmsChannel arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onError(TmsChannel ch, byte[] rxd) {

		System.out.format("%s] path[%s] evt(%s) msg(%s) \r\n", TimeUtil.now(), ch.getPath(), "e", new String(rxd));

	}

	@Override
	public void onLink(TmsChannel ch) {

	}

	@Override
	public TmsItem onReceived(TmsChannel arg0, byte[] arg1, TmsItem e) {
		// TODO Auto-generated method stub
		return e;
	}

}