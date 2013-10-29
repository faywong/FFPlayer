APP_PROJECT_PATH := $(call my-dir)/..

#APP_CFLAGS := -O3 -DNDEBUG -g # arm-linux-androideabi-4.4.3 crashes in -O0 mode on SDL sources

APP_PLATFORM := android-14 # Android 4.0, it should be backward compatible to previous versions
APP_PIE := false # This feature makes executables incompatible to Android API 15 or lower

APP_MODULES := libSDL2 libavcodec libavdevice libavfilter libavformat libavresample libavutil libswresample libswscale

APP_ABI := armeabi-v7a

APP_OPTIM := release
