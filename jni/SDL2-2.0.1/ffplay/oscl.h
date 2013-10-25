#ifndef __OSCL_H__
#define __OSCL_H__
#ifdef __cplusplus
extern "C" {
#endif
typedef enum os_platform_ {
    OS_UNSUPPORTED = -1,
    ANDROID_OS
} os_platform;

typedef union oscl_meta_data_ {
    char c_m;
    int i_m;
    float f_m;
    double d_m;
    void* p_m;
    char* str_m;
} oscl_meta_data;

typedef struct oscl_player_adapter_ {
    const char* (*getName)();
    float (*getVersion)();
    int (*setUp)();
    int (*tearDown)();
    int (*getCurrentPosition)();
    int (*getDuration)();
    oscl_meta_data (*getMetaData)(const char* key, void* extra);
} oscl_player_adapter;

extern oscl_player_adapter* get_player_adapter_impl(os_platform os);
#ifdef __cplusplus
}
#endif
#endif
