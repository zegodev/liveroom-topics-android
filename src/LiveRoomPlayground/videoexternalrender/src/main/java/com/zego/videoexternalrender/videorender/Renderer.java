package com.zego.videoexternalrender.videorender;

import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import com.zego.videoexternalrender.ve_gl.GlRectDrawer;
import com.zego.videoexternalrender.ve_gl.GlShader;
import com.zego.videoexternalrender.ve_gl.GlUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * Renderer
 * 渲染类
 * 展示了如何渲染 RGB、YUV 类型的视频数据
 */

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
public class Renderer implements TextureView.SurfaceTextureListener {

    private static final String TAG = "RendererView";

    public static final Object lock = new Object();

    private EGLContext eglContext;
    private EGLConfig eglConfig;
    private EGLDisplay eglDisplay;
    private EGLSurface eglSurface = EGL14.EGL_NO_SURFACE;
    private Surface mTempSurface;
    private int viewWidth = 0;
    private int viewHeight = 0;
    private GlShader shader;
    private int mTextureId = 0;

    private TextureView mTextureView;
    private String streamID;

    // 处理 yuv 格式视频数据的 drawer
    private GlRectDrawer mDrawer = null;
    // 处理 rgb 格式视频数据的 drawer
    private GlRectDrawer mRgbDrawer = null;

    // 纹理变换矩阵，图像会正立显示
    private float[] flipMatrix = new float[]{1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, -1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f};

    // 设置流名，是当前待渲染的拉流或者推流的流名
    public void setStreamID(String streamID) {
        this.streamID = streamID;
    }


    /** 初始化 Renderer
     *
     * @param eglContext OpenGL的共享上下文
     * @param eglDisplay 关联显示屏的通用数据类型
     * @param eglConfig  绘图配置
     */
    public Renderer(EGLContext eglContext, EGLDisplay eglDisplay, EGLConfig eglConfig) {
        this.eglContext = eglContext;
        this.eglDisplay = eglDisplay;
        this.eglConfig = eglConfig;
    }

    // 绑定eglContext、eglDisplay、eglSurface
    private void makeCurrent() {
        if (eglSurface == EGL14.EGL_NO_SURFACE) {
            throw new RuntimeException("No EGLSurface - can't make current");
        }
        synchronized (lock) {
            if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
                throw new RuntimeException(
                        "eglMakeCurrent failed: 0x" + Integer.toHexString(EGL14.eglGetError()));
            }
        }
    }

    // 将Buffer绘制到当前的view上
    public void draw(VideoRenderer.PixelBuffer pixelBuffer) {

        if (mTextureView != null) {
            attachTextureView();
        } else {
            Log.e(TAG, "draw error view is null");
            return;
        }

        if (pixelBuffer == null || eglSurface == EGL14.EGL_NO_SURFACE) {
            return;
        }

        // 绘制 yuv 格式的buffer
        if (pixelBuffer.strides[2] > 0) {

            if (mDrawer == null) {
                // 创建 yuv 格式的 drawer
                mDrawer = new GlRectDrawer();
            }

            // 绑定eglContext、eglDisplay、eglSurface
            makeCurrent();

            // 生成Texture并上传贴图
            yuvTextures = uploadYuvData(pixelBuffer.width, pixelBuffer.height, pixelBuffer.strides, pixelBuffer.buffer);

            int[] value = measure(pixelBuffer.width, pixelBuffer.height, viewWidth, viewHeight);
            // 渲染yuv格式图像
            mDrawer.drawYuv(yuvTextures, flipMatrix, pixelBuffer.width, pixelBuffer.height, value[0], value[1], value[2], value[3]);
            // 交换渲染好的buffer 去显示
            swapBuffers();
            // 分离当前eglContext
            detachCurrent();
        } else {

            // 绘制 rgb 格式的buffer

            // 绑定eglContext、eglDisplay、eglSurface
            makeCurrent();

            if (mTextureId == 0) {
                // 生成纹理
                mTextureId = GlUtil.generateTexture(GLES20.GL_TEXTURE_2D);
            }

            // 选择可以由纹理函数进行修改的当前纹理单位
            GLES20.glActiveTexture(GLES20.GL_TEXTURE4);
            // 绑定纹理
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);

            if (mRgbDrawer == null) {
                // 创建绘制rgb的drawer
                mRgbDrawer = new GlRectDrawer();
            }

            // 生成2D纹理
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, pixelBuffer.width, pixelBuffer.height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelBuffer.buffer[0]);
            // 获取展示图像的平面坐标及宽高
            int[] value = measure(pixelBuffer.width, pixelBuffer.height, viewWidth, viewHeight);
            // 渲染rgb格式图像
            mRgbDrawer.drawRgb(mTextureId, flipMatrix, pixelBuffer.width, pixelBuffer.height, value[0], value[1], value[2], value[3]);
            // 交换渲染好的buffer 去显示
            swapBuffers();
            // 分离当前eglContext
            detachCurrent();
        }
    }

    // 根据图像数据和展示视图的宽高计算缩放后的实际图像宽高及平面坐标
    private int[] measure(int imageWidth, int imageHeight, int viewWidth, int viewHeight) {
        int[] value = {0, 0, viewWidth, viewHeight};
        float scale;
        scale = (float) viewWidth / (float)imageWidth;
        float height = imageHeight * scale;
        value[0] = 0;
        value[1] = (int) (viewHeight - height) / 2;
        value[2] = viewWidth;
        value[3] = (int) height;
        return value;
    }

    // 设置渲染视图
    public int setRendererView(TextureView view) {
        if (view != null && view == mTextureView) {
            return 0;
        }
        final TextureView temp = view;

        if (mTextureView != null) {
            if (eglSurface != EGL14.EGL_NO_SURFACE) {
                uninitEGLSurface();
            }
            mTextureView.setSurfaceTextureListener(null);
            mTextureView = null;
            if (shader != null) {
                shader.release();
            }
        }

        mTextureView = temp;
        if (mTextureView != null) {
            mTextureView.setSurfaceTextureListener(this);
        }

        return 0;
    }

    // 将待展示的TextureView附着到EGL Surface上
    private void attachTextureView() {

        // 判断是否切入后台后又切入前台，若有需要重新创建Surface
        if (isTextureAvailable){
            releaseSurface();
            isTextureAvailable = false;
        }

        if (eglSurface != EGL14.EGL_NO_SURFACE
                && eglContext != EGL14.EGL_NO_CONTEXT
                && eglDisplay != EGL14.EGL_NO_DISPLAY)
        {
            return;
        }

        if (!mTextureView.isAvailable()) {
            return;
        }

        mTempSurface = new Surface(mTextureView.getSurfaceTexture());
        viewWidth = mTextureView.getWidth();
        viewHeight = mTextureView.getHeight();
        try {
            initEGLSurface(mTempSurface);
        } catch (Exception e) {
            viewWidth = 0;
            viewWidth = 0;
        }
    }

    // 创建EGL Surface
    private void initEGLSurface(Surface surface) {
        try {
            // Both these statements have been observed to fail on rare occasions, see BUG=webrtc:5682.
            createSurface(surface);
            // 绑定eglContext、eglDisplay、eglSurface
            makeCurrent();
        } catch (RuntimeException e) {
            // Clean up before rethrowing the exception.
            uninitEGLSurface();
            throw e;
        }

        // 分离当前的EGL context
        detachCurrent();
    }

    // 分离当前的EGL context，以便可以在另一个线程上使其成为当前的EGL context
    private void detachCurrent() {
        synchronized (lock) {
            if (!EGL14.eglMakeCurrent(
                    eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)) {
                throw new RuntimeException(
                        "eglDetachCurrent failed: 0x" + Integer.toHexString(EGL14.eglGetError()));
            }
        }
    }

    // 创建EGL Surface
    private void createSurface(Object surface) {
        if (!(surface instanceof Surface) && !(surface instanceof SurfaceTexture)) {
            throw new IllegalStateException("Input must be either a Surface or SurfaceTexture");
        }

        if (eglSurface != EGL14.EGL_NO_SURFACE) {
            throw new RuntimeException("Already has an EGLSurface");
        }
        int[] surfaceAttribs = {EGL14.EGL_NONE};

        eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, surface, surfaceAttribs, 0);
        if (eglSurface == EGL14.EGL_NO_SURFACE) {
            throw new RuntimeException(
                    "Failed to create window surface: 0x" + Integer.toHexString(EGL14.eglGetError()));
        }
        Log.i(TAG, "createSurface");
    }

    // 释放EGL Surface
    public void uninitEGLSurface() {
        if (mTextureId != 0) {
            int[] textures = new int[]{mTextureId};
            GLES20.glDeleteTextures(1, textures, 0);
            mTextureId = 0;
        }

        releaseSurface();
        detachCurrent();

        if (mTempSurface != null) {
            mTempSurface.release();
            mTempSurface = null;
        }
    }

    private void releaseSurface() {
        if (eglSurface != EGL14.EGL_NO_SURFACE) {
            //销毁Surface对象
            EGL14.eglDestroySurface(eglDisplay, eglSurface);
            eglSurface = EGL14.EGL_NO_SURFACE;
        }
    }

    // 交换渲染好的buffer 去显示
    public void swapBuffers() {
        if (eglSurface == EGL14.EGL_NO_SURFACE) {
            throw new RuntimeException("No EGLSurface - can't swap buffers");
        }
        synchronized (lock) {
            EGL14.eglSwapBuffers(eglDisplay, eglSurface);
        }
    }

    // 释放 Render
    public void uninit() {
        if (mTextureView != null) {
            mTextureView.setSurfaceTextureListener(null);
            mTextureView = null;
        }
        if (shader != null) {
            shader.release();
        }
        if (mDrawer != null) {
            mDrawer.release();
        }
        if (mRgbDrawer != null) {
            mRgbDrawer.release();
        }
        eglContext = null;
        eglDisplay = null;
        eglConfig = null;
        shader = null;
        mDrawer = null;
        mRgbDrawer = null;
    }

    private boolean isTextureAvailable = false;

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        isTextureAvailable = true;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        Log.i(TAG, "onSurfaceTextureDestroyed");
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }


    private ByteBuffer copyBuffer;
    private int[] yuvTextures;
    private ByteBuffer[] packedByteBuffer = new ByteBuffer[3];

    // 上传yuv plane
    public int[] uploadYuvData(int width, int height, int[] strides, ByteBuffer[] planes) {
        // 三个plane的width
        final int[] planeWidths = new int[]{width, width / 2, width / 2};
        // 确保strides裁剪后是4字节对齐
        // 三个plane的stride
        final int[] destStrides = new int[3];
        for (int i = 0; i < planeWidths.length; i++) {
            if (planeWidths[i] % 4 == 0) {
                destStrides[i] = planeWidths[i];
            } else {
                destStrides[i] = (planeWidths[i] / 4 + 1) * 4;
            }
        }
        // 三个plane的height
        final int[] planeHeights = new int[]{height, height / 2, height / 2};

        // 确定中间变量buffer的存储大小
        int copyCapacityNeeded = 0;
        for (int i = 0; i < 3; ++i) {
            if (strides[i] > planeWidths[i]) {
                copyCapacityNeeded = Math.max(copyCapacityNeeded, planeWidths[i] * planeHeights[i]);
            }
        }
        // 为中间变量buffer分配存储
        if (copyCapacityNeeded > 0
                && (copyBuffer == null || copyBuffer.capacity() < copyCapacityNeeded)) {
            copyBuffer = ByteBuffer.allocateDirect(copyCapacityNeeded);
        }
        // 生成三个纹理
        if (yuvTextures == null) {
            yuvTextures = new int[3];
            for (int i = 0; i < 3; i++) {
                yuvTextures[i] = GlUtil.generateTexture(GLES20.GL_TEXTURE_2D);
            }
        }

        // 上传三个贴图
        for (int i = 0; i < 3; ++i) {
            // 选择可以由纹理函数进行修改的当前纹理单位
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i);
            // 绑定纹理
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextures[i]);

            // GLES 只接收 packed data, 即 stride == planeWidth.
            if (strides[i] == planeWidths[i]) {
                // 此plane是 packed data
                packedByteBuffer[i] = planes[i];

            } else {
                copyBuffer.clear();
                planes[i].position(0);
                // 裁剪plane，使其是 packed data，裁剪后的数据存入 copybuffer
                copyPlane(
                        planes[i], strides[i], copyBuffer, destStrides[i], planeWidths[i], planeHeights[i]);
                packedByteBuffer[i] = copyBuffer;
            }
            packedByteBuffer[i].position(0);
//            GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1); // 1像素对齐
            // 生成2D纹理
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, planeWidths[i],
                    planeHeights[i], 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, packedByteBuffer[i]);
        }
        return yuvTextures;
    }

    // 源码请查看 main/cpp/CutPlane.cpp
    // 根据stride裁剪plane
    public native void copyPlane(ByteBuffer src, int srcStride, ByteBuffer dst, int dstStride, int width, int height);

}
