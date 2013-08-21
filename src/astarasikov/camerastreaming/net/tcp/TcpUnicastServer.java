/*
 * This file is part of CameraStreaming application.
 * CameraStreaming is an application for Android for streaming
 * video over MJPEG and applying DSP effects like convolution
 *
 * Copyright (C) 2012 Alexander Tarasikov <alexander.tarasikov@gmail.com>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package astarasikov.camerastreaming.net.tcp;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.util.Log;
import astarasikov.camerastreaming.ImageSink;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

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
