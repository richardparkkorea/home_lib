package home.lib.log;

public interface ILogger {

	public int l(String fmt, Object... args);

	public int l(int level, String from, String fmt, Object... args);

	public int e(Exception e);

	public int e(int level, String from, Exception e);
}
