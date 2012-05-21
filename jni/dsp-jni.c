#include <string.h>
#include <jni.h>

#define RGB_SLICE_SIZE 256
#define YUV_SLICE_SIZE 128

char getByte(JNIEnv *env, jbyteArray *jary, char *buffer, int idx) {
	int bufPtr = idx % YUV_SLICE_SIZE;
	if (bufPtr == 0) {
		(*env)->GetByteArrayRegion(env, *jary, idx, YUV_SLICE_SIZE, buffer);	
	}
	return buffer[bufPtr];
}

void Java_my_test_image_ImageUtils_nYUV2RGB
(
	JNIEnv *env,
	jobject thiz,
	jintArray j_rgb,
	jbyteArray j_yuv,
	jint width,
	jint height
)
{
	int frameSize = width * height;
	
	int i, j, ypd;
	int rgbSlice[RGB_SLICE_SIZE];
	char ySlice[YUV_SLICE_SIZE];
	char uvSlice[YUV_SLICE_SIZE];

	for (j = 0, ypd = 0; j < height; j++) {
		int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
		for (i = 0; i < width; i++, ypd++) {

			int y_ptr = j * width + i;
			int yraw = getByte(env, &j_yuv, ySlice, y_ptr);
			
			int y = (0xff & (yraw)) - 16;
			if (y < 0) {
				y = 0;
			}
			if ((i & 1) == 0) {
				v = (0xff & getByte(env, &j_yuv, uvSlice, uvp)) - 128;
				u = (0xff & getByte(env, &j_yuv, uvSlice, uvp + 1)) - 128;
				uvp += 2;
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
	
			int rgb = 0xff000000 |
					((r << 6) & 0xff0000) |
					((g >> 2) & 0xff00) |
					((b >> 10) & 0xff);

			rgbSlice[ypd % RGB_SLICE_SIZE] = rgb;

			int y_ptr_rem = ypd % RGB_SLICE_SIZE;
			if (RGB_SLICE_SIZE - 1 == y_ptr_rem) {
				(*env)->SetIntArrayRegion(env, j_rgb,
					ypd - y_ptr_rem,
					RGB_SLICE_SIZE, rgbSlice);
			}
		}
	}
}
