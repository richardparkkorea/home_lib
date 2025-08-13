package deprecated.lib.net.nio.old;

interface INioConnectorListener {

	public NqItem received(NqConnector ask, NqItem m,byte[] dataOfItem);

	public void connected(NqConnector ask);

	public void disconnected(NqConnector ask);

	public void sendSucceeded(NqConnector ask, NqItem e);

	public void sendFailed(NqConnector ask, NqItem e);

	public boolean putPath(NqConnector ask, String name);
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

 
}
