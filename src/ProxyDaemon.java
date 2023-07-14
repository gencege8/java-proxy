import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.net.ssl.SSLSocket;

public class ProxyDaemon {

	final static ArrayList<String> forbiddenAddresses = new ArrayList<>();
	final static HashMap<String,byte[]> dataCache=new HashMap<String,byte[]>();
    final static HashMap<String,String> infCache=new HashMap<String,String>();
    public boolean flag=false;
    public void runApp() throws Exception {
		
		forbiddenAddresses.add("www.yandex.com.tr");
		forbiddenAddresses.add("www.apple.com");
		forbiddenAddresses.add("www.facebook.com");
		//forbiddenAddresses.add("www.example.com");
		ServerSocket welcomeSocket = new ServerSocket(8080);
		
		while (true) {
			Socket connectionSocket = welcomeSocket.accept();
			new ServerHandler(connectionSocket);
				
		}
		
		

	}

}

class ServerHandler implements Runnable {

	Socket clientSocket;

	DataInputStream inFromClient;
	DataOutputStream outToClient;
	String host;
	String path;
	String hostCon;
	String pathCon;
	PrinterClass pC;
	
	public ServerHandler(Socket s) {
		try {
			clientSocket = s;
			pC = new PrinterClass();

			pC.add("A connection from a client is initiated...");

			inFromClient = new DataInputStream(s.getInputStream());
			outToClient = new DataOutputStream(s.getOutputStream());

			new Thread(this).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		try {
			String hd = getHeader(inFromClient);

			int sp1 = hd.indexOf(' ');
			int sp2 = hd.indexOf(' ', sp1 + 1);
			int eol = hd.indexOf('\r');
			int forCon=hd.indexOf(' ');
			int forConEnd=hd.indexOf(' ',forCon+1);
			String reqHeaderRemainingLines = hd.substring(eol + 2);

			MimeHeader reqMH = new MimeHeader(reqHeaderRemainingLines);

			String url = hd.substring(sp1 + 1, sp2);
			String urlForConPre=hd.substring(forCon+1,forConEnd);

			String method = hd.substring(0, sp1);
			host = reqMH.get("Host");
			int hostIden=host.indexOf(':');
			reqMH.put("Connection", "close");
			System.out.println(urlForConPre);
			int forConHost=urlForConPre.indexOf(':');
			String urlForConFin=urlForConPre.substring(0, forConHost);
			hostCon=urlForConFin;
			String tmpPath;
			
			String tmpHost;
			
			
			if(method.equalsIgnoreCase("connect")) {
				String hostMan=host.substring(0,hostIden);
				reqMH.put("Host", hostMan);
				tmpHost=host;
			}
			else {
				URL u = new URL(url);
				tmpPath = u.getPath();

				tmpHost = u.getHost();
				path = ((tmpPath == "") ? "/" : tmpPath);
			}

			if (ProxyDaemon.forbiddenAddresses.contains(host)) {
				pC.add("Connection blocked to the host due to the proxy policy");
				outToClient.writeBytes(createErrorPage(401,"Not authorized", "WEBSITE_FORBIDDEN"));
			} else if (host.equals(tmpHost)) {
				if (method.equalsIgnoreCase("get")) {
					pC.add("Client requests...\r\nHost: " + host + "\r\nPath: " + path);
					handleProxyGET(url, reqMH);
				}
				else if(method.equalsIgnoreCase("post")) {
					pC.add("Client requests...\r\nHost: " + host + "\r\nPath: " + path);
					handleProxyPOST(url, reqMH);
				}
				else if(method.equalsIgnoreCase("connect")) {
					pC.add("Client requests...\r\nHost: " + host + "\r\nPath: " + path);
					handleProxyCONNECT(urlForConPre, reqMH);
				}
				else if(method.equalsIgnoreCase("head")) {
					pC.add("Client requests...\r\nHost: " + host + "\r\nPath: " + path);
					handleProxyHEAD(url, reqMH);
				}
				else {
					pC.add("Requested method " + method + " is not allowed on proxy server");
					outToClient.writeBytes(createErrorPage(405, "Method Not Allowed", method));
				}
			} else {
				pC.add("Error for request: " + url);
			}
			pC.removeThread();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void handleProxyGET(String url, MimeHeader reqMH) {
		try {
			pC.add("\r\nInitiating the server connection");
			Socket sSocket = new Socket(host, 80);
			DataInputStream inFromServer = new DataInputStream(sSocket.getInputStream());
			DataOutputStream outToServer = new DataOutputStream(sSocket.getOutputStream());
			if(ProxyDaemon.infCache.containsKey(host)) {
				reqMH.put("If-Modified-Since", ProxyDaemon.infCache.get(host));
				reqMH.put("User-Agent", reqMH.get("User-Agent") + " via CSE471 Proxy");
				pC.add("\r\nSending to server...\r\n" + "HEAD " + path + " HTTP/1.1\r\n" + reqMH + "\r\n");

				outToServer.writeBytes("HEAD " + path + " HTTP/1.1\r\n" + reqMH + "\r\n");

				pC.add("HTTP request sent to: " + host);

				ByteArrayOutputStream bAOS = new ByteArrayOutputStream(10000);

				int a;

				byte[] buffer = new byte[1024];

				while ((a = inFromServer.read(buffer)) != -1) {
					bAOS.write(buffer, 0, a);
				}

				byte[] response = bAOS.toByteArray();

				String rawResponse = new String(response);

				String responseHeader = rawResponse.substring(0, rawResponse.indexOf("\r\n\r\n"));
				pC.add("\r\nResponse Header\r\n" + responseHeader);

				
				int forStat1=responseHeader.indexOf(' ');
				int forStat2=responseHeader.indexOf('\r');
				
				String inf=responseHeader.substring(forStat1+1, forStat2);
				if(inf.equals("304 Not Modified")) {
					if(ProxyDaemon.dataCache.containsKey(host)){

						pC.add("\r\n\r\nGot " + ProxyDaemon.dataCache.get(host).length + " bytes of cached response data...\r\n"
								+ "Sending it back to the client...\r\n");

						outToClient.write(ProxyDaemon.dataCache.get(host));
						
					}
				}
				else {
					reqMH.remove("If-Modified-Since");
					reqMH.put("User-Agent", reqMH.get("User-Agent") + " via CSE471 Proxy");

					pC.add("\r\nSending to server...\r\n" + "GET " + path + " HTTP/1.1\r\n" + reqMH + "\r\n");

					outToServer.writeBytes("GET " + path + " HTTP/1.1\r\n" + reqMH + "\r\n");

					pC.add("HTTP request sent to: " + host);

					ByteArrayOutputStream bAOS1 = new ByteArrayOutputStream(10000);

					int a1;

					byte[] buffer1 = new byte[1024];

					while ((a1 = inFromServer.read(buffer1)) != -1) {
						bAOS1.write(buffer, 0, a1);
					}
					byte[] response1 = bAOS1.toByteArray();

					String rawResponse1 = new String(response1);

					String responseHeader1 = rawResponse1.substring(0, rawResponse1.indexOf("\r\n\r\n"));
					
					cacheHandler(rawResponse1, host, response1);
					pC.add("\r\nResponse Header\r\n" + responseHeader1);

					pC.add("\r\n\r\nGot " + response1.length + " bytes of response data...\r\n"
							+ "Sending it back to the client...\r\n");

					outToClient.write(response1);

					
				}
			}
			else {
				reqMH.remove("If-Modified-Since");
                reqMH.put("User-Agent", reqMH.get("User-Agent") + " via CSE471 Proxy");
                pC.add("\r\nSending to server...\r\n" + "GET " + path + " HTTP/1.1\r\n" + reqMH + "\r\n");
                outToServer.writeBytes("GET " + path + " HTTP/1.1\r\n" + reqMH + "\r\n");
                pC.add("HTTP request sent to: " + host);
                ByteArrayOutputStream bAOS = new ByteArrayOutputStream(10000);

    			int a;

    			byte[] buffer = new byte[1024];

    			while ((a = inFromServer.read(buffer)) != -1) {
    				bAOS.write(buffer, 0, a);
    			}
    			byte[] response = bAOS.toByteArray();

    			String rawResponse = new String(response);

    			String responseHeader = rawResponse.substring(0, rawResponse.indexOf("\r\n\r\n"));
    			
    			cacheHandler(rawResponse, host, response);
                pC.add("\r\nResponse Header\r\n" + responseHeader);
                pC.add("\r\n\r\nGot " + response.length + " bytes of response data...\r\n"
                        + "Sending it back to the client...\r\n");
                outToClient.write(response);

			}
			outToClient.close();

			sSocket.close();

			pC.add("Served http://" + host + path + "\r\nExiting ServerHelper thread...\r\n"
					+ "\r\n----------------------------------------------------" + "\r\n");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private void handleProxyHEAD(String url, MimeHeader reqMH) {
		try {
			pC.add("\r\nInitiating the server connection");
			Socket sSocket = new Socket(host, 80);
			DataInputStream inFromServer = new DataInputStream(sSocket.getInputStream());
			DataOutputStream outToServer = new DataOutputStream(sSocket.getOutputStream());

			reqMH.put("User-Agent", reqMH.get("User-Agent") + " via CSE471 Proxy");

			pC.add("\r\nSending to server...\r\n" + "HEAD " + path + " HTTP/1.1\r\n" + reqMH + "\r\n");

			outToServer.writeBytes("HEAD " + path + " HTTP/1.1\r\n" + reqMH + "\r\n");

			pC.add("HTTP request sent to: " + host);

			ByteArrayOutputStream bAOS = new ByteArrayOutputStream(10000);

			int a;

			byte[] buffer = new byte[1024];

			while ((a = inFromServer.read(buffer)) != -1) {
				bAOS.write(buffer, 0, a);
			}

			byte[] response = bAOS.toByteArray();

			String rawResponse = new String(response);

			String responseHeader = rawResponse.substring(0, rawResponse.indexOf("\r\n\r\n"));

			pC.add("\r\nResponse Header\r\n" + responseHeader);

			pC.add("\r\n\r\nGot " + response.length + " bytes of response data...\r\n"
					+ "Sending it back to the client...\r\n");

			outToClient.write(response);

			outToClient.close();

			sSocket.close();

			pC.add("Served http://" + host + path + "\r\nExiting ServerHelper thread...\r\n"
					+ "\r\n----------------------------------------------------" + "\r\n");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private void handleProxyPOST(String url, MimeHeader reqMH) {
		try {
			pC.add("\r\nInitiating the server connection");
			Socket sSocket = new Socket(host, 80);
			DataInputStream inFromServer = new DataInputStream(sSocket.getInputStream());
			DataOutputStream outToServer = new DataOutputStream(sSocket.getOutputStream());

			reqMH.put("User-Agent", reqMH.get("User-Agent") + " via CSE471 Proxy");

			pC.add("\r\nSending to server...\r\n" + "POST " + path + " HTTP/1.1\r\n" + reqMH + "\r\n");

			outToServer.writeBytes("POST " + path + " HTTP/1.1\r\n" + reqMH + "\r\n");

			pC.add("HTTP request sent to: " + host);

			ByteArrayOutputStream bAOS = new ByteArrayOutputStream(10000);

			int a;

			byte[] buffer = new byte[1024];

			while ((a = inFromServer.read(buffer)) != -1) {
				bAOS.write(buffer, 0, a);
			}

			byte[] response = bAOS.toByteArray();

			String rawResponse = new String(response);

			String responseHeader = rawResponse.substring(0, rawResponse.indexOf("\r\n\r\n"));

			pC.add("\r\nResponse Header\r\n" + responseHeader);

			pC.add("\r\n\r\nGot " + response.length + " bytes of response data...\r\n"
					+ "Sending it back to the client...\r\n");

			outToClient.write(response);

			outToClient.close();

			sSocket.close();

			pC.add("Served http://" + host + path + "\r\nExiting ServerHelper thread...\r\n"
					+ "\r\n----------------------------------------------------" + "\r\n");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private void handleProxyCONNECT(String url, MimeHeader reqMH) {
		try {
			pC.add("\r\nInitiating the server connection");
			Socket sSocket = new Socket(hostCon, 443);
			
			DataInputStream inFromServer = new DataInputStream(sSocket.getInputStream());
			DataOutputStream outToServer = new DataOutputStream(sSocket.getOutputStream());
			

			reqMH.put("User-Agent", reqMH.get("User-Agent") + " via CSE471 Proxy");

			pC.add("\r\nSending to server...\r\n" + "CONNECT " + host + " HTTP/1.1\r\n" + reqMH + "\r\n");

			outToServer.writeBytes("CONNECT " + host + " HTTP/1.1\r\n" + reqMH + "\r\n");

			pC.add("HTTP request sent to: " + host);

			ByteArrayOutputStream bAOS = new ByteArrayOutputStream(10000);

			int a;

			byte[] buffer = new byte[1024];

			while ((a = inFromServer.read(buffer)) != -1) {
				bAOS.write(buffer, 0, a);
			}

			byte[] response = bAOS.toByteArray();

			String rawResponse = new String(response);

			String responseHeader = rawResponse;

			pC.add("\r\nResponse Header\r\n" + responseHeader);

			pC.add("\r\n\r\nGot " + response.length + " bytes of response data...\r\n"
					+ "Sending it back to the client...\r\n");

			outToClient.write(response);
			
			pC.add("Served http://" + host + path + "\r\nExiting ServerHelper thread...\r\n"
					+ "\r\n----------------------------------------------------" + "\r\n");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private String createErrorPage(int code, String msg, String body) {
		String html_page = "<!DOCTYPE html>\r\n"
				+ "<body>\r\n"
				+ "<h1>\r\n"
				+code+" "+msg+"\r\n"
				+ "</h1>\r\n"
				+body+"\r\n"
				+ "</body>\r\n"
				+ "</html>";
		//  ADD -> create html page using method parameters
		
		
		
		MimeHeader mh = makeMimeHeader("text/html", html_page.length());
		HttpResponse hr = new HttpResponse(code, msg, mh);
		return hr + html_page;
	}

	private MimeHeader makeMimeHeader(String type, int length) {
		MimeHeader mh = new MimeHeader();
		Date d = new Date();
		TimeZone gmt = TimeZone.getTimeZone("GMT");
		SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy hh:mm:ss zzz");
		sdf.setTimeZone(gmt);
		//ADD-> add date, server, and content-type fields to mimeheader
		//
		//
		if (length >= 0)
			mh.put("Content-Length", String.valueOf(length));
		return mh;
	}

	public String getHeader(DataInputStream in) throws Exception {
		byte[] header = new byte[1024];

		int data;
		int h = 0;

		while ((data = in.read()) != -1) {
			header[h++] = (byte) data;

			if (header[h - 1] == '\n' && header[h - 2] == '\r' && header[h - 3] == '\n' && header[h - 4] == '\r') {
				break;
			}
		}

		return new String(header, 0, h);
	}


	private void cacheHandler(String servRes, String host, byte[] data) {
		if(servRes.contains("Last-Modified:")) {
			int first = servRes.indexOf("Last-Modified:");
			int last = servRes.indexOf('\r', first);
			String lastModified = servRes.substring(first+15, last);
			System.out.println("Date: " + lastModified);
			ProxyDaemon.infCache.put(host, lastModified);
			ProxyDaemon.dataCache.put(host, data);
		}
	}
}