package home.lib.net.nio;

import home.lib.lang.UserException;

interface INioConnectorListener {

	public NqItem received(NqConnector ask, NqItem m,byte[] dataOfItem);

	public void connected(NqConnector ask);

	public void disconnected(NqConnector ask);

	public void sendSucceeded(NqConnector ask, NqItem e);

	public void sendFailed(NqConnector ask, NqItem e);

	public boolean putPath(NqConnector ask, String name);

	public boolean login(NqConnector nqConnector, String id, String pwd);

	public void returnError(NqConnector nqConnector, UserException userException);

 
}

public class NqConnectorListener implements INioConnectorListener {

	@Override
	public NqItem received(NqConnector ask, NqItem m, byte[] dataOfItem) {
 
		return m;
	}

	@Override
	public void connected(NqConnector ask) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void disconnected(NqConnector ask) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sendSucceeded(NqConnector ask, NqItem e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sendFailed(NqConnector ask, NqItem e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean putPath(NqConnector ask, String name) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean login(NqConnector nqConnector, String id, String pwd) {
 
		return true;
	}

	@Override
	public void returnError(NqConnector nqConnector, UserException userException) {
		// TODO Auto-generated method stub
		
	}

	 

	 

 
}
