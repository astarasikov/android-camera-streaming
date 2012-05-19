LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := dsp-jni
LOCAL_SRC_FILES := dsp-jni.c

include $(BUILD_SHARED_LIBRARY)
