package my.test.image;

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
	class OverlayEffect {
		
	}
	
	
	PreferenceHelper mPreferenceHelper;
	Context mContext;
	Paint mPaint;
	int mLastWidth;
	int mLastHeight;
	
	FaceDetector mFaceDetector;
	Face mFaces[];
	
	Bitmap coolFace;
	
	public ImageProcessor(Context context, PreferenceHelper preferenceHelper) {
		mContext = context;
		mPreferenceHelper = preferenceHelper;
		
		mPaint = new Paint();
		mPaint.setColor(Color.CYAN);
		mPaint.setTextSize(20);
		mPaint.setTypeface(Typeface.DEFAULT_BOLD);
		
		coolFace = BitmapFactory.decodeResource(mContext.getResources(),
				R.drawable.coolface);
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
		
		width = bitmap.getWidth();
		height = bitmap.getHeight();
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
			
			mPaint.setStyle(Paint.Style.STROKE);
			mPaint.setStrokeWidth(3);
			
			int x0 = (int)(pf.x - dEyes);
			int y0 = (int)(pf.y - dEyes);
			int x1 = (int)(pf.x + dEyes);
			int y1 = (int)(pf.y + dEyes);
			
			boolean doProcessing = 
					mPreferenceHelper.booleanPreference(
							R.string.key_pref_ar_effects,
							false
							);
			
			if (doProcessing) {
				Bitmap cool = Bitmap.createScaledBitmap(
						coolFace,
						2 * dEyes,
						2 * dEyes,
						false
						);
				
				c.drawBitmap(cool, x0, y0, mPaint);		
			}
			c.drawRect(x0, y0, x1, y1, mPaint);
		}
		else {
			c.drawText("No faces found", 0, 0, mPaint);
		}
		
		return bitmap;
	}
}
