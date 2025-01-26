#include <jni.h>
#include <string>

extern "C" {

// Karıştırılmış parçalar
const char* p1 = "6019";
const char* p2 = "Er.0";
const char* p3 = "Alp.";
const char* p4 = "07/0";

JNIEXPORT jstring JNICALL
Java_com_alperyuceer_komik_1replikler_AudioEncryption_getKeyFromNative(JNIEnv* env, jobject) {
    // Parçaları karıştırarak birleştir
    std::string key = std::string(p3) + std::string(p2) + std::string(p1) + std::string(p4);
    return env->NewStringUTF(key.c_str());
}

} 