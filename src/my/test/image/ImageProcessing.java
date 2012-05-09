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
import android.util.Log;

public class ImageProcessing {
	public static class Kernel2D {
		public final int matrix[];
		public final int magnitude;
		public final int height;
		public final int width;
		
		public Kernel2D(int matrix[], int width, int height) {
			if (matrix == null) {
				throw new NullPointerException("matrix must not be null");
			}
			
			if (width * height != matrix.length) {
				throw new IllegalArgumentException(
						"matrix size must equal the product of its dimensions");
			}
			
			int magnitude = 0;
			for (int d : matrix) {
				magnitude += d;
			}
			this.magnitude = magnitude;
			this.matrix = matrix;
			this.width = width;
			this.height = height;
		}
		
		public Kernel2D Blur(int radius) {	
			int matrix[] = new int[radius * radius];
			for (int i = 0; i < radius; i++) {
				int rowshift = i * radius;
				for (int j = 0; j < radius; j++) {
					int index = rowshift + j;
					matrix[index] = 1 << (Math.min(i, j));
				}
			}
			return new Kernel2D(matrix, radius, radius);
		}
		
		public static Kernel2D Identity() {
			return new Kernel2D(new int[] {
				0, 0, 0,
				0, 1, 0,
				0, 0, 0,
			}, 3, 3);
		}
		
		public static Kernel2D Sobel() {
			int matrix[] = new int[] {
				1, 2, 1,
				0, 0, 0,
				-1, -2, -1,
			};
			return new Kernel2D(matrix, 3, 3);
		}
		
		public static Kernel2D Test() {
			return new Kernel2D(new int[] {
				1, 1, 1,
				1, -9, 1,
				1, 1, 1,
			}, 3, 3);
		}
	}
	
	public static class SeparableKernel2D {
		
	}
	
	/**
	 * Convolves the image with the filter kernel
	 * @param kernel the convolution filter kernel as a 2d matrix
	 * @param rgb image data; rgb.length <= width * height
	 * @param width image width
	 * @param height image height
	 */
	
	public static void Convolve2D(Kernel2D kernel,
			int old[], int out[], int width, int height)
	{
		int dx = kernel.width >> 1;
		int dy = kernel.height >> 1;
				
		int mask_r = 0xff;
		int mask_g = 0xff;
		int mask_b = 0xff;
		
		int shift_r = 16;
		int shift_g = 8;
				
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				int red = 0;
				int green = 0;
				int blue = 0;
				
				for (int ki = 0; ki < kernel.height; ki++) {
					/*
					 * Flip the kernel as per definition of convolution
					 */
					int kern_row = kernel.height - ki - 1;
					int img_row = i + kern_row - dy;
					
					/*
					 * Wrap around edges. An alternative approach
					 * is to pad with zeroes, but if we wrap
					 * around, the transformation is reversible
					 * and black edges do not appear
					 */
					if (img_row < 0 || img_row >= height) {
						img_row += height;
						img_row %= height;
					}
					
					for (int kj = 0; kj < kernel.width; kj++) {
						int kern_column = kernel.width - kj - 1;
						int img_col = j + kern_column - dx;
						if (img_col < 0 || img_col >= width) {
							img_col += width;
							img_col %= width;
						}
						
						int kern_idx = kern_row * kernel.width + kern_column;
						
						int pixel = old[img_row * width + img_col];
						int _red = (pixel >> shift_r) & mask_r;
						int _green = (pixel >> shift_g) & mask_g;
						int _blue = pixel & mask_b;
						
						red += _red * kernel.matrix[kern_idx];
						green += _green * kernel.matrix[kern_idx];
						blue += _blue * kernel.matrix[kern_idx];
					}
				}
				
				if (kernel.magnitude != 0) {
					red = mask_r & (red / kernel.magnitude);
					green = mask_g & (green / kernel.magnitude);
					blue = mask_b & (blue / kernel.magnitude);
				}
								
				int result = (0xff << 24) | (red << shift_r)
						| (green << shift_g) | blue;
				out[width * i + j] = result;
			}
		}
	}
	
	public static void Convolve2DSeparable(SeparableKernel2D kernel,
			int rgb[], int width, int height)
	{
	}
	
	public static void preProcess(int rgb[], int out[], int width, int height) {
		//for (int i = 0; i < rgb.length; i++) {
		//	rgb[i] = 0xff000000 | ~(rgb[i] & 0xffffff);
		//}
		//Convolve2D(Kernel2D.Identity(), rgb, width, height);
		Convolve2D(Kernel2D.Sobel(), rgb, out, width, height);
	}
	public static Bitmap process(Bitmap bitmap, int angle) {
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
						
			c.drawRect(pf.x - (dEyes / 2), pf.y, dEyes, 10, paint);
		}
		else {
			c.drawText("No faces found", 0, 0, paint);
		}
		
		return bitmap;
	}
}
