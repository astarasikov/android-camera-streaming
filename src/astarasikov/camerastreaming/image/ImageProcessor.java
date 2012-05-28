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

import java.util.ArrayList;
import java.util.List;

import astarasikov.camerastreaming.R;
import astarasikov.camerastreaming.image.ImageUtils.Kernel2D;
import astarasikov.camerastreaming.image.VJFaceDetector.VJFace;
import astarasikov.camerastreaming.utils.PreferenceHelper;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;
import android.util.Log;

public class ImageProcessor {
	final static String LOG_TAG = ImageProcessor.class.getSimpleName();
	
	PreferenceHelper mPreferenceHelper;
	Context mContext;
	Paint mPaint;
	int mLastWidth;
	int mLastHeight;
	int mMaxFaces;
	
	boolean mArEffects;
	
	boolean mUseJavaVJ;
	FaceDetector mFaceDetector;
	Face mFaces[];
	
	VJFaceDetector mVJFaceDetector;
	VJFace mVJFaces[];
	
	List<FaceOverlayEffect> mFaceEffects;

	int mFilterBuffer[];
	boolean mFilter;
	Kernel2D mFilterKernel;
	
	void initFaceEffects() {
		List<FaceOverlayEffect> effects = new ArrayList<FaceOverlayEffect>(4);
		
		if (mPreferenceHelper.booleanPreference(R.string.key_ar_coolface,
				false))
		{
			effects.add(new FaceOverlayEffect.CoolFaceEffect(mContext));
		}
		if (mPreferenceHelper.booleanPreference(R.string.key_ar_hat,
				false))
		{
			effects.add(new FaceOverlayEffect.HatEffect(mContext));
		}
		if (mPreferenceHelper.booleanPreference(R.string.key_ar_beard,
				false))
		{
			effects.add(new FaceOverlayEffect.BeardEffect(mContext));
		}
		if (mPreferenceHelper.booleanPreference(R.string.key_ar_moustache,
				false))
		{
			effects.add(new FaceOverlayEffect.MoustacheEffect(mContext));
		}
	
		mFaceEffects = effects;
	}
	
	void initFilter() {
		mFilter = mPreferenceHelper.booleanPreference(
				R.string.key_pref_dsp_use_filter, false);
		String filter = mPreferenceHelper.stringPreference(
				R.string.key_pref_dsp_filter,
				"gaussian-blur");
						
		if (filter.equals("box-blur")) {
			mFilterKernel = Kernel2D.BoxBlur();
		}
		else if (filter.equals("emboss")) {
			mFilterKernel = Kernel2D.Emboss();		
		}
		else if (filter.equals("sharpen")) {
			mFilterKernel = Kernel2D.Sharpen();
		}
		else if (filter.equals("edge-detection")) {
			mFilterKernel = Kernel2D.EdgeDetection();
		}
		else {
			mFilterKernel = Kernel2D.GaussianBlur();
		}
	}
	
	public ImageProcessor(Context context, PreferenceHelper preferenceHelper) {
		mContext = context;
		mPreferenceHelper = preferenceHelper;
		
		mArEffects = mPreferenceHelper.booleanPreference
				(R.string.key_ar_effects, false);
		mUseJavaVJ = mPreferenceHelper.booleanPreference
				(R.string.key_pref_use_java_vj, false);
		mMaxFaces = mPreferenceHelper.intPreference
				(R.string.key_ar_max_faces, 1);
		
		mPaint = new Paint();
		mPaint.setColor(Color.CYAN);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeWidth(3);
		mPaint.setTextSize(20);
		mPaint.setTypeface(Typeface.DEFAULT_BOLD);
		
		initFaceEffects();
		initFilter();
	}
	
	protected void reallocNativeFaceDetector(int newWidth, int newHeight) {
		if (
				(mFaceDetector != null)
				&& (mLastHeight == newHeight)
				&& (mLastWidth == newWidth)
			)
		{
			return;
		}
		
		mLastWidth = newWidth;
		mLastHeight = newHeight;
		
		mFaceDetector = new FaceDetector(mLastWidth, mLastHeight, mMaxFaces);
		mFaces = new Face[mMaxFaces];
	}
	
	void processNativeFaceDetector(Bitmap bitmap, Canvas canvas) {
		int numFaces = mFaceDetector.findFaces(bitmap, mFaces);
		for (int i = 0; i < numFaces; i++) {
			Face f = mFaces[i];
			PointF pf = new PointF();
			f.getMidPoint(pf);

			int dEyes = (int)f.eyesDistance();
			int x = (int)pf.x;
			int y = (int)pf.y;
			
			canvas.drawRect(x - dEyes, y - dEyes, x + dEyes, y + dEyes, mPaint);

			for (FaceOverlayEffect effect : mFaceEffects) {
				effect.process(x, y, dEyes, canvas, mPaint);
			}
		}
	}
	
	void reallocVJFaceDetector(int newWidth, int newHeight) {
		if (
				(mVJFaceDetector != null)
				&& (mLastHeight == newHeight)
				&& (mLastWidth == newWidth)
			)
		{
			return;
		}
		
		mLastWidth = newWidth;
		mLastHeight = newHeight;
		
		try {
			mVJFaceDetector = new VJFaceDetector(mContext, mLastWidth,
				mLastHeight, mMaxFaces);
			mVJFaces = new VJFace[mMaxFaces];
		}
		catch (Exception e) {
			Log.e(LOG_TAG, "failed to allocate VJ detector", e);
			mVJFaceDetector = null;
			mVJFaces = null;
		}
	}
	
	void processVJFaceDetector(Bitmap bitmap, Canvas canvas) {
		if (mVJFaceDetector == null) {
			return;
		}
		
		int numFaces = mVJFaceDetector.findFaces(bitmap, mVJFaces);
		for (int i = 0; i < numFaces; i++) {
			VJFace f = mVJFaces[i];
			int x = f.centerX;
			int y = f.centerY;
			int dEyes = f.eyesDistance;
			
			canvas.drawRect(x - dEyes, y - dEyes, x + dEyes, y + dEyes, mPaint);

			for (FaceOverlayEffect effect : mFaceEffects) {
				effect.process(x, y, dEyes, canvas, mPaint);
			}
		}
	}
	
	Bitmap processArEffects(Bitmap bitmap, Bitmap filtered) {
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		
		Canvas c = new Canvas(filtered);
		
		if (mUseJavaVJ) {
			reallocVJFaceDetector(width, height);
			processVJFaceDetector(bitmap, c);
		}
		else {
			reallocNativeFaceDetector(width, height);
			processNativeFaceDetector(bitmap, c);
		}
		
		return filtered;
	}
	
	void reallocFilterBuffer(int width, int height) {
		int length = width * height;
		if (mFilterBuffer == null || mFilterBuffer.length != length) {
			mFilterBuffer = new int[length];
		}
	}
	
	public Bitmap filter(int rgbBuffer[], int width, int height) {
		if (!mFilter) {
			return null;
		}
		reallocFilterBuffer(width, height);

		Kernel2D.Convolve2D(mFilterKernel, rgbBuffer, mFilterBuffer,
					width, height);
		return Bitmap.createBitmap(mFilterBuffer, width, height,
				Bitmap.Config.RGB_565);
	}
	
	public Bitmap process(int rgbBuffer[], int width, int height, int angle) {
		Bitmap.Config rgbConfig = Bitmap.Config.RGB_565;
		Bitmap bitmap = Bitmap.createBitmap(rgbBuffer, width, height,
				rgbConfig);
		Bitmap filtered = filter(rgbBuffer, width, height);
		
		if (filtered == null) {
			filtered = bitmap;
		}
		
		if (angle != 0) {
			Matrix m = new Matrix();
			m.postRotate(angle);
			bitmap = Bitmap.createBitmap(bitmap, 0, 0,
					width, height, m, false);
			if (filtered != null) {
				filtered = Bitmap.createBitmap(filtered, 0, 0,
						width, height, m, false);
			}
		}
		else {
			bitmap = bitmap.copy(rgbConfig, true);
			filtered = filtered.copy(rgbConfig, true);
		}
				
		if (mArEffects) {
			filtered = processArEffects(bitmap, filtered);
		}
				
		return filtered;		
	}
}
