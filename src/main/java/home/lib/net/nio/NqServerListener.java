package home.lib.net.nio;

import home.lib.lang.UserException;

/**
 * 
 * @author richard
 *
 */
interface INioServerListener {

	public NqItem recv(NqServer svr, NqConnector ask, NqItem m,byte[]dataOfItem);

	public void connected(NqServer svr, NqConnector ask);

	public void disconnected(NqServer svr, NqConnector ask);

	public void sendSucceeded(NqServer svr, NqConnector ask, NqItem e);

	public void sendFailed(NqServer svr, NqConnector ask, NqItem e);

	public boolean putPath(NqConnector ask, String name);
	
	public void debug(NqServer svr, String f,Object ...args);

	public boolean login(NqServer svr, NqConnector nqConnector, String id, String pwd);

	public void returnError(NqServer nqServer, NqConnector nqConnector, UserException userException);
	
}

/**
 * 
 * @author richard
 *
 */
public class NqServerListener implements INioServerListener {

	@Override
	public NqItem recv(NqServer svr, NqConnector ask, NqItem m,byte[]dataOfItem) {
		 
		return m;
	}

	@Override
	public void connected(NqServer svr, NqConnector ask) {
		// TODO Auto-generated method stub

	}

	@Override
	public void disconnected(NqServer svr, NqConnector ask) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sendSucceeded(NqServer svr, NqConnector ask, NqItem e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sendFailed(NqServer svr, NqConnector ask, NqItem e) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean putPath(NqConnector ask, String name) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void debug(NqServer svr, String f, Object... args) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean login(NqServer svr, NqConnector nqConnector, String id, String pwd) {
 
		return true;
	}

	@Override
	public void returnError(NqServer nqServer, NqConnector nqConnector, UserException userException) {
		 debug( nqServer, "%s", userException );
	}

}
