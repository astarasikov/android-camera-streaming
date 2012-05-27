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
package astarasikov.camerastreaming;

import android.graphics.Bitmap;

public interface ImageSource {
	public void setOnFrameCallback(OnFrameRawCallback callback);
	public void setOnFrameBitmapCallback(OnFrameBitmapCallback callback);
	
	public static interface OnFrameRawCallback {
		public void onFrame(int rgb[], int width, int height);
	}
	
	public static interface OnFrameBitmapCallback {
		public void onFrame(Bitmap bitmap);
	}
	
	public void close();
}
