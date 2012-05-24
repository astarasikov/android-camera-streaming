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
			int shiftX = (int)(1.25 * dEyes);
			int shiftY = (int)(dEyes);
			
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
			int newSize = (int)(2 * dEyes);
			int shiftX = (int)(-1 * dEyes);
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

	PreferenceHelper mPreferenceHelper;
	Context mContext;
	Paint mPaint;
	int mLastWidth;
	int mLastHeight;
	int mMaxFaces;
	
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
		if (mPreferenceHelper.booleanPreference(R.string.key_ar_hat,
				false))
		{
			effects.add(new HatEffect(mContext));
		}
		if (mPreferenceHelper.booleanPreference(R.string.key_ar_beard,
				false))
		{
			effects.add(new BeardEffect(mContext));
		}
		if (mPreferenceHelper.booleanPreference(R.string.key_ar_moustache,
				false))
		{
			effects.add(new MoustacheEffect(mContext));
		}
	
		mFaceEffects = effects;
	}
	
	public ImageProcessor(Context context, PreferenceHelper preferenceHelper) {
		mContext = context;
		mPreferenceHelper = preferenceHelper;
		
		mArEffects = mPreferenceHelper.booleanPreference
				(R.string.key_ar_effects, false);
		mMaxFaces = mPreferenceHelper.intPreference
				(R.string.key_ar_max_faces, 1);
		
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
		
		mFaceDetector = new FaceDetector(mLastWidth, mLastHeight, mMaxFaces);
		mFaces = new Face[mMaxFaces];
	}
	
	Bitmap processArEffects(Bitmap bitmap) {
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		reallocFaceDetector(width, height);
		
		Canvas c = new Canvas(bitmap);
		
		int numFaces = mFaceDetector.findFaces(bitmap, mFaces);
		for (int i = 0; i < numFaces; i++) {
			Face f = mFaces[i];
			PointF pf = new PointF();
			f.getMidPoint(pf);

			int dEyes = (int)f.eyesDistance();
			int x = (int)pf.x;
			int y = (int)pf.y;

			for (FaceOverlayEffect effect : mFaceEffects) {
				effect.process(x, y, dEyes, c, mPaint);
			}
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
