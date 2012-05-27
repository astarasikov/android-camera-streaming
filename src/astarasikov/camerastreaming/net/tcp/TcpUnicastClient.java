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

import java.net.Socket;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import astarasikov.camerastreaming.ImageSource;

public class TcpUnicastClient implements ImageSource {
	final static String LOG_TAG = TcpUnicastClient.class.getSimpleName();
	
	OnFrameRawCallback rawCallback;
	OnFrameBitmapCallback bitmapCallback;
	Socket socket;
	Thread thread;
	
	protected synchronized void readFrame() {
		if (socket == null || socket.isClosed()) {
			return;
		}
		try {
			Bitmap bitmap = BitmapFactory.decodeStream(socket.getInputStream());
			if (bitmapCallback != null) {
				bitmapCallback.onFrame(bitmap);
			}
			
			if (rawCallback != null) {
				int w = bitmap.getWidth();
				int h = bitmap.getHeight();
				int rgb[] = new int[w * h];
				bitmap.getPixels(rgb, 0, w, 0, 0, w, h);
				rawCallback.onFrame(rgb, w, h);
			}
		}
		catch (Exception e) {
			Log.d(LOG_TAG, "failed to decode bitmap", e);
		}
	}
	
	@Override
	public synchronized void setOnFrameCallback(OnFrameRawCallback callback) {
		this.rawCallback = callback;
	}
	
	@Override
	public void setOnFrameBitmapCallback(OnFrameBitmapCallback callback) {
		this.bitmapCallback = callback;
	}
	
	public synchronized void connect(String dstName, int port) throws Exception {
		close();
		socket = new Socket(dstName, port);
		thread = new Thread() {
			@Override
			public void run() {
				while (!isInterrupted()) {
					readFrame();
				}
			}
		};
		thread.start();
	}
	
	@Override
	public synchronized void close() {
		if (socket == null || socket.isClosed()) {
			return;
		}
		
		if (thread != null) {
			try {
				thread.interrupt();
				thread.join();
				thread = null;
			}
			catch (Exception e) {
				Log.d(LOG_TAG, "failed to wait for thread to die", e);
			}
		}
		
		try {
			socket.close();
		}
		catch (Exception e) {
			Log.e(LOG_TAG, "failed to close socket", e);
		}
		socket = null;
	}
}
