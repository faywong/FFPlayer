This a SDL & FFmpeg based MultimediaPlayer for Android platform

Installation
============

This project should be compiled with Android 4.2 SDK (API level 17) and NDK r8, r7c, r6 or r5c,
google for them and install them as described in their docs.
You'll need to install Java Ant too.
The application will run on Android OS 1.6 and above, but will use features from Android 4.2 if available.
Also it's compatible with NDK r4b and all versions of CrystaX NDK starting from r4b.
CrystaX NDK adds support for wide chars, and required if you want to use Boost libraries.
http://www.crystax.net/android/ndk.php
The most supported environment for this port is Linux, MacOs should be okay too.
If you're developing under Windows you'd better install Portable Ubuntu, to get proper Linux environment
running inside Windows, then install Linux toolchain on it.
https://sourceforge.net/projects/portableubuntu/
Cygwin is not supported by the NDK, starting from the NDK r6.


How to compile the application
===============================

build all from scratch

'''bash
. build/envsetup.sh
build
'''

if you want to harness the prebuilt libraries, you can import the "project" directory as a android project in Eclipse

Remind to add NDK dir to your PATH, then launch it.
It will compile a bunch of libs under project/libs/armeabi,
create Android package file project/bin/MainActivity-debug.apk,
and install it to your device or emulator, if you specify option -i or -r to build.sh.
Then you can test it by launching Ballfield icon from Android applications menu.

There are other applications inside project/jni/application directory,
some of them are referenced using Git submodule mechanism, you may download them using command
git submodule update --init
Some of them may be outdated and won't compile, some contain only patch file and no sources,
so you should check out Git logs before compiling a particular app, and checkout whole repo at that date:
gitk project/jni/application/<directory>

License information
===================

The SDL 1.2 port is licensed under LGPL, so you may use it for commercial purposes
without releasing source code, however to fullfill LGPL requirements you'll have to publish
the file AndroidAppSettings.cfg to allow linking other version of libsdl-1.2.so with the libraries
in the binary package you're distributing - typically libapplication.so and other
closed-source libraries in your .apk file.

The SDL 1.3 port and Java source files are licensed under zlib license, which means
you may modify them as you like without releasing source code.

The libraries under project/jni have their own license, I've tried to compile all LGPL-ed libs
as shared libs but you should anyway inspect the licenses of the libraries you're linking to.
libmad and liblzo2 are licensed under GPL, so if you're planning to make commercial app you should avoid
using them, otherwise you'll have to release your whole application sources under GPL too.

The "Ultimate Droid" on-screen keyboard theme by Sean Stieber is licensed under Creative Commons - Attribution license.
The "Simple Theme" on-screen keyboard theme by Dmitry Matveev is licensed under zlib license.
The "Sun" on-screen keyboard theme by Sirea (Martina Smejkalova) is licensed under Creative Commons - Attribution license.

Distribute
==========
you can download the [apk](https://github.com/faywong/FFPlayer/blob/trunk/FFPlayer.apk)

or 
<a
href="https://play.google.com/store/apps/details?id=io.github.faywong.ffplayer">
  <img alt="Get it on Google Play"
       src="https://developer.android.com/images/brand/en_generic_rgb_wo_45.png"
/>
</a>
