package home.lib.net.tms.mqtt3ssl;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.SSLContext;

class SimpleSSLClient {

	public static void start(String[] args) {
		String host = "localhost";
		int port = 8443;
		//String truststoreFile = "truststore.jks";
		//String truststorePassword = "password";

		try {
			// Load truststore
//            KeyStore trustStore = KeyStore.getInstance("JKS");
//            trustStore.load(new FileInputStream(truststoreFile), truststorePassword.toCharArray());
//
//            // Initialize TrustManagerFactory
//            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
//            trustManagerFactory.init(trustStore);
//
//            // Initialize SSLContext
//            SSLContext sslContext = SSLContext.getInstance("TLS");
//            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

			String cacert = "-----BEGIN CERTIFICATE-----\r\n"//
					+ "MIIFFTCCAv2gAwIBAgIUER/Wqdae15Q2dbrDOdLr+6VpkOkwDQYJKoZIhvcNAQEN\r\n"//
					+ "BQAwGjEYMBYGA1UEAwwPMTE0LjIwMC4yNTQuMTgxMB4XDTI0MDcwNDA3NTgyNFoX\r\n"//
					+ "DTMyMDcwMjA3NTgyNFowGjEYMBYGA1UEAwwPMTE0LjIwMC4yNTQuMTgxMIICIjAN\r\n"//
					+ "BgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAtWTNFH5xm1u77drnvcc2hrtCjoQA\r\n"//
					+ "K6lTcYVkr6trCbKolCHYqlwsdQelfJcNLmhi3K6Aanj+VLCkCthTr45IxJtQtlL9\r\n"//
					+ "AAkpDXcvOmdOxNjrMP3rTtxQ7VihUVolERyxR6lAiB9mezhPk5F2MQcCdSON5XSj\r\n"//
					+ "IHdDY9pIAYxbiEsMwF2tOOn8SWCZxQ0mIqIeEu7vdOWky41cbCOBuVJaO+8bBObS\r\n"//
					+ "3xP40YmCgdtiAownBP/lkDGbgBr1gALosc6cNgeX5jfF7fqTlHcT3mYVut6hhWRZ\r\n"//
					+ "QdyByNXFV5fLu6uic4bCbWPL/mMGTEaMnz3Z4ezyre8YyjkJs36FFXqb4ji2om74\r\n"//
					+ "KpEPngX8X+1qBhBfYY31cF/80C2lck2bCafIKL+kxextqmovLv1y7OmbFPJ5mnXM\r\n"//
					+ "lR2GowEA4KbEvNoBD9dWWxoTRnP97qvaDoij2PEtAJWOaam4qOWa0++V3/aPfsAh\r\n"//
					+ "/xYecByR1e1eepIg5TrQ43rMYXozrIyg0boU/RHayyXSdlylpSAKqYsluf9mqlMh\r\n"//
					+ "GAMyMoHdWmTkb8zX7NYMky39zrS0CKY6bHEZfKKXnbwomvN0ZoHUrUwphmo9ZmQu\r\n"//
					+ "QRR1bNSfA9lU43Tz7gCiGX1Fi/zA5RbcYdvDFSf1XUlIXofLfFuP/cTUePIm5/+s\r\n"//
					+ "ivVWmYAsBMcQNnECAwEAAaNTMFEwHQYDVR0OBBYEFImMdxMSD0KPVdxrq7AyEFWl\r\n"//
					+ "QxRqMB8GA1UdIwQYMBaAFImMdxMSD0KPVdxrq7AyEFWlQxRqMA8GA1UdEwEB/wQF\r\n"//
					+ "MAMBAf8wDQYJKoZIhvcNAQENBQADggIBAEuEajA1zeJocUK0plhkgH4mx+3xzPHX\r\n"//
					+ "O6OJDflDRV8Mytw/VhqG8Tt60h00KR7E5WSMvioERGDgIz3omDCd1RPOVI8ENC+z\r\n"//
					+ "eCp0Rt1Q9DY3IB0tiR2ouFuqzZL8VMatodcsj+J8lzpCepkRMJXULqf9lXKc6WBV\r\n"//
					+ "FDY5QE/mMpg6YvoII/NIdgad8nsX3UbIqS5Zd4sQRMuQtdfaecoetn1gUlGWmUNi\r\n"//
					+ "kzP7iGgLPLKttluDFHUTz1h1n1TpJFR49xWtjKxlS/VF7aziuwchCezc3/f86ntg\r\n"//
					+ "89VtZO4JnbyVOAqBhJPVFMR3BGlFNfW/bYPTfDtBZpn1JZaNknLXbmHCUCW7v5bm\r\n"//
					+ "Qpt/bTbaU5yv4U+5lKHOUdKVTyxz9d+a1fkRjBrUQNVQJ9KSKjvHVGcJInjl5oUt\r\n"//
					+ "nJiaPvzgjqUa4cVgYfi+46vHHQehYQzvHBRec99dUUnwvqkVYA4R1f/H6YoWpTcc\r\n"//
					+ "dV8JTE2VZxRxgH5lzAObZiNcashZFn29oCe1FXKhPW8EuZoj4rSi/Q7yyEQZcNhh\r\n"//
					+ "SM/Wj17hC+gGHoqBHrY8SnMN5oT0j8I/HcKyj6QkMXTQswQ/wKpNdJiQOKbFexrq\r\n"//
					+ "tIEHhO+RMvHPmB0jSLxo8V+vjHVJgz99bFcvAntVLrb/gk+9ji+rKp5IXVd/EubR\r\n"//
					+ "Qb8NyfvYybLj\r\n"//
					+ "-----END CERTIFICATE-----";//

			KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
			trustStore.load(null, null);

			trustStore.setCertificateEntry("Custom CA", (X509Certificate) CertificateFactory.getInstance("X509")
					.generateCertificate(new ByteArrayInputStream(cacert.getBytes(StandardCharsets.UTF_8))));

			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(trustStore);
			TrustManager[] trustManagers = tmf.getTrustManagers();

			SSLContext sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, trustManagers, null);

			// SSLContext sslContext = getSsl();

			// SSLSocketFactory socketFactory = sslContext.getSocketFactory();
			// SSLSocket kkSocket = (SSLSocket) socketFactory.createSocket(ip,port);

			// Create SSLSocketFactory
			SSLSocketFactory socketFactory = sslContext.getSocketFactory();

			// Create SSLSocket
			Socket socket = (Socket) socketFactory.createSocket(host, port);

			//socket.setEnabledProtocols(new String[] { "TLSv1.2" });
			// socket.startHandshake();

			
			OutputStream out=socket.getOutputStream();
		
			
			
			// Get input and output streams
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
 	//		PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), 					true);

			// Send data to server
			// out.println("Hello, SSL Server!");
out.write("Hello, SSL Server!\r\n".getBytes() );
			
			 
			 
			
			// Read response from server
			String response = in.readLine();
			System.out.println("Received: " + response);

			// Close streams and socket
			out.close();
			in.close();
			socket.close();

			System.out.println("SSL Client stopped");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
