LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := librilspy
LOCAL_SRC_FILES := rilspy.c.arm
LOCAL_C_INCLUDES := ../../adbi/instruments/base/
LOCAL_LDLIBS    := -L ../../../../build/native/obj/local/armeabi -lbase -llog
LOCAL_CFLAGS    := -g -std=c99 -Wall -Wextra

include $(BUILD_SHARED_LIBRARY)
