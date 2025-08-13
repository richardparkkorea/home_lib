package home.lib.util;

import java.io.*;
import java.util.List;
import java.util.zip.*;

public class ZipUtil {
	static final int BUFFER = 2048;

	public static byte[] zip(byte[] data) {
		try {

			// BufferedInputStream origin = null;

			// FileOutputStream dest = new
			// FileOutputStream("c:\\zip\\myfigs.zip");

			ByteArrayOutputStream dest = new ByteArrayOutputStream();

			CheckedOutputStream checksum = new CheckedOutputStream(dest, new Adler32());
			ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(checksum));
			// //out.setMethod(ZipOutputStream.DEFLATED);
			// byte data[] = new byte[BUFFER];
			// // get a list of files from current directory
			// File f = new File(".");
			// String files[] = f.list();
			//
			// for (int i=0; i<files.length; i++) {
			// System.out.println("Adding: "+files[i]);
			// FileInputStream fi = new
			// FileInputStream(files[i]);
			// origin = new
			// BufferedInputStream(fi, BUFFER);
			// ZipEntry entry = new ZipEntry(files[i]);
			// out.putNextEntry(entry);
			// int count;
			// while((count = origin.read(data, 0,
			// BUFFER)) != -1) {
			// out.write(data, 0, count);
			// }
			// origin.close();
			// }
			// out.close();
			ZipEntry entry = new ZipEntry("1");
			out.putNextEntry(entry);
			out.write(data, 0, data.length);
			// origin.close();
			out.close();

			// Log.debug("checksum: " + checksum.getChecksum().getValue() + " original :" + data.length + " length: "
			// + dest.toByteArray().length);

			return dest.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static byte[] unzip(byte[] srcBuf) {
		try {
			final int BUFFER = 2048;
			// BufferedOutputStream dest = null;
			ByteArrayOutputStream dest = new ByteArrayOutputStream();

			ByteArrayInputStream src = new ByteArrayInputStream(srcBuf);

			// FileInputStream fis = new
			// FileInputStream(argv[0]);

			CheckedInputStream checksum = new CheckedInputStream(src, new Adler32());
			ZipInputStream zis = new ZipInputStream(new BufferedInputStream(checksum));
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				// System.out.println("Extracting: " + entry);
				int count;
				byte data[] = new byte[BUFFER];
				// write the files to the disk
				// FileOutputStream fos = new FileOutputStream(entry.getName());
				// dest = new BufferedOutputStream(fos,
				// BUFFER);
				while ((count = zis.read(data, 0, BUFFER)) != -1) {
					dest.write(data, 0, count);
				}
				dest.flush();
				dest.close();
			}
			zis.close();
			// Log.debug("Checksum: " + checksum.getChecksum().getValue() + " extracted length: "
			// + dest.toByteArray().length);

			return dest.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	/////////////////////////////////////////////////////////////////////////////////

	
 	public static void zipFile(String baseDir, String files[], String outname) throws Exception {

		BufferedInputStream origin = null;
		FileOutputStream dest = new FileOutputStream(outname);
		CheckedOutputStream checksum = new CheckedOutputStream(dest, new Adler32());
		ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(checksum));
		// out.setMethod(ZipOutputStream.DEFLATED);
		byte data[] = new byte[BUFFER];
		// get a list of files from current directory
		// File f = new File(".");
		// String files[] = f.list();

		for (int i = 0; i < files.length; i++) {
			// System.out.println("Adding: " + files[i]);
			FileInputStream fi = new FileInputStream(  FileUtil.pathAdd( baseDir, files[i] ));
			origin = new BufferedInputStream(fi, BUFFER);
			ZipEntry entry = new ZipEntry(  files[i] );
			out.putNextEntry(entry);
			int count;
			while ((count = origin.read(data, 0, BUFFER)) != -1) {
				out.write(data, 0, count);
			}
			origin.close();
		}
		out.close();
		// System.out.println("checksum: " + checksum.getChecksum().getValue());

	}

	public static void unzipFile(String zipname, List<String> outfiles) throws Exception {

		final int BUFFER = 2048;
		BufferedOutputStream dest = null;
		FileInputStream fis = new FileInputStream(zipname);
		CheckedInputStream checksum = new CheckedInputStream(fis, new Adler32());
		ZipInputStream zis = new ZipInputStream(new BufferedInputStream(checksum));
		ZipEntry entry;
		while ((entry = zis.getNextEntry()) != null) {
			//System.out.println("Extracting: " + entry);
			int count;
			byte data[] = new byte[BUFFER];
			// write the files to the disk
			FileOutputStream fos = new FileOutputStream(entry.getName());
			dest = new BufferedOutputStream(fos, BUFFER);
			while ((count = zis.read(data, 0, BUFFER)) != -1) {
				dest.write(data, 0, count);
			}
			dest.flush();
			dest.close();
			
			outfiles.add( entry.getName() ); 
			
			
		}
		zis.close();
		// System.out.println("Checksum: " + checksum.getChecksum().getValue());
	}
}