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
 * Created by zego on 2018/12/28.
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

    private GlRectDrawer mDrawer = null;
    private GlRectDrawer mRgbDrawer = null;
    private final Matrix renderMatrix = new Matrix();
    private float[] flipMatrix = new float[]{1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, -1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f};

    public void setStreamID(String streamID) {
        this.streamID = streamID;
    }


    public Renderer(EGLContext eglContext, EGLDisplay eglDisplay, EGLConfig eglConfig) {
        this.eglContext = eglContext;
        this.eglDisplay = eglDisplay;
        this.eglConfig = eglConfig;
    }

    // 绑定context
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

    // 绘制Buffer到当前的view
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

        // if yuv
        if (pixelBuffer.strides[2] > 0) {

            if (mDrawer == null) {
                mDrawer = new GlRectDrawer();
            }

            // 正图像
            renderMatrix.reset();
            renderMatrix.preTranslate(0.5f, 0.5f);
            renderMatrix.preScale(1f,-1f); // I420上下颠倒
            renderMatrix.preTranslate(-0.5f, -0.5f);

            makeCurrent();

            // Bind the textures.
            yuvTextures = uploadYuvData(pixelBuffer.width,pixelBuffer.height,pixelBuffer.strides,pixelBuffer.buffer);

            int[] value = measure(pixelBuffer.width, pixelBuffer.height, viewWidth, viewHeight);
            float[] matrix = convertMatrixFromAndroidGraphicsMatrix(renderMatrix);
            mDrawer.drawYuv(yuvTextures,matrix,pixelBuffer.width,pixelBuffer.height,value[0], value[1], value[2], value[3]);
            swapBuffers();
            detachCurrent();
        } else {

            makeCurrent();

            if (mTextureId == 0) {
                mTextureId = GlUtil.generateTexture(GLES20.GL_TEXTURE_2D);
            }

            GLES20.glActiveTexture(GLES20.GL_TEXTURE4);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);

            if (mRgbDrawer == null) {
                mRgbDrawer = new GlRectDrawer();
            }

            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, pixelBuffer.width, pixelBuffer.height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelBuffer.buffer[0]);
            int[] value = measure(pixelBuffer.width, pixelBuffer.height, viewWidth, viewHeight);
            mRgbDrawer.drawRgb(mTextureId,flipMatrix,pixelBuffer.width,pixelBuffer.height,value[0], value[1], value[2], value[3]);
            swapBuffers();
            detachCurrent();
        }
    }


    private int[] measure(int imageWidth, int imageHeight, int viewWidth, int viewHeight) {
        int[] value = {0, 0, viewWidth, viewHeight};
        float scale;
        scale = viewWidth / imageWidth;
        float height = imageHeight * scale;
        value[0] = 0;
        value[1] = (int) (viewHeight - height) / 2;
        value[2] = viewWidth;
        value[3] = (int) height;
        return value;
    }

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

    private void attachTextureView() {
        if (eglSurface != EGL14.EGL_NO_SURFACE
                && eglContext != EGL14.EGL_NO_CONTEXT
                && eglDisplay != EGL14.EGL_NO_DISPLAY
                ) {
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

    // 创建Surface
    private void initEGLSurface(Surface surface) {
        try {
            // Both these statements have been observed to fail on rare occasions, see BUG=webrtc:5682.
            createSurface(surface);
            makeCurrent();
        } catch (RuntimeException e) {
            // Clean up before rethrowing the exception.
            uninitEGLSurface();
            throw e;
        }

        detachCurrent();
    }

    // Detach the current EGL context, so that it can be made current on another thread.
    private void detachCurrent() {
        synchronized (lock) {
            if (!EGL14.eglMakeCurrent(
                    eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)) {
                throw new RuntimeException(
                        "eglDetachCurrent failed: 0x" + Integer.toHexString(EGL14.eglGetError()));
            }
        }
    }

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

    public void uninit() {
        if (mTextureView != null) {
            mTextureView.setSurfaceTextureListener(null);
            mTextureView = null;
        }
        if (shader != null) {
            shader.release();
        }
        if (mDrawer != null){
            mDrawer.release();
        }
        if (mRgbDrawer != null){
            mRgbDrawer.release();
        }
        eglContext = null;
        eglDisplay = null;
        eglConfig = null;
        shader = null;
        mDrawer = null;
        mRgbDrawer = null;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {

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
    private ByteBuffer[] packedByteBuffer =  new ByteBuffer[3];

    public int[] uploadYuvData(int width, int height, int[] strides, ByteBuffer[] planes) {
        final int[] planeWidths = new int[] {width, width / 2, width / 2};
        // 确保strides裁剪后是4字节对齐
        final int[] destStrides = new int[3];
        for (int i = 0; i < planeWidths.length; i++){
            if (planeWidths[i]%4 == 0){
                destStrides[i] = planeWidths[i];
            }else {
                destStrides[i] = (planeWidths[i]/4+1)*4;
            }
        }
        final int[] planeHeights = new int[] {height, height / 2, height / 2};

        // Make a first pass to see if we need a temporary copy buffer.
        int copyCapacityNeeded = 0;
        for (int i = 0; i < 3; ++i) {
            if (strides[i] > planeWidths[i]) {
                copyCapacityNeeded = Math.max(copyCapacityNeeded, planeWidths[i] * planeHeights[i]);
            }
        }
        // Allocate copy buffer if necessary.
        if (copyCapacityNeeded > 0
                && (copyBuffer == null || copyBuffer.capacity() < copyCapacityNeeded)) {
            copyBuffer = ByteBuffer.allocateDirect(copyCapacityNeeded);
        }
        // Make sure YUV textures are allocated.
        if (yuvTextures == null) {
            yuvTextures = new int[3];
            for (int i = 0; i < 3; i++) {
                yuvTextures[i] = GlUtil.generateTexture(GLES20.GL_TEXTURE_2D);
            }
        }

        // Upload each plane.
        for (int i = 0; i < 3; ++i) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextures[i]);

            // GLES only accepts packed data, i.e. stride == planeWidth.
            if (strides[i] == planeWidths[i]) {
                // Input is packed already.
                packedByteBuffer[i] = planes[i];

            } else {
                copyBuffer.clear();
                planes[i].position(0);
                copyPlane(
                        planes[i], strides[i], copyBuffer, destStrides[i], planeWidths[i], planeHeights[i]);
                packedByteBuffer[i] = copyBuffer;
            }
            packedByteBuffer[i].position(0);
//            GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1); // 1像素对齐
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, planeWidths[i],
                    planeHeights[i], 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, packedByteBuffer[i]);
        }
        return yuvTextures;
    }

    // tools
    public static float[] convertMatrixFromAndroidGraphicsMatrix(android.graphics.Matrix matrix) {
        float[] values = new float[9];
        matrix.getValues(values);

        // The android.graphics.Matrix looks like this:
        // [x1 y1 w1]
        // [x2 y2 w2]
        // [x3 y3 w3]
        // We want to contruct a matrix that looks like this:
        // [x1 y1  0 w1]
        // [x2 y2  0 w2]
        // [ 0  0  1  0]
        // [x3 y3  0 w3]
        // Since it is stored in column-major order, it looks like this:
        // [x1 x2 0 x3
        //  y1 y2 0 y3
        //   0  0 1  0
        //  w1 w2 0 w3]
        // clang-format off
        float[] matrix4x4 = {
                values[0 * 3 + 0], values[1 * 3 + 0], 0, values[2 * 3 + 0],
                values[0 * 3 + 1], values[1 * 3 + 1], 0, values[2 * 3 + 1],
                0, 0, 1, 0,
                values[0 * 3 + 2], values[1 * 3 + 2], 0, values[2 * 3 + 2],
        };
        // clang-format on
        return matrix4x4;
    }

    public native void copyPlane(ByteBuffer src, int srcStride, ByteBuffer dst, int dstStride, int width, int height);

}
