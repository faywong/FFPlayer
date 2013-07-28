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

/****************************************************************************************
 ** File Name : hdmicec_def.h
 **
 ** Description : Function prototypes , common structures, macros for hdmicec jni implement
 **
 ** Author : Faywong <faywong@marvell.com>
 **
 ****************************************************************************************/


#ifndef JNICTX_H
#define JNICTX_H

#include "jni.h"

typedef enum JNICtxResult {
    UNKNOW_ERROR = -3,
    BAD_PARAMETER = -2,
    MALLOC_FAILED = -1,
    OK = 0,
    SUCCESS = OK,
} JNICtxResult;

typedef struct JNICtx {
    JavaVM *jvm;
    int vmversion;
    pthread_key_t tls_key;
    void (*thread_dtr)(void *context);
    struct JNICtx *self;
} JNICtx;

typedef struct TLStore {
    JNIEnv* jni_env;
    JNICtx* context;
} TLStore;

int InitJNICtx(JavaVM* vm, JNICtx **jni_ctx);
JNIEnv* GetJNIEnv(JNICtx* context);
int LogJNIEnv(JNICtx* context, JNIEnv* env);

#endif  // JNICTX_H
