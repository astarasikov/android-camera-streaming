package my.test.net.tcp;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.net.Socket;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.util.Log;

import my.test.ImageSink;

public class TcpUnicastServer implements ImageSink {
	final static String LOG_TAG = TcpUnicastServer.class.getSimpleName();

	final ThreadPoolExecutor executor;
	final List<Socket> clients = Collections
			.synchronizedList(new LinkedList<Socket>());
	

	public TcpUnicastServer(int port, ThreadPoolExecutor executor)
			throws Exception
	{
		this.executor = executor;
		final ServerSocket ss = new ServerSocket(port);
		ss.setReuseAddress(true);
		new Thread() {
			@Override
			public void run() {
				while (!isInterrupted()) {
					try {
						clients.add(ss.accept());
					} catch (IOException e) {
						Log.e(LOG_TAG, "failed to accept socket", e);
					}
				}
			}
		}.start();
	}
	
	class SendBitmapTask implements Runnable 
	{
		final Bitmap bitmap;
		final Socket socket;
		
		public SendBitmapTask(Socket socket, Bitmap bitmap) {
			this.socket = socket;
			this.bitmap = bitmap;
		}
		
		@Override
		public void run() {
			try {
				synchronized(socket) {
					bitmap.compress(CompressFormat.JPEG, 60,
							socket.getOutputStream());
				}
			}
			catch (IOException e) {
				clients.remove(socket);
				try {
					socket.close();
				} catch (IOException closeException) {
					Log.e(LOG_TAG, "Failed to close socket", closeException);
				}
				
				Log.e(LOG_TAG, "error sending image", e);
			}			
		}
	}

	@Override
	public void send(Bitmap bitmap) {
		synchronized (clients) {
			for (Socket client : clients) {
				Runnable tasklet = new SendBitmapTask(client, bitmap);
				executor.submit(tasklet);
			}
		}
	}
}
