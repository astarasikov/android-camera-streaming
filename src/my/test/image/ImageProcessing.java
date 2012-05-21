package my.test.image;

import java.util.Arrays;

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

public class ImageProcessing {
	public static Bitmap process(Bitmap bitmap, int buffer[], int angle) {
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
		
		Canvas c = new Canvas(bitmap);
				
		Paint paint = new Paint();
		paint.setColor(Color.RED);
		paint.setTextSize(20);
		paint.setTypeface(Typeface.DEFAULT_BOLD);

		/* some debugging code to test scaling */
		c.drawLine(0, 0, width, height, paint);
		c.drawLine(height, 0, width, 0, paint);		
		/* end of scaling code */
		
		FaceDetector fd = new FaceDetector(width, height, 1);
		Face faces[] = new Face[1];
		int numFaces = fd.findFaces(bitmap, faces);
		if (numFaces > 0) {
			Face f = faces[0];
			PointF pf = new PointF();
			f.getMidPoint(pf);

			c.drawText("Found a face", 0, 0, paint);
			int dEyes = (int)f.eyesDistance();
						
			c.drawRect(pf.x - dEyes, pf.y - dEyes,
					pf.x + dEyes, pf.y + dEyes, paint);
		}
		else {
			c.drawText("No faces found", 0, 0, paint);
		}
		
		return bitmap;
	}
}
