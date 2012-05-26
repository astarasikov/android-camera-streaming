package astarasikov.camerastreaming.net.tcp;

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

import astarasikov.camerastreaming.ImageSink;

public class TcpUnicastServer implements ImageSink {
	final static String LOG_TAG = TcpUnicastServer.class.getSimpleName();

	final Thread mAcceptorThread;
	final ServerSocket mServerSocket;
	
	final ThreadPoolExecutor mExecutor;
	final List<Socket> mClients = Collections
			.synchronizedList(new LinkedList<Socket>());
	

	public TcpUnicastServer(int port, ThreadPoolExecutor executor)
			throws Exception
	{
		this.mExecutor = executor;
		mServerSocket = new ServerSocket(port);
		mServerSocket.setReuseAddress(true);
		mAcceptorThread = new Thread() {
			@Override
			public void run() {
				while (!isInterrupted()) {
					try {
						mClients.add(mServerSocket.accept());
					} catch (IOException e) {
						Log.e(LOG_TAG, "failed to accept socket", e);
					}
				}
			}
		};
		mAcceptorThread.start();
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
				mClients.remove(socket);
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
		synchronized (mClients) {
			for (Socket client : mClients) {
				Runnable tasklet = new SendBitmapTask(client, bitmap);
				mExecutor.submit(tasklet);
			}
		}
	}
	
	@Override
	public synchronized void close() {
		try {
			mServerSocket.close();
		} catch (IOException e) {
			Log.d(LOG_TAG, "failed to close the socket", e);
		}
		
		if (mAcceptorThread == null) {
			return;
		}
		if (!mAcceptorThread.isAlive()) {
			return;
		}
		
		mAcceptorThread.interrupt();
		try {
			mAcceptorThread.join();
		} catch (InterruptedException e) {
			Log.d(LOG_TAG, "failed to wait for thread completion", e);
		}
	}
}
