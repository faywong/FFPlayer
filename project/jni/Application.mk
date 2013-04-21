APP_PROJECT_PATH := $(call my-dir)/..

APP_STL := gnustl_static
APP_CFLAGS := -O3 -DNDEBUG -g # arm-linux-androideabi-4.4.3 crashes in -O0 mode on SDL sources
APP_PLATFORM := android-14 # Android 4.0, it should be backward compatible to previous versions
APP_PIE := false # This feature makes executables incompatible to Android API 15 or lower

# Available libraries: mad (GPL-ed!) sdl_mixer sdl_image sdl_ttf sdl_net sdl_blitpool sdl_gfx sdl_sound intl xml2 lua jpeg png ogg flac tremor vorbis freetype xerces curl theora fluidsynth lzma lzo2 mikmod openal timidity zzip bzip2 yaml-cpp python
APP_MODULES := application sdl-1.2 sdl_main stlport jpeg png ogg flac vorbis freetype avutil avcodec avformat avdevice swscale swresample avresample avfilter

# To filter out static libs from all libs in makefile
APP_AVAILABLE_STATIC_LIBS := jpeg png tremor freetype xerces ogg tremor vorbis flac boost_date_time boost_filesystem boost_iostreams boost_program_options boost_regex boost_signals boost_system boost_thread glu

APP_ABI := armeabi armeabi-v7a

# The namespace in Java file, with dots replaced with underscores
SDL_JAVA_PACKAGE_PATH := org_ffmpeg_ffplayer_render

# Path to files with application data - they should be downloaded from Internet on first app run inside
# Java sources, or unpacked from resources (TODO)
# Typically /sdcard/alienblaster
# Or /data/data/de.schwardtnet.alienblaster/files if you're planning to unpack data in application private folder
# Your application will just set current directory there
SDL_CURDIR_PATH := org.ffmpeg.ffplayer

# Android Dev Phone G1 has trackball instead of cursor keys, and
# sends trackball movement events as rapid KeyDown/KeyUp events,
# this will make Up/Down/Left/Right key up events with X frames delay,
# so if application expects you to press and hold button it will process the event correctly.
# TODO: create a libsdl config file for that option and for key mapping/on-screen keyboard
SDL_TRACKBALL_KEYUP_DELAY := 1

# If the application designed for higher screen resolution enable this to get the screen
# resized in HW-accelerated way, however it eats a tiny bit of CPU
SDL_VIDEO_RENDER_RESIZE := 1

COMPILED_LIBRARIES := avutil avcodec avformat avdevice swscale swresample avresample avfilter

APPLICATION_ADDITIONAL_CFLAGS := -O3 -Ijni/ffmpeg

APPLICATION_ADDITIONAL_LDFLAGS :=

APPLICATION_OVERLAPS_SYSTEM_HEADERS := n

APPLICATION_SUBDIRS_BUILD :=

APPLICATION_BUILD_EXCLUDE :=

APPLICATION_CUSTOM_BUILD_SCRIPT :=

SDL_ADDITIONAL_CFLAGS := -DSDL_ANDROID_KEYCODE_0=SPACE -DSDL_ANDROID_KEYCODE_1=RETURN -DSDL_ANDROID_KEYCODE_2=NO_REMAP -DSDL_ANDROID_KEYCODE_3=NO_REMAP -DSDL_ANDROID_KEYCODE_4=SPACE -DSDL_ANDROID_KEYCODE_5=ESCAPE  -DSDL_ANDROID_SCREENKB_KEYCODE_0=0 -DSDL_ANDROID_SCREENKB_KEYCODE_1=1 -DSDL_ANDROID_SCREENKB_KEYCODE_2=2 -DSDL_ANDROID_SCREENKB_KEYCODE_3=3 -DSDL_ANDROID_SCREENKB_KEYCODE_4=4 -DSDL_ANDROID_SCREENKB_KEYCODE_5=5 -DSDL_ANDROID_SCREENKB_KEYCODE_6=6 -DSDL_ANDROID_SCREENKB_KEYCODE_7=7 -DSDL_ANDROID_SCREENKB_KEYCODE_8=8 -DSDL_ANDROID_SCREENKB_KEYCODE_9=9

SDL_VERSION := 1.2

