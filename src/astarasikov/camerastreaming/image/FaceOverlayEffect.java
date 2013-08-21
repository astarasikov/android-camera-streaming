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
package astarasikov.camerastreaming.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import astarasikov.camerastreaming.R;

abstract class FaceOverlayEffect {
	static class CoolFaceEffect extends FaceOverlayEffect {
		public CoolFaceEffect(Context context) {
			initialize(R.drawable.coolface, context);
		}
		
		@Override
		public void process(
				int x, int y, int dEyes,
				Canvas canvas, Paint paint)
		{
			int newSize = (int)(2.5 * dEyes);
			int shiftX = (int)(1.25 * dEyes);
			int shiftY = (dEyes);
			
			Bitmap face = fetchBitmap(newSize, newSize);
			canvas.drawBitmap(face, x - shiftX, y - shiftY, paint);		
		}
	}

	static class BeardEffect extends FaceOverlayEffect {
		public BeardEffect(Context context) {
			initialize(R.drawable.beard, context);
		}
		
		@Override
		public void process(
				int x, int y, int dEyes,
				Canvas canvas, Paint paint)
		{
			int newSize = (2 * dEyes);
			int shiftX = (-1 * dEyes);
			int shiftY = (int)(1.15 * dEyes);
			
			Bitmap face = fetchBitmap(newSize, newSize);
			canvas.drawBitmap(face, x + shiftX, y + shiftY, paint);		
		}
	}

	static class HatEffect extends FaceOverlayEffect {
		public HatEffect(Context context) {
			initialize(R.drawable.hat, context);
		}
		
		@Override
		public void process(
				int x, int y, int dEyes,
				Canvas canvas, Paint paint)
		{
			int newSize = (int)(3.0 * dEyes);
			int shiftX = (int)(1.5 * dEyes);
			int shiftY = (int)(-3.5 * dEyes);
	
			Bitmap face = fetchBitmap(newSize, newSize);
			canvas.drawBitmap(face, x - shiftX, y + shiftY, paint);		
		}
	}

	static class MoustacheEffect extends FaceOverlayEffect {
		public MoustacheEffect(Context context) {
			initialize(R.drawable.moustache, context);
		}
		
		@Override
		public void process(
				int x, int y, int dEyes,
				Canvas canvas, Paint paint)
		{
			int newSizeX = (int)(2.0 * dEyes);
			int newSizeY = (int)(0.50 * dEyes);
			
			int shiftX = (int)(-1.0 * dEyes);
			int shiftY = (int)(0.40 * dEyes);
	
			Bitmap face = fetchBitmap(newSizeX, newSizeY);
			canvas.drawBitmap(face, x + shiftX, y + shiftY, paint);		
		}
	}

	int mResourceId;
	Context mContext;
	Bitmap mCachedBitmap;
	
	protected void fetchOriginalBitmap() {
		mCachedBitmap = BitmapFactory.decodeResource
				(mContext.getResources(), mResourceId);
	}
	
	protected Bitmap fetchBitmap(int width, int height) {
		if (mCachedBitmap == null) {
			fetchOriginalBitmap();
		}
		
		boolean widthGood = Math.abs(width - mCachedBitmap.getWidth()) <
					width >> 4;
		boolean heightGood = Math.abs(height - mCachedBitmap.getHeight()) <
				height >> 4;
		boolean cacheIsGood = widthGood && heightGood;
		
		if (!cacheIsGood) {
			fetchOriginalBitmap();
			mCachedBitmap = Bitmap.createScaledBitmap(mCachedBitmap,
					width, height, false);
		}
		
		return mCachedBitmap;
	}
	
	protected void initialize(int resourceId, Context context) {
		this.mContext = context;
		this.mResourceId = resourceId;
	}
	
	public abstract void process(int x, int y,
			int dEyes, Canvas canvas, Paint paint);
}
