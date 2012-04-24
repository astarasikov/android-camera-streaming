package my.test;

import android.graphics.Bitmap;

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
	}
	
	public static class SeparableKernel2D {
		
	}
	
	public static void Convolve2D(Kernel2D kernel, 
			int rgb[], int width, int height)
	{
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				int tempRed = 0;
				int tempGreen = 0;
				int tempBlue = 0;
				
				for (int ki = 0; ki < kernel.width; ki++) {
					for (int kj = 0; kj < kernel.height; kj++) {
						
					}
				}
				
				
			}
		}
	}
	
	public static void Convolve2DSeparable(SeparableKernel2D kernel,
			int rgb[], int width, int height)
	{
	}
	
	
	public static void preProcess(int rgb[], int width, int height) {
		//for (int i = 0; i < rgb.length; i++) {
		//	rgb[i] = 0xff000000 | ~(rgb[i] & 0xffffff);
		//}
	}
	public static void process(Bitmap bitmap) {
		bitmap.setPixel(30, 30, 0xffff0000);
	}
}
