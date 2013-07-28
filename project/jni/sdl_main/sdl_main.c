#include <unistd.h>
#include <stdlib.h>
#include <limits.h>
#include <jni.h>
#include <android/log.h>
#include <pthread.h>
#include "SDL_version.h"
#include "SDL_thread.h"
#include "SDL_main.h"
#include "SDL_android.h"

#include "JNICtx.h"
/* JNI-C wrapper stuff */

#ifdef __cplusplus
#define C_LINKAGE "C"
#else
#define C_LINKAGE
#endif


#ifndef SDL_JAVA_PACKAGE_PATH
#error You have to define SDL_JAVA_PACKAGE_PATH to your package path with dots replaced with underscores, for example "com_example_SanAngeles"
#endif
#define JAVA_EXPORT_NAME2(name,package) Java_##package##_##name
#define JAVA_EXPORT_NAME1(name,package) JAVA_EXPORT_NAME2(name,package)
#define JAVA_EXPORT_NAME(name) JAVA_EXPORT_NAME1(name,SDL_JAVA_PACKAGE_PATH)

struct ProcessContext {
	int argc;
	char **argv;
	JavaVM *jvm;
	JNIEnv* jni_env;
    jobject thiz;
} gProcessContext = {
		0,
		NULL,
		NULL,
		NULL,
		NULL
};

int JNI_OnLoad(JavaVM *jvm, void* reserved) {
    JNIEnv *env;
    __android_log_print(ANDROID_LOG_INFO, "libSDL", "JNI_OnLoad() in jvm:%p", jvm);
    gProcessContext.jvm = jvm;
    if ((*jvm)->GetEnv(jvm, (void**) &env, JNI_VERSION_1_2) != JNI_OK) {
        return JNI_ERR;
    }
    return JNI_VERSION_1_2;
}

JNIEnv* SDL_ANDROID_JniEnv()
{
	return gProcessContext.jni_env;
}
jobject SDL_ANDROID_JniVideoObject()
{
	return gProcessContext.thiz;
}

#if SDL_VERSION_ATLEAST(1,3,0)
#else
extern void SDL_ANDROID_MultiThreadedVideoLoopInit();
extern void SDL_ANDROID_MultiThreadedVideoLoop();

static int threadedMain(void * waitForDebugger);

int threadedMain(void * waitForDebugger)
{
	if (waitForDebugger)
	{
		//__android_log_print(ANDROID_LOG_INFO, "libSDL", "We are being debugged - waiting for debugger for 7 seconds");
		//usleep(7000000);
	}
	SDL_main(gProcessContext.argc, gProcessContext.argv);
	__android_log_print(ANDROID_LOG_INFO, "libSDL", "Application closed, calling exit(0)");
	exit(0);
}
#endif

extern C_LINKAGE void
Java_org_ffmpeg_ffplayer_render_DefaultRender_nativeInit(JNIEnv* env, jobject thiz, jstring jmediaurl, jstring cmdline, jint multiThreadedVideo, jint waitForDebugger)
{
    int i = 0;
    char *mediaurl = NULL;
    const jbyte *jstr;
    JNICtx *ctx = NULL;
    if (!InitJNICtx(gProcessContext.jvm, &ctx)) {
        LogJNIEnv(ctx, env);
    }

    gProcessContext.jni_env = env;
    gProcessContext.thiz = thiz;

	jstr = (*env)->GetStringUTFChars(env, jmediaurl, NULL);
	if (jstr != NULL && strlen(jstr) > 0) {
		mediaurl = strdup(jstr);
	}

	(*env)->ReleaseStringUTFChars(env, jmediaurl, jstr);

	__android_log_print(ANDROID_LOG_INFO, "libSDL", "mediaurl:\"%s\"", mediaurl);
        if (NULL == mediaurl) {
            __android_log_print(ANDROID_LOG_INFO, "libSDL", "mediaurl invalid, return!!!");
            return;
        }

	chdir(mediaurl);

	jstr = (*env)->GetStringUTFChars(env, cmdline, NULL);

	char * str = NULL;
	if (jstr != NULL && strlen(jstr) > 0) {
		str = jstr;
		__android_log_print(ANDROID_LOG_INFO, "libSDL", "got cmdline:\"%s\"", str);
	} else {
		return;
	}

	char * str1, * str2;
	str1 = strdup(str);
	str2 = str1;
	while(str2)
	{
		gProcessContext.argc++;
		str2 = strchr(str2, ' ');
		if(!str2)
			break;
		str2++;
	}

	gProcessContext.argv = (char **)malloc((gProcessContext.argc + 1) * sizeof(char *));
	str2 = str1;
	while(str2)
	{
		gProcessContext.argv[i] = str2;
		i++;
		str2 = strchr(str2, ' ');
		if (str2)
			*str2 = 0;
		else
			break;
		str2++;
	}
	char *it = mediaurl;
	__android_log_print(ANDROID_LOG_INFO, "libSDL", "Calling SDL_main(\"%s\") case 1", it);

	while (*it == ' ' && *it != 0) {
		it++;
	}
	gProcessContext.argv[i] = it;
	gProcessContext.argc++;

	__android_log_print(ANDROID_LOG_INFO, "libSDL", "Calling SDL_main(\"%s\"), case 2", it);

	__android_log_print(ANDROID_LOG_INFO, "libSDL", "Calling SDL_main(\"%s\")", str);

	(*env)->ReleaseStringUTFChars(env, cmdline, jstr);

	for( i = 0; i < gProcessContext.argc; i++ )
		__android_log_print(ANDROID_LOG_INFO, "libSDL", "param %d = \"%s\"", i, gProcessContext.argv[i]);

#if SDL_VERSION_ATLEAST(1,3,0)
	SDL_main(gProcessContext.argc, gProcessContext.argv);
#else
	if(!multiThreadedVideo)
	{
		if(waitForDebugger)
		{
			//__android_log_print(ANDROID_LOG_INFO, "libSDL", "We are being debugged - waiting for debugger for 7 seconds");
			//usleep(7000000);
		}
		SDL_main(gProcessContext.argc, gProcessContext.argv);
	}
	else
	{
		SDL_ANDROID_MultiThreadedVideoLoopInit();
		SDL_CreateThread(threadedMain, (void *)waitForDebugger);
		SDL_ANDROID_MultiThreadedVideoLoop();
	}
#endif
	free(mediaurl);
	free(str1);
};
