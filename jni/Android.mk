LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE    := hangul_cutter
LOCAL_SRC_FILES := hangul_cutter.c
LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)
