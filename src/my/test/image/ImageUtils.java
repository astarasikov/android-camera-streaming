package my.test.image;

import android.util.Log;

public class ImageUtils {
	static {
		Log.i("NATIVE", "loading dsp-jni");
		System.loadLibrary("dsp-jni");
	}
	
	public static native void nYUV2RGB(
			int rgb[], byte yuv[], int width, int height);
	
	final static boolean useNative = true;
	
	/*
	 * This subroutine is based on the decodeYUV420SPQuarterSize
	 * from Android project Camera application
	 */
    public static void decodeYUV420SP(
    		int[] rgb, byte[] yuv420sp, int width, int height)
    {
    	if (useNative) {
    		nYUV2RGB(rgb, yuv420sp, width, height);
    		return;
    	}
    		
        final int frameSize = width * height;

        for (int j = 0, ypd = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, ypd++) {
                int y = (0xff & ((int) yuv420sp[j * width + i])) - 16;
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
