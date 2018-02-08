LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_C_INCLUDES := src

LOCAL_SRC_FILES := \
	jni/org_pocketworkstation_pckeyboard_BinaryDictionary.cpp \
	src/dictionary.cpp \
	src/char_utils.cpp

LOCAL_MODULE := libjni_pckeyboard

LOCAL_MODULE_TAGS := user

include $(BUILD_SHARED_LIBRARY)
