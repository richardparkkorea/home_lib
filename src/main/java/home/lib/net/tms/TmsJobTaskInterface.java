package home.lib.net.tms;


interface TmsJobTaskInterface {
	
	public void doSucceeded(TmsJobTask jt, Object e);

	public void doFailed(TmsJobTask jt, Object e);
}
