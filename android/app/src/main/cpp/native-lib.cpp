#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include "pocket_sparrow.hpp"

#define TAG "PocketSparrowNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

using namespace pocket_sparrow;

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_pocketsparrow_core_NativeBridge_initEngine(
    JNIEnv* env,
    jobject /* this */,
    jstring model_path,
    jstring vocab_path
) {
    std::string m_path = "";
    if (model_path != nullptr) {
        const char* m_chars = env->GetStringUTFChars(model_path, nullptr);
        m_path = m_chars;
        env->ReleaseStringUTFChars(model_path, m_chars);
    }

    std::string v_path = "";
    if (vocab_path != nullptr) {
        const char* v_chars = env->GetStringUTFChars(vocab_path, nullptr);
        v_path = v_chars;
        env->ReleaseStringUTFChars(vocab_path, v_chars);
    }

    LOGI("Initializing Pocket Sparrow C++ Engine with 0 cloud telemetry...");
    auto* engine = new DetectionEngine(m_path, v_path);
    return reinterpret_cast<jlong>(engine);
}

JNIEXPORT void JNICALL
Java_com_pocketsparrow_core_NativeBridge_freeEngine(
    JNIEnv* /* env */,
    jobject /* this */,
    jlong handle
) {
    if (handle != 0) {
        auto* engine = reinterpret_cast<DetectionEngine*>(handle);
        delete engine;
    }
}

JNIEXPORT jobject JNICALL
Java_com_pocketsparrow_core_NativeBridge_scanContent(
    JNIEnv* env,
    jobject /* this */,
    jlong handle,
    jint content_type,
    jstring payload
) {
    if (handle == 0 || payload == nullptr) {
        return nullptr;
    }

    auto* engine = reinterpret_cast<DetectionEngine*>(handle);
    const char* p_chars = env->GetStringUTFChars(payload, nullptr);
    std::string payload_str(p_chars);
    env->ReleaseStringUTFChars(payload, p_chars);

    ContentType c_type = static_cast<ContentType>(content_type);
    ScanResult result = engine->scan(c_type, payload_str);

    jclass result_class = env->FindClass("com/pocketsparrow/core/ScanResult");
    if (result_class == nullptr) {
        LOGE("Failed to find ScanResult class in Kotlin classpath");
        return nullptr;
    }

    jmethodID constructor = env->GetMethodID(
        result_class,
        "<init>",
        "(IIFJLjava/lang/String;Ljava/lang/String;Z)V"
    );

    jstring j_category = env->NewStringUTF(result.category.c_str());
    jstring j_xai = env->NewStringUTF(result.xai_reason.c_str());

    jobject result_obj = env->NewObject(
        result_class,
        constructor,
        static_cast<jint>(result.verdict),
        static_cast<jint>(result.tier_triggered),
        result.confidence,
        static_cast<jlong>(result.latency_us),
        j_category,
        j_xai,
        result.should_block
    );

    return result_obj;
}

JNIEXPORT jobject JNICALL
Java_com_pocketsparrow_core_NativeBridge_auditPermissions(
    JNIEnv* env,
    jobject /* this */,
    jlong handle,
    jobjectArray permissions
) {
    if (handle == 0 || permissions == nullptr) {
        return nullptr;
    }

    auto* engine = reinterpret_cast<DetectionEngine*>(handle);
    jsize len = env->GetArrayLength(permissions);
    std::vector<std::string> perm_vec;
    perm_vec.reserve(len);

    for (jsize i = 0; i < len; ++i) {
        jstring p_str = static_cast<jstring>(env->GetObjectArrayElement(permissions, i));
        if (p_str != nullptr) {
            const char* chars = env->GetStringUTFChars(p_str, nullptr);
            perm_vec.emplace_back(chars);
            env->ReleaseStringUTFChars(p_str, chars);
            env->DeleteLocalRef(p_str);
        }
    }

    PermissionAuditResult audit = engine->audit_apk_permissions(perm_vec);

    jclass result_class = env->FindClass("com/pocketsparrow/core/ScanResult");
    jmethodID constructor = env->GetMethodID(
        result_class,
        "<init>",
        "(IIFJLjava/lang/String;Ljava/lang/String;Z)V"
    );

    jstring j_category = env->NewStringUTF(audit.category.c_str());
    jstring j_xai = env->NewStringUTF(audit.explanation.c_str());

    return env->NewObject(
        result_class,
        constructor,
        static_cast<jint>(audit.verdict),
        1, // Tier 1 static
        static_cast<jfloat>(audit.risk_score) / 100.0f,
        static_cast<jlong>(120), // <1ms
        j_category,
        j_xai,
        audit.should_block
    );
}

} // extern "C"
