package astarasikov.camerastreaming.net.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

public class HttpServer {	
	public static enum ResponseCode {
		OK(200, "OK"),
		BAD_REQUEST(400, "Bad request"),
		UNAUTHORIZED(401, "Unauthorized"),
		FORBIDDEN(403, "Forbidden"),
		NOT_FOUND(404, "Not found"),
		INTERNAL_ERROR(500, "Internal error"),
		NOT_IMPLEMENTED(501, "Not implemented");
		
		private final int id;
		private final String string;
		
		public int getValue() {
			return id;
		}
		
		public String getString() {
			return string;
		}
		
		private ResponseCode(int id, String string) {
			this.id = id;
			this.string = string;
		}		
	}
	
	public static interface Handler {
		public boolean handle(Map<String, String> params,
				OutputStream outputStream) throws IOException;
	}

	final static String LOG_TAG = HttpServer.class.getSimpleName();
	
	final ServerSocket mServerSocket;
	final Thread mServerThread;
	
	final List<Thread> mHandlerThreads = new LinkedList<Thread>();
	final Map<String, Handler> mRequestHandlers =
			Collections.synchronizedMap(new HashMap<String, Handler>());
	
	class HandlerThread extends Thread {
		final Socket socket;
		Map<String, String> params = new HashMap<String, String>();
		Handler handler = null;
		OutputStream outputStream;
		InputStream inputStream;
		PrintStream printStream;
		
		public HandlerThread(Socket socket) {
			this.socket = socket;
		}
		
		protected void writeResponseCode(ResponseCode code) throws IOException {
			printStream.printf("HTTP/1.0 %d %s",
					code.getValue(), code.getString());
		}
		
		protected ResponseCode parseRequest() throws Exception {
			InputStreamReader isr = new InputStreamReader(inputStream);
			BufferedReader br = new BufferedReader(isr);
			String request = br.readLine();

			Pattern p = Pattern.compile("(GET|POST) (.*) HTTP/(\\d+\\.\\d+)");
			Matcher m = p.matcher(request);
			
			if (!m.matches()) {
				return ResponseCode.BAD_REQUEST;
			}
			
			String url = m.group(2);
			Log.i(LOG_TAG, String.format("Request '%s'", url));
			
			String path;
			int paramIdx = url.indexOf("?");
			if (paramIdx == -1) {
				path = url;
			}
			else {
				path = url.substring(0, paramIdx);
				String paramString = url.substring(paramIdx + 1);
				String paramKVpairs[] = paramString.split("&");
				for (String paramKV : paramKVpairs) {
					String paramAndValue[] = paramKV.split("=");
					if (paramAndValue.length == 2) {
						params.put(paramAndValue[0], paramAndValue[1]);
					}
				}
			}
			if (path.startsWith("/")) {
				path = path.substring(1);
			}
			
			handler = mRequestHandlers.get(path);
			if (handler == null) {
				return ResponseCode.NOT_FOUND;
			}
			
			return ResponseCode.OK;
		}
		
		@Override
		public void run() {
			try {
				outputStream = socket.getOutputStream();
				inputStream = socket.getInputStream();
				printStream = new PrintStream(outputStream);
				
				ResponseCode parseResult = parseRequest();
				if (parseResult != ResponseCode.OK) {
					writeResponseCode(parseResult);
				}
				
				if (handler != null) {
					writeResponseCode(ResponseCode.OK);				
					handler.handle(params, outputStream);
				}
				
				outputStream.flush();
				outputStream.close();
				inputStream.close();
			}
			catch (InterruptedException e) {
				return;
			}
			catch (Exception e) {
				Log.d(LOG_TAG, "error handling http request", e);
			}

			mHandlerThreads.remove(this);
		}
	}

	public HttpServer(int port) throws Exception {
		mServerSocket = new ServerSocket(port);
		mServerThread = new Thread() {
			@Override
			public void run() {
				while (!isInterrupted()) {
					try {
						Socket socket = mServerSocket.accept();
						HandlerThread clientThread = new HandlerThread(socket);
						synchronized (mHandlerThreads) {
							mHandlerThreads.add(clientThread);
						}
						clientThread.start();
					} catch (IOException e) {
						Log.e(LOG_TAG, "failed to accept socket", e);
					}
				}
			}
		};
		mServerThread.start();
	}
	
	public void stop() {
		try {
			mServerThread.interrupt();
			mServerThread.join();
		}
		catch (InterruptedException e) {
			Log.e(LOG_TAG, "failed to stop thread", e);

		}
		
		try {
			mServerSocket.close();
		}
		catch (IOException e) {
			Log.e(LOG_TAG, "failed to close socket", e);
		}
		
		synchronized (mHandlerThreads) {
			for (Thread t : mHandlerThreads) {
				if (t == null) {
					continue;
				}
				
				if (!t.isAlive()) {
					continue;
				}
				
				t.interrupt();
				try {
					t.join();
				} catch (InterruptedException e) {
					Log.e(LOG_TAG, "failed to stop handler thread", e);
				}
			}
		}
	}
	
	public void addHandler(String path, Handler handler) {
		mRequestHandlers.put(path, handler);
	}
	
	public void removeHandler(String path, Handler handler) {
		mRequestHandlers.remove(path);
	}
}
