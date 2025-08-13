package home.lib.util;

import java.io.*;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * Title:
 * </p>
 *
 * <p>
 * Description:
 * </p>
 *
 * <p>
 * Copyright: Copyright (c) 2007
 * </p>
 *
 * <p>
 * Company:
 * </p>
 *
 * @author not attributable
 * @version 1.0
 */
public class FileUtil {
	public FileUtil() {
	}

	/**
	 * 
	 * @param src
	 * @param dest
	 * @return
	 * 
	 *         return copyied bytes
	 * 
	 */
	public static int CopyFile(String src, String dest) throws Exception {

		int bytes = 0;
		// Use unbuffered streams, because we're going to use a large buffer
		// for this sequential io.
		// try {
		FileInputStream input = new FileInputStream(src);
		FileOutputStream output = new FileOutputStream(dest);

		int bytesRead;
		byte[] buffer = new byte[256 * 1024];
		while ((bytesRead = input.read(buffer, 0, buffer.length)) > 0) {
			output.write(buffer, 0, bytesRead);
			bytes += bytesRead;

		}
		input.close();
		output.close();

		long lm = new File(src).lastModified();
		new File(dest).setLastModified(lm);
		// return 1;
		// } catch (Exception e) {
		// e.printStackTrace();
		// }

		return bytes;
	}

	/**
	 * _splitpath (refer to win32)
	 */
	public static String[] _splitpath(String pathname) {

		String[] strl = new String[4];
		int length = pathname.length();
		String dr = "";
		String path = "";
		String name = "";
		String ext = "";
		char separatorChar = File.separatorChar;

		if (pathname.trim().length() == 0)
			return strl;

		if (separatorChar == '\\') {
			pathname = pathname.replace('/', separatorChar);
		} else if (separatorChar == '/') {
			pathname = pathname.replace('\\', separatorChar);
		}

		int dp = pathname.indexOf(":");
		if (dp != -1) {
			dp++;
			dr = pathname.substring(0, dp); // c:
		}
		if (dp == -1) {
			dp = 0;
		}

		int pp = pathname.lastIndexOf(separatorChar);
		if (pp != -1 && dp <= pp) {
			pp++;
			path = pathname.substring(dp, pp); // /abc/cd/
			// System.out.println("xxx:"+dp+" "+pp);
		}
		if (pp == -1) { // case:(c:name.exe)
			pp = dp;

		}

		int dot = pathname.lastIndexOf('.');
		if (dot != -1 && pp <= dot) {
			if (pp != dot) {
				name = pathname.substring(pp, dot); // name
			}
			ext = pathname.substring(dot, length); // .exe
		} else {
			if (pp != length) {
				name = pathname.substring(pp, length); // name
			}
		}

		strl[0] = dr;
		strl[1] = path;
		strl[2] = name;
		strl[3] = ext;
		return strl;
	}

	/**
	 *
	 * @param pathname
	 *            String
	 * @return String
	 */
	public static String extract_dirname(String pathname) {
		String[] sl = _splitpath(pathname);
		if (sl == null)
			return null;
		return sl[0] + sl[1];
	}

	/**
	 *
	 * @param pathname
	 *            String
	 * @return String
	 */
	public static String extract_filename(String pathname) {
		String[] sl = _splitpath(pathname);
		if (sl == null)
			return null;
		return sl[2] + sl[3];
	}

	/**
	 *
	 * @param mask
	 *            String
	 * @param fstr
	 *            String
	 * @return boolean
	 */
	public static boolean findSimilarName(String mask, String fstr) {
		int i;

		// System.out.print( mask+" "+fstr+" :");

		//
		// split
		String co = "";// collect
		ArrayList<String> al = new ArrayList<String>();
		for (i = 0; i < mask.length(); i++) {

			char ch = (char) mask.charAt(i);
			if (i >= 1 && (char) mask.charAt(i - 1) == '*' && ch == '*')
				continue;// ignore

			if (ch == '*') {
				if (co.length() != 0) {
					al.add(co);
					co = "";
				}
				al.add(new Character(ch).toString());
			} else {
				co += new Character(ch).toString();
			}

		}
		if (co.length() != 0) {
			al.add(co);
		}
		String[] arr = al.toArray(new String[1]);

		int findst = 0;
		for (i = 0; i < arr.length; i++) {

			String c = arr[i];
			// System.out.println( "c : "+c );
			if (c.equals("*"))
				continue;

			int p = fstr.indexOf(c, findst);

			if (i == 0 && p != 0) // abd*...=-abc fail!
				return false;

			if (i == 0 && p == 0)// abc...
				continue;

			if (i == arr.length - 1 && p == (fstr.length() - c.length())) { // ...abc
				continue;
			} else if (i >= 1 && p != -1) { // *abc...
				findst = p;
				continue;
			}

			// System.out.println(" xxx "+i+" "+arr.length+ " " + p + " "+
			// (fstr.length()-c.length()) );

			if (p == -1) {
				return false;
			}
		} // for

		return true;
	}

	/**
	 * 
	 * use Application.getAppPath, it's better then this.<br>
	 * 
	 * @param o
	 * @return
	 */
	@Deprecated
	public static String getAppPath(Object o) {

		//
		String path = o.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
		try {
			String decodedPath = URLDecoder.decode(path, "UTF-8");

			// System.out.println("app path:" + new File(decodedPath).getParent());

			return new File(decodedPath).getParent();

		} catch (UnsupportedEncodingException e) {

		}
		return "";

	}

	/**
	 * combine the two file path characters
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	@Deprecated
	public static String pathAdd(String a, String b) {
		return pathAdd(a, b, File.separator);
	}
	/**
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static String join(String a, String b) {
		return pathAdd(a, b, File.separator);
	}

	/**
	 * combine the two file path characters
	 * 
	 * @param a
	 * @param b
	 * @param separator
	 * @return
	 */
	public static String pathAdd(String a, String b, String separator) {
		a = a.trim();
		b = b.trim();

		// remove / from end of 'a'
		if (a.length() > 0) {
			if (a.endsWith(separator)) {
				a = a.substring(0, a.length() - 1);
			}
		}

		// remove / from start of 'b'
		if (b.length() > 0) {
			if (b.startsWith(separator)) {
				b = b.substring(1, b.length());
			}
		}

		return (a + separator + b);
	}

	/**
	 * 
	 * @param baseDir
	 * @param pattern
	 *            null or regex
	 * @param result
	 *            return name of files
	 */
	public static void getFilenames(String baseDir, final String pattern, List<String> result) {

		// remove last seperator
		if (baseDir.endsWith(File.separator)) {
			baseDir = baseDir.substring(0, baseDir.length() - 1);
		}

		getFilenames(baseDir, pattern, new File(baseDir), result);
	}

	/**
	 * 
	 * @param baseDir
	 * @param pattern
	 *            null or regex
	 * @param from
	 *            null(default)
	 * @param result
	 */
	private static void getFilenames(String baseDir, final String pattern, final File from, List<String> result) {

		for (final File f : from.listFiles()) {

			if (f.isDirectory()) {
				getFilenames(baseDir, pattern, f, result);
			}

			if (f.isFile()) {
				if (pattern == null || f.getName().matches(pattern)) {
					// result.add(f.getAbsolutePath());

					String n = f.getAbsolutePath();

					result.add(n.substring(baseDir.length() + 1, n.length()));

				}
			}

		}
	}

	/**
	 * @param filename
	 * @return
	 * @throws Exception
	 */
	public static List<String> getLines(String filename) throws IOException {

		List<String> ret = new ArrayList<String>();

		BufferedReader reader;

		reader = new BufferedReader(new FileReader(filename));
		String line = reader.readLine();
		while (line != null) {
			// System.out.println(line);
			ret.add(line);
			// read next line
			line = reader.readLine();
		}
		reader.close();

		// return ret.toArray(new String[ret.size()]);
		return ret;

	}

	/**
	 * @param data
	 * @param fh
	 * @return
	 * @throws IOException
	 */
	public static void write(byte[] data, File fh) throws IOException {

		FileOutputStream fos = new FileOutputStream(fh);
		fos.write(data);
		fos.close();
	}

	/**
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static byte[] readAllBytes(File file) throws IOException {
		FileInputStream fileInputStream = null;
		byte[] bFile = new byte[(int) file.length()];

		// convert file into array of bytes
		fileInputStream = new FileInputStream(file);
		fileInputStream.read(bFile);
		fileInputStream.close();
		for (int i = 0; i < bFile.length; i++) {
			System.out.print((char) bFile[i]);
		}
		return bFile;
	}

	/**
	 * 
	 * @param dir
	 * @param arr
	 * @param includeSub
	 * @param ff
	 * @return
	 */
	public static long listFiles(String dir, boolean includeSub, FilenameFilter ff, ArrayList<String> out) {

		long byteLength = 0;

		if (ff == null) {
			ff = new FilenameFilter() {

				@Override
				public boolean accept(File dir, String name) {
					return true;// name.toLowerCase().endsWith(".zip");
				}

			};
		}

		//
		// is file?

		if (new File(dir).isFile()) {

			out.add(dir);

			return new File(dir).length();
		}

		//
		// is dir?

		File[] l = new File(dir).listFiles(ff);
		for (File f : l) {

			if (f.isFile()) {

				out.add(f.getAbsolutePath());

				byteLength += f.length();

			} else if (f.isDirectory()) {

				out.add(f.getAbsolutePath() + File.separator);

				if (includeSub) {
					byteLength += listFiles(f.getAbsolutePath(), true, ff, out);
				}
			}
		}
		return byteLength;

	}
}// class
