/*
 *  Copyright 2015 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package com.zego.videoexternalrender.ve_gl;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.util.Log;
import android.view.Surface;

import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGL10;

/**
 * Holds EGL state and utility methods for handling an egl 1.0 EGLContext, an EGLDisplay,
 * and an EGLSurface.
 */
public abstract class EglBase {
    // EGL wrapper for an actual EGLContext.
    public static class Context {}

    // According to the documentation, EGL can be used from multiple threads at the same time if each
    // thread has its own EGLContext, but in practice it deadlocks on some devices when doing this.
    // Therefore, synchronize on this global lock before calling dangerous EGL functions that might
    // deadlock. See https://bugs.chromium.org/p/webrtc/issues/detail?id=5702 for more info.
    public static final Object lock = new Object();

    // These constants are taken from EGL14.EGL_OPENGL_ES2_BIT and EGL14.EGL_CONTEXT_CLIENT_VERSION.
    // https://android.googlesource.com/platform/frameworks/base/+/master/opengl/java/android/opengl/EGL14.java
    // This is similar to how GlSurfaceView does:
    // http://grepcode.com/file/repository.grepcode.com/java/ext/com.google.android/android/5.1.1_r1/android/opengl/GLSurfaceView.java#760
    private static final int EGL_OPENGL_ES2_BIT = 4;
    // Android-specific extension.
    private static final int EGL_RECORDABLE_ANDROID = 0x3142;

    // clang-format off
    public static final int[] CONFIG_PLAIN = {
            EGL10.EGL_RED_SIZE, 8,
            EGL10.EGL_GREEN_SIZE, 8,
            EGL10.EGL_BLUE_SIZE, 8,
            EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
            EGL10.EGL_NONE
    };
    public static final int[] CONFIG_RGBA = {
            EGL10.EGL_RED_SIZE, 8,
            EGL10.EGL_GREEN_SIZE, 8,
            EGL10.EGL_BLUE_SIZE, 8,
            EGL10.EGL_ALPHA_SIZE, 8,
            EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
            EGL10.EGL_NONE
    };
    public static final int[] CONFIG_PIXEL_BUFFER = {
            EGL10.EGL_RED_SIZE, 8,
            EGL10.EGL_GREEN_SIZE, 8,
            EGL10.EGL_BLUE_SIZE, 8,
            EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
            EGL10.EGL_SURFACE_TYPE, EGL10.EGL_PBUFFER_BIT,
            EGL10.EGL_NONE
    };
    public static final int[] CONFIG_PIXEL_RGBA_BUFFER = {
            EGL10.EGL_RED_SIZE, 8,
            EGL10.EGL_GREEN_SIZE, 8,
            EGL10.EGL_BLUE_SIZE, 8,
            EGL10.EGL_ALPHA_SIZE, 8,
            EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
            EGL10.EGL_SURFACE_TYPE, EGL10.EGL_PBUFFER_BIT,
            EGL10.EGL_NONE
    };
    public static final int[] CONFIG_RECORDABLE = {
            EGL10.EGL_RED_SIZE, 8,
            EGL10.EGL_GREEN_SIZE, 8,
            EGL10.EGL_BLUE_SIZE, 8,
            EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
            EGL_RECORDABLE_ANDROID, 1,
            EGL10.EGL_NONE
    };
    // clang-format on

    // Create a new context with the specified config attributes, sharing data with sharedContext.
    // |sharedContext| can be null.
    public static EglBase create(Context sharedContext, int[] configAttributes) {
        return (EglBase14.isEGL14Supported()
                && (sharedContext == null || sharedContext instanceof EglBase14.Context))
                ? new EglBase14((EglBase14.Context) sharedContext, configAttributes)
                : new EglBase10((EglBase10.Context) sharedContext, configAttributes);
    }

    public static EglBase create() {
        return create(null, CONFIG_PLAIN);
    }

    public static EglBase create(Context sharedContext) {
        return create(sharedContext, CONFIG_PLAIN);
    }

    public abstract void createSurface(Surface surface);

    // Create EGLSurface from the Android SurfaceTexture.
    public abstract void createSurface(SurfaceTexture surfaceTexture);

    // Create dummy 1x1 pixel buffer surface so the context can be made current.
    public abstract void createDummyPbufferSurface();

    public abstract void createPbufferSurface(int width, int height);

    public abstract Context getEglBaseContext();

    public abstract boolean hasSurface();

    public abstract int surfaceWidth();

    public abstract int surfaceHeight();

    public abstract void releaseSurface();

    public abstract void release();

    public abstract void makeCurrent();

    // Detach the current EGL context, so that it can be made current on another thread.
    public abstract void detachCurrent();

    public abstract void swapBuffers();

//    // Helper class for handling OpenGL shaders and shader programs.
//    public static class GlShader {
//        private static final String TAG = "GlShader";
//
//        private static int compileShader(int shaderType, String source) {
//            final int shader = GLES20.glCreateShader(shaderType);
//            if (shader == 0) {
//                throw new RuntimeException("glCreateShader() failed. GLES20 error: " + GLES20.glGetError());
//            }
//            GLES20.glShaderSource(shader, source);
//            GLES20.glCompileShader(shader);
//            int[] compileStatus = new int[] {
//                    GLES20.GL_FALSE
//            };
//            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
//            if (compileStatus[0] != GLES20.GL_TRUE) {
//                Log.e(TAG, "Could not compile shader " + shaderType + ":" +
//                        GLES20.glGetShaderInfoLog(shader));
//                throw new RuntimeException(GLES20.glGetShaderInfoLog(shader));
//            }
//            GlUtil.checkNoGLES2Error("compileShader");
//            return shader;
//        }
//
//        private int program;
//
//        public GlShader(String vertexSource, String fragmentSource) {
//            final int vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource);
//            final int fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
//            program = GLES20.glCreateProgram();
//            if (program == 0) {
//                throw new RuntimeException("glCreateProgram() failed. GLES20 error: " + GLES20.glGetError());
//            }
//            GLES20.glAttachShader(program, vertexShader);
//            GLES20.glAttachShader(program, fragmentShader);
//            GLES20.glLinkProgram(program);
//            int[] linkStatus = new int[] {
//                    GLES20.GL_FALSE
//            };
//            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
//            if (linkStatus[0] != GLES20.GL_TRUE) {
//                Log.e(TAG, "Could not link program: " +
//                        GLES20.glGetProgramInfoLog(program));
//                throw new RuntimeException(GLES20.glGetProgramInfoLog(program));
//            }
//            // According to the documentation of glLinkProgram():
//            // "After the link operation, applications are free to modify attached shader objects, compile
//            // attached shader objects, detach shader objects, delete shader objects, and attach additional
//            // shader objects. None of these operations affects the information log or the program that is
//            // part of the program object."
//            // But in practice, detaching shaders from the program seems to break some devices. Deleting the
//            // shaders are fine however - it will delete them when they are no longer attached to a program.
//            GLES20.glDeleteShader(vertexShader);
//            GLES20.glDeleteShader(fragmentShader);
//            GlUtil.checkNoGLES2Error("Creating GlShader");
//        }
//
//        public int getAttribLocation(String label) {
//            if (program == -1) {
//                throw new RuntimeException("The program has been released");
//            }
//            int location = GLES20.glGetAttribLocation(program, label);
//            if (location < 0) {
//                throw new RuntimeException("Could not locate '" + label + "' in program");
//            }
//            return location;
//        }
//
//        /**
//         * Enable and upload a vertex array for attribute |label|. The vertex data is specified in
//         * |buffer| with |dimension| number of components per vertex.
//         */
//        public void setVertexAttribArray(String label, int dimension, FloatBuffer buffer) {
//            if (program == -1) {
//                throw new RuntimeException("The program has been released");
//            }
//            int location = getAttribLocation(label);
//            GLES20.glEnableVertexAttribArray(location);
//            GLES20.glVertexAttribPointer(location, dimension, GLES20.GL_FLOAT, false, 0, buffer);
//            GlUtil.checkNoGLES2Error("setVertexAttribArray");
//        }
//
//        public int getUniformLocation(String label) {
//            if (program == -1) {
//                throw new RuntimeException("The program has been released");
//            }
//            int location = GLES20.glGetUniformLocation(program, label);
//            if (location < 0) {
//                throw new RuntimeException("Could not locate uniform '" + label + "' in program");
//            }
//            return location;
//        }
//
//        public void useProgram() {
//            if (program == -1) {
//                throw new RuntimeException("The program has been released");
//            }
//            GLES20.glUseProgram(program);
//            GlUtil.checkNoGLES2Error("glUseProgram");
//        }
//
//        public void release() {
//            Log.d(TAG, "Deleting shader.");
//            // Delete program, automatically detaching any shaders from it.
//            if (program != -1) {
//                GLES20.glDeleteProgram(program);
//                program = -1;
//            }
//        }
//    }
}
