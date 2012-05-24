package my.test.image;

import java.util.ArrayList;
import java.util.List;

import my.test.R;
import my.test.utils.PreferenceHelper;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;

public class ImageProcessor {
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
