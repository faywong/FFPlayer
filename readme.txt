This a SDL & FFmpeg based MultimediaPlayer for Android platform

Installation
============

This project should be compiled with Android 4.2 SDK (API level 17) and NDK r9, r8, r7c, r6.
google for them and install them as described in their docs. Install JDK & add a system environment
"JAVA_HOME" to point to its installation directory. You'll need to install Java Ant too.
The application will run on Android OS 1.6 and above, but will use features from Android 4.2 if available.
The native code(in c/c++) it's only compatible with armv7-a arch.
The most supported environment for this port is Linux, MacOs, Windows(Cygwin) 

How to compile
===============================

Remind to add NDK, SDK dir(in which the binary tools are located) & ANT dir to your PATH, then launch it.
# command line interface
cd {project_root}
ndk-build V=1
android update project -p .
ant debug

It will compile a bunch of libs under libs/armeabi-v7a,
create an Android package file as bin/FFPlayer-debug.apk,

License information
===================

The SDL 2.0.1 is licensed under zlib, so you can use it for commercial purposes
without releasing source code.

Changelog
=========

Author
======
Faywong <philip584521@gmail.com>
