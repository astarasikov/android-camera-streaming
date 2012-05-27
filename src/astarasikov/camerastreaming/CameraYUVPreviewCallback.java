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

import astarasikov.camerastreaming.image.ImageUtils;
import android.hardware.Camera;

public class CameraYUVPreviewCallback implements Camera.PreviewCallback,
	ImageSource
{
	OnFrameRawCallback onFrameCallback;
	int width;
	int height;
	
	int rgbBuffer[];
	byte yuvBuffer[];
	
	synchronized void allocateBuffers() {
		int bufSize = width * height;
		int yuvSize = (bufSize * 3) / 2;
		rgbBuffer = new int[bufSize];		
		yuvBuffer = new byte[yuvSize];
	}
	
	public CameraYUVPreviewCallback(Camera camera) {
		Camera.Size params = camera.getParameters().getPreviewSize();
		width = params.width;
		height = params.height;
		allocateBuffers();
		camera.addCallbackBuffer(yuvBuffer);
	}
	
	@Override
	public synchronized void onPreviewFrame(byte[] data, Camera camera) 
	{
		if (onFrameCallback != null) {
			ImageUtils.decodeYUV420SP(rgbBuffer, data, width, height);
			onFrameCallback.onFrame(rgbBuffer, width, height);
		}
		camera.addCallbackBuffer(yuvBuffer);
	}

	@Override
	public synchronized void setOnFrameCallback(OnFrameRawCallback callback) {
		this.onFrameCallback = callback;
	}

	@Override
	public void setOnFrameBitmapCallback(OnFrameBitmapCallback callback) {
		
	}

	@Override
	public void close() {
		rgbBuffer = null;
		yuvBuffer = null;
	}
}
