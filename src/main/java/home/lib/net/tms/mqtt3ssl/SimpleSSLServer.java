package home.lib.net.tms.mqtt3ssl;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

class SimpleSSLServer {

	private static final int PORT = 8443;
	private static final String KEYSTORE_PASSWORD = "test12345";
	private static final String KEYSTORE_PATH = "C:\\Users\\richard\\Downloads\\keystore.jks";

	public static void start(String[] args) {
		try {
			// Load the KeyStore
			KeyStore keyStore = KeyStore.getInstance("JKS");
			FileInputStream keyStoreStream = new FileInputStream(KEYSTORE_PATH);
			keyStore.load(keyStoreStream, KEYSTORE_PASSWORD.toCharArray());

			// Create KeyManagerFactory
			KeyManagerFactory keyManagerFactory = KeyManagerFactory
					.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			keyManagerFactory.init(keyStore, KEYSTORE_PASSWORD.toCharArray());

			// Initialize SSLContext
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

			// Create SSLServerSocketFactory
			SSLServerSocketFactory serverSocketFactory = sslContext.getServerSocketFactory();
			ServerSocket serverSocket = (ServerSocket) serverSocketFactory.createServerSocket(PORT);

			System.out.println("SSL server started on port " + PORT);

			// Wait for client connections
			while (true) {
				Socket sslSocket = (Socket) serverSocket.accept();
				handleClient(sslSocket);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void handleClient(Socket sslSocket) {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(sslSocket.getOutputStream()))) {

			String line;
			while ((line = reader.readLine()) != null) {
				System.out.println("Received: " + line);
				writer.write("Echo: " + line);
				writer.newLine();
				writer.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
