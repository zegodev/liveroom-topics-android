//
// Created by winnie on 2019/1/29.
//

#include <jni.h>
#include <string>

//
extern "C" JNIEXPORT void
Java_com_zego_videoexternalrender_videorender_Renderer_copyPlane(JNIEnv* env,
         jclass j_class,
         jobject j_src_buffer,
          jint src_stride,
          jobject j_dst_buffer,
          jint dst_stride,
          jint width,
          jint height) {

            uint8_t* src =  reinterpret_cast<uint8_t*>(env->GetDirectBufferAddress(j_src_buffer));
            uint8_t* dst =  reinterpret_cast<uint8_t*>(env->GetDirectBufferAddress(j_dst_buffer));
            if (src_stride == dst_stride) {
              memcpy(dst, src, src_stride * height);
            } else {
                for (int i = 0; i < height; i++) {
                    memcpy(dst, src, width);
                    src += src_stride;
                    dst += dst_stride;
                }
            }

}

