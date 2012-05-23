package my.test.image;

import java.util.ArrayList;
import java.util.List;

import my.test.R;
import my.test.utils.PreferenceHelper;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;

public class ImageProcessor {
	static abstract class FaceOverlayEffect {
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
	
	static class FaceEffect extends FaceOverlayEffect {
		public FaceEffect(Context context) {
			initialize(R.drawable.coolface, context);
		}
		
		@Override
		public void process(
				int x, int y, int dEyes,
				Canvas canvas, Paint paint)
		{
			int newSize = (int)(2.5 * dEyes);
			int shift = (int)(1.25 * dEyes);
			Bitmap face = fetchBitmap(newSize, newSize);
			canvas.drawBitmap(face, x - shift, y - shift, paint);		
		}
		
	}
	
	PreferenceHelper mPreferenceHelper;
	Context mContext;
	Paint mPaint;
	int mLastWidth;
	int mLastHeight;
	
	boolean mArEffects;
	
	FaceDetector mFaceDetector;
	Face mFaces[];
	
	List<FaceOverlayEffect> mFaceEffects;

	void initFaceEffects() {
		List<FaceOverlayEffect> effects = new ArrayList<FaceOverlayEffect>(4);
		
		if (mPreferenceHelper.booleanPreference(R.string.key_ar_coolface,
				false))
		{
			effects.add(new FaceEffect(mContext));
		}
	
		mFaceEffects = effects;
	}
	
	public ImageProcessor(Context context, PreferenceHelper preferenceHelper) {
		mContext = context;
		mPreferenceHelper = preferenceHelper;
		
		mArEffects = mPreferenceHelper.booleanPreference
				(R.string.key_ar_effects, false);
		
		mPaint = new Paint();
		mPaint.setColor(Color.CYAN);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeWidth(3);
		mPaint.setTextSize(20);
		mPaint.setTypeface(Typeface.DEFAULT_BOLD);
		
		initFaceEffects();
	}
	
	protected void reallocFaceDetector(int newWidth, int newHeight) {
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
		
		mFaceDetector = new FaceDetector(mLastWidth, mLastHeight, 1);
		mFaces = new Face[1];
	}
	
	Bitmap processArEffects(Bitmap bitmap) {
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		reallocFaceDetector(width, height);
		
		Canvas c = new Canvas(bitmap);

		/* some debugging code to test scaling */
		c.drawLine(0, 0, width, height, mPaint);
		c.drawLine(height, 0, width, 0, mPaint);		
		/* end of scaling code */
		
		int numFaces = mFaceDetector.findFaces(bitmap, mFaces);
		if (numFaces > 0) {
			Face f = mFaces[0];
			PointF pf = new PointF();
			f.getMidPoint(pf);

			c.drawText("Found a face", 0, 0, mPaint);
			int dEyes = (int)f.eyesDistance();
			int x = (int)pf.x;
			int y = (int)pf.y;
			
			int x0 = x - dEyes;
			int y0 = y - dEyes;
			int x1 = x + dEyes;
			int y1 = y + dEyes;
			c.drawRect(x0, y0, x1, y1, mPaint);

			for (FaceOverlayEffect effect : mFaceEffects) {
				effect.process(x, y, dEyes, c, mPaint);
			}
		}
		else {
			c.drawText("No faces found", 0, 0, mPaint);
		}

		return bitmap;
	}
	
	public Bitmap process(Bitmap bitmap, int buffer[], int angle) {
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();

		if (angle != 0) {
			Matrix m = new Matrix();
			m.postRotate(angle);
			bitmap = Bitmap.createBitmap(bitmap, 0, 0,
					width, height, m, false);			
		}
		else {
			bitmap = bitmap.copy(Config.RGB_565, true);
		}
		
		if (mArEffects) {
			bitmap = processArEffects(bitmap);
		}
		return bitmap;		
	}
}
