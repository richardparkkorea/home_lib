package home.lib.io;


public interface JobTaskInterface {
	
	public void doSucceeded(JobTask jt, Object e);

	public void doFailed(JobTask jt, Object e);
}
