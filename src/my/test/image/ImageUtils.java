package my.test.image;

public class ImageUtils {
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
		
		/**
		 * Convolves the image with the filter kernel
		 * @param kernel the convolution filter kernel as a 2d matrix
		 * @param rgb image data; rgb.length <= width * height
		 * @param width image width
		 * @param height image height
		 */
		
		public static void Convolve2D(ImageUtils.Kernel2D kernel,
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

		public static Kernel2D Identity() {
			return new Kernel2D(new int[] {
				0, 0, 0,
				0, 1, 0,
				0, 0, 0,
			}, 3, 3);
		}
		
		public static Kernel2D BoxBlur() {
			return new Kernel2D(new int[] {
				1, 1, 1,
				1, 1, 1,
				1, 1, 1,
			}, 3, 3);
		}
		
		public static Kernel2D GaussianBlur() {
			return new Kernel2D(new int[] {
				1, 1, 1,
				1, 8, 1,
				1, 1, 1,
			}, 3, 3);
		}
		
		public static Kernel2D EdgeDetection() {
			int matrix[] = new int[] {
				1, 2, 1,
				0, 0, 0,
				-1, -2, -1,
			};
			return new Kernel2D(matrix, 3, 3);
		}
		
		public static Kernel2D Emboss() {
			return new Kernel2D(new int[] {
				-1, -1, 0,
				-1, 0, 1,
				0, 1, 1,
			}, 3, 3);
		}
		
		public static Kernel2D Sharpen() {
			return new Kernel2D(new int[] {
				-1, -1, -1,
				-1, 12, -1,
				-1, -1, -1,
			}, 3, 3);
		}
	}

	static boolean mUseNative = true;
	
	public static void setUseNative(boolean useNative) {
		mUseNative = useNative;
	}
	
    public static void decodeYUV420SP(
    		int[] rgb, byte[] yuv420sp, int width, int height)
    {
    	if (mUseNative) {
    		nYUV2RGB(rgb, yuv420sp, width, height);
    	}
    	else {
    		jYUV420SP(rgb, yuv420sp, width, height);
    	}
    }
    
	public static native void nYUV2RGB(
			int rgb[], byte yuv[], int width, int height);

	/*
	 * This subroutine is based on the decodeYUV420SPQuarterSize
	 * from Android project Camera application
	 */	
    protected static void jYUV420SP(
    		int[] rgb, byte[] yuv420sp, int width, int height) {    		
        final int frameSize = width * height;

        for (int j = 0, ypd = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, ypd++) {
                int y = (0xff & yuv420sp[j * width + i]) - 16;
                if (y < 0) {
                    y = 0;
                }
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }
                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0) {
                    r = 0;
                } else if (r > 262143) {
                    r = 262143;
                }
                if (g < 0) {
                    g = 0;
                } else if (g > 262143) {
                    g = 262143;
                }
                if (b < 0) {
                    b = 0;
                } else if (b > 262143) {
                    b = 262143;
                }

                rgb[ypd] = 0xff000000 |
                		((r << 6) & 0xff0000) |
                		((g >> 2) & 0xff00) |
                        ((b >> 10) & 0xff);
            }
        }
    }
}
