/*
**                Copyright 2012, MARVELL SEMICONDUCTOR, LTD.
** THIS CODE CONTAINS CONFIDENTIAL INFORMATION OF MARVELL.
** NO RIGHTS ARE GRANTED HEREIN UNDER ANY PATENT, MASK WORK RIGHT OR COPYRIGHT
** OF MARVELL OR ANY THIRD PARTY. MARVELL RESERVES THE RIGHT AT ITS SOLE
** DISCRETION TO REQUEST THAT THIS CODE BE IMMEDIATELY RETURNED TO MARVELL.
** THIS CODE IS PROVIDED "AS IS". MARVELL MAKES NO WARRANTIES, EXPRESSED,
** IMPLIED OR OTHERWISE, REGARDING ITS ACCURACY, COMPLETENESS OR PERFORMANCE.
**
** MARVELL COMPRISES MARVELL TECHNOLOGY GROUP LTD. (MTGL) AND ITS SUBSIDIARIES,
** MARVELL INTERNATIONAL LTD. (MIL), MARVELL TECHNOLOGY, INC. (MTI), MARVELL
** SEMICONDUCTOR, INC. (MSI), MARVELL ASIA PTE LTD. (MAPL), MARVELL JAPAN K.K.
** (MJKK), MARVELL ISRAEL LTD. (MSIL).
*/

#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>

#define LOG_NDEBUG 0
#undef LOG_TAG
#define LOG_TAG "JNICtx"
#include <android/log.h>
#include "jni.h"

#define ALOGD(...) __android_log_print(ANDROID_LOG_INFO, "JNICtx", __VA_ARGS__)
#define ENTER() __android_log_print(ANDROID_LOG_INFO, "JNICtx", "@%d %s() in", __LINE__, __FUNCTION__)
#define LEAVE() __android_log_print(ANDROID_LOG_INFO, "JNICtx", "@%d %s() leave", __LINE__, __FUNCTION__)

#include "JNICtx.h"
#define JNICTX_VALID_CHECK(ctx) \
    do { \
        if (NULL == ctx || ctx->self != ctx) { \
            ALOGD("bad input parameter!, @line:%d, %s() return!", __LINE__, __FUNCTION__); \
            return BAD_PARAMETER; \
        } \
    } while (0)

static void ThreadDestructor(void *context)
{
    ALOGD("thread_dtr() in context:0x%x", context);
    TLStore* tlstore = (TLStore*)context;
    if (NULL != tlstore && NULL != tlstore->context) {
        pthread_setspecific(tlstore->context->tls_key, NULL);
        JavaVM * jvm = tlstore->context->jvm;
        ALOGD("JVM:%p", jvm);
        if ((NULL != jvm) && (*jvm)->DetachCurrentThread(jvm) != JNI_OK) {
            ALOGD("FATAL ERROR! DetachCurrentThread() failed!");
        }
        free(tlstore->context);
        free(tlstore);
    }
}

int InitJNICtx(JavaVM* vm, JNICtx **jni_ctx)
{
    ENTER();
    JNICtx * ctx = (JNICtx*) malloc(sizeof(JNICtx));
    if (NULL == jni_ctx) {
        ALOGD("Bad input parameter!");
        LEAVE();
        return BAD_PARAMETER;
    }

    if (NULL == ctx) {
        ALOGD("malloc failed for JNICtx object");
        LEAVE();
        return MALLOC_FAILED;
    }

    ctx->jvm = vm;
    ctx->thread_dtr = ThreadDestructor;
    pthread_key_create(&ctx->tls_key, ctx->thread_dtr);
    ctx->self = ctx;
    *jni_ctx = ctx;
    LEAVE();
    return OK;
}

int LogJNIEnv(JNICtx *context, JNIEnv* env)
{
    ENTER();
    JNICTX_VALID_CHECK(context);
    if ((pthread_getspecific(context->tls_key)) == NULL) {
         ALOGD("pthread_getspecific() NULL case");
         TLStore* tlstore = (TLStore*)malloc(sizeof(TLStore));
         memset(tlstore, 0, sizeof(TLStore));
         if (NULL != tlstore) {
             tlstore->jni_env = env;
             tlstore->context = context;
             pthread_setspecific(context->tls_key, tlstore);
         }
    }
    LEAVE();
    return 0;
}

JNIEnv* GetJNIEnv(JNICtx *context) {
    ENTER();
    JNICTX_VALID_CHECK(context);

    JNIEnv* env;
    TLStore* tlstore = NULL;
    void* value = pthread_getspecific(context->tls_key);
    tlstore = (TLStore*)value;

    if (NULL == context->jvm) {
        ALOGD("context.jvm NULL");
        return NULL;
    }
    if (NULL == tlstore || (NULL == tlstore->jni_env)) {
        //case:native thread, so attach it to retrieve the JNIEnv object
        ALOGD("native thread so attach it to retrieve the JNIEnv object");
        JavaVMAttachArgs thread_args;
        thread_args.name = "HDMI_JNI";
        thread_args.version = context->vmversion;
        thread_args.group = NULL;
        (*context->jvm)->AttachCurrentThread(context->jvm, &env, &thread_args);
        LogJNIEnv(context, env);
    } else {
        ALOGD("already have a JNIEnv, use it directly");
        env = tlstore->jni_env;
    }
    ALOGD("native thread GetJNIEnv()=%08x\n", (unsigned int)env);
    LEAVE();
    return env;
}
