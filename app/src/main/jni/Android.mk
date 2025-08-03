# This is the build file for the native hangul_cutter library.

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

# Set the module name for the shared library
LOCAL_MODULE    := hangul_cutter

# List the source files to be compiled
LOCAL_SRC_FILES := hangul_cutter.c

# THIS IS THE FIX: Add the linker flag for 16KB page size alignment.
# This tells the linker to ensure the compiled library is compatible
# with modern Android memory pages.
LOCAL_LDFLAGS   += -Wl,-z,max-page-size=16384

# Build the shared library
include $(BUILD_SHARED_LIBRARY)
