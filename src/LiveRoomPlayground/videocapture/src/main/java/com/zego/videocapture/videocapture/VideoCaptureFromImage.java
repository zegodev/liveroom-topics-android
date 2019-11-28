package com.zego.videocapture.videocapture;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.view.Choreographer;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;

import com.zego.videocapture.ve_gl.EglBase;
import com.zego.videocapture.ve_gl.EglBase14;
import com.zego.videocapture.ve_gl.GlRectDrawer;
import com.zego.videocapture.ve_gl.GlUtil;
import com.zego.zegoavkit2.ZegoVideoCaptureDevice;

import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * VideoCaptureFromImage
 * 实现将图片源作为视频数据并传给ZEGO SDK，需要继承实现ZEGO SDK 的ZegoVideoCaptureDevice类
 * 采用SURFACE_TEXTURE方式传递数据，将client返回的SurfaceTexture对象转为EglSurface再进行图像绘制
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class VideoCaptureFromImage extends ZegoVideoCaptureDevice
        implements Choreographer.FrameCallback, TextureView.SurfaceTextureListener, SurfaceHolder.Callback {
    // 用于向SDK传递SurfaceTexture的相关变量
    private EglBase captureEglBase;
    private int mCaptureTextureId = 0;
    private GlRectDrawer captureDrawer;

    // 支持两种视图格式，TextureView和SurfaceView
    private TextureView mTextureView = null;
    private SurfaceView mSurfaceView = null;
    // 用于展示预览图的相关变量
    private EglBase previewEglBase;
    private int mPreviewTextureId = 0;
    private GlRectDrawer previewDrawer;
    // 纹理变换矩阵
    private float[] transformationMatrix = new float[]{1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, -1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f};

    private int mViewWidth = 0;
    private int mViewHeight = 0;
    private int mImageWidth = 0;
    private int mImageHeight = 0;

    private boolean mIsRunning = false;
    private boolean mIsCapture = false;
    private boolean mIsPreview = false;
    private Bitmap mBitmap = null;

    private HandlerThread mThread = null;
    private Handler mHandler = null;

    private Context mContext = null;
    // SDK 内部实现的、同样实现 ZegoVideoCaptureDevice.Client 协议的客户端，用于通知SDK采集结果
    private ZegoVideoCaptureDevice.Client mClient = null;
    private boolean mIsEgl14 = false;

    // 图片坐标参数
    private int mX = 0;
    private int mY = 0;
    private int mDrawCounter = 0;

    // context用于获取图片资源
    VideoCaptureFromImage(Context context) {
        mContext = context;
    }

    // 初始化OpenGL ES 的资源，为保证后续调用都是合法的，此处使用同步方式初始化
    public final int init() {
        mThread = new HandlerThread("VideoCaptureFromImage" + hashCode());
        mThread.start();
        mHandler = new Handler(mThread.getLooper());

        final CountDownLatch barrier = new CountDownLatch(1);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                // 创建EGL上下文环境
                captureEglBase = EglBase.create(null, EglBase.CONFIG_RECORDABLE);
                previewEglBase = EglBase.create(captureEglBase.getEglBaseContext(), EglBase.CONFIG_RGBA);
                // 创建实时采集和预览的绘制类
                captureDrawer = new GlRectDrawer();
                previewDrawer = new GlRectDrawer();

                mIsEgl14 = EglBase14.isEGL14Supported();
                // 注册 Choreographer 的刷新回调，保证后续绘制图片时不会出现画面撕裂的问题
                Choreographer.getInstance().postFrameCallback(VideoCaptureFromImage.this);
                mIsRunning = true;

                barrier.countDown();
            }
        });
        try {
            barrier.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // 释放OpenGL ES 的资源
    public final int uninit() {
        final CountDownLatch barrier = new CountDownLatch(1);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mIsRunning = false;
                release();
                barrier.countDown();
            }
        });
        try {
            barrier.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mHandler = null;

        if (Build.VERSION.SDK_INT >= 18) {
            mThread.quitSafely();
        } else {
            mThread.quit();
        }
        mThread = null;

        return 0;
    }

    // 将client返回的SurfaceTexture转换成EglSurface，用于 OpenGL ES 绘制
    public int setOutputSurfaceTexture(SurfaceTexture surface_texture) {
        final SurfaceTexture temp = surface_texture;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                // 先释放原有的采集Surface
                releaseCaptureSurface();

                if (temp != null) {
                    // 设置图像宽高，SDK 内部通过系统API获取后续的图像宽高
                    temp.setDefaultBufferSize(mImageWidth, mImageHeight);

                    try {
                        // 创建EGLSurface
                        captureEglBase.createSurface(temp);
                        // 绑定eglContext、eglDisplay、eglSurface
                        captureEglBase.makeCurrent();
                        mIsCapture = true;
                    } catch (RuntimeException e) {
                        // 销毁Surface对象
                        captureEglBase.releaseSurface();
                        throw e;
                    }
                } else {
                    mIsCapture = false;
                }
            }
        });
        return 0;
    }

    // 设置位图
    public void setBitmap(final Bitmap bitmap) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mBitmap = bitmap;
            }
        });
    }

    /**
     * 更新采集数据
     *
     * 当 Choreographer 刷新回调触发时，绘制图像数据到屏幕和 SDK 提供的 EglSurface 上
     */
    @Override
    public void doFrame(long frameTimeNanos) {
        if (!mIsRunning) {
            return;
        }
        Choreographer.getInstance().postFrameCallback(this);

        if (mBitmap == null) {
            return;
        }

        if (mIsPreview) {
            if (mTextureView != null) {
                attachTextureView();
            } else if (mSurfaceView != null) {
                attachSurfaceView();
            }
            if (previewEglBase.hasSurface()) {
                drawToPreview(mBitmap);
            }
        }

        if (mIsCapture && captureEglBase.hasSurface()) {
            drawToCapture(mBitmap);
        }

        if (mDrawCounter == 0) {
            mX = (mX + 1) % 4;
            if (mX == 0) {
                mY = (mY + 1) % 4;
            }
        }
        mDrawCounter = (mDrawCounter + 1) % 60;
    }

    // 绘制图像数据到SDK提供的EglSurface上
    private void drawToCapture(final Bitmap bitmap) {
        try {
            captureEglBase.makeCurrent();

            if (mCaptureTextureId == 0) {
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                mCaptureTextureId = GlUtil.generateTexture(GLES20.GL_TEXTURE_2D);
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            }

            long now = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                now = SystemClock.elapsedRealtimeNanos();
            } else {
                now = TimeUnit.MILLISECONDS.toNanos(SystemClock.elapsedRealtime());
            }

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            // 绘制rgb格式图像
            captureDrawer.drawRgb(mCaptureTextureId, transformationMatrix, mImageWidth, mImageHeight,
                    mImageWidth / 4 * mX, mImageHeight / 4 * mY,
                    mImageWidth / 4, mImageHeight / 4);

            // 交换渲染好的buffer 去显示
            if (mIsEgl14) {
                ((EglBase14) captureEglBase).swapBuffers(now);
            } else {
                captureEglBase.swapBuffers();
            }

            // 分离当前eglContext
            captureEglBase.detachCurrent();
        } catch (RuntimeException e) {
            System.out.println(e.toString());
        }
    }

    // 绘制图像数据到屏幕
    private void drawToPreview(final Bitmap bitmap) {
        try {
            previewEglBase.makeCurrent();

            if (mPreviewTextureId == 0) {
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                mPreviewTextureId = GlUtil.generateTexture(GLES20.GL_TEXTURE_2D);
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            }

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            // 绘制rgb格式图像
            previewDrawer.drawRgb(mPreviewTextureId, transformationMatrix, mViewWidth, mViewHeight,
                    mViewWidth / 4 * mX, mViewHeight / 4 * mY,
                    mViewWidth / 4, mViewHeight / 4);
            // 交换渲染好的buffer 去显示
            previewEglBase.swapBuffers();

            // 分离当前eglContext
            previewEglBase.detachCurrent();
        } catch (RuntimeException e) {
            System.out.println(e.toString());
        }
    }

    /**
     * 初始化资源，必须实现
     * @param client 通知ZEGO SDK采集结果的客户端
     */
    @Override
    protected void allocateAndStart(Client client) {
        mClient = client;

        // 初始化 OpenGL ES 的资源
        init();
        // 设置图片的位图
        setBitmap(createBitmapFromAsset());
    }

    /**
     * 释放资源，必须实现
     * 先释放OpenGL ES相关资源再清理client对象，以保证ZEGO SDK调用stopAndDeAllocate后，没有残留的异步任务导致野指针crash
     */
    @Override
    protected void stopAndDeAllocate() {
        uninit();

        mClient.destroy();
        mClient = null;
    }

    // 开始推流时，ZEGO SDK 调用 startCapture 通知外部采集设备开始工作，必须实现
    @Override
    protected int startCapture() {
        // 将client返回的SurfaceTexture转换成EglSurface，用于 OpenGL ES 绘制
        setOutputSurfaceTexture(mClient.getSurfaceTexture());
        return 0;
    }

    // 停止推流时，ZEGO SDK 调用 stopCapture 通知外部采集设备停止采集，必须实现
    @Override
    protected int stopCapture() {
        setOutputSurfaceTexture(null);
        return 0;
    }

    // 告知ZEGO SDK当前采集数据的类型，必须实现
    @Override
    protected int supportBufferType() {
        // SurfaceTexture 类型
        return PIXEL_BUFFER_TYPE_SURFACE_TEXTURE;
    }

    @Override
    protected int setFrameRate(int framerate) {
        return 0;
    }

    // 设置视图宽高
    @Override
    protected int setResolution(int width, int height) {
        mImageWidth = width;
        mImageHeight = height;
        return 0;
    }

    // 前后摄像头的切换
    @Override
    protected int setFrontCam(int bFront) {
        return 0;
    }

    // 设置展示视图
    @Override
    protected int setView(View view) {
        if (view instanceof TextureView) {
            setRendererView((TextureView) view);
        } else if (view instanceof SurfaceView) {
            setRendererView((SurfaceView) view);
        }

        return 0;
    }

    @Override
    protected int setViewMode(int nMode) {
        return 0;
    }

    @Override
    protected int setViewRotation(int nRotation) {
        return 0;
    }

    @Override
    protected int setCaptureRotation(int nRotation) {
        return 0;
    }

    // 启动预览
    @Override
    protected int startPreview() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mIsPreview = true;
            }
        });
        return 0;
    }

    // 停止预览
    @Override
    protected int stopPreview() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mIsPreview = false;
            }
        });
        return 0;
    }

    @Override
    protected int enableTorch(boolean bEnable) {
        return 0;
    }

    @Override
    protected int takeSnapshot() {
        return 0;
    }

    @Override
    protected int setPowerlineFreq(int nFreq) {
        return 0;
    }

    // 从资源区获取图片位图
    private Bitmap createBitmapFromAsset() {
        Bitmap bitmap = null;
        try {
            AssetManager assetManager = mContext.getAssets();
            InputStream is = assetManager.open("logo.png");
            bitmap = BitmapFactory.decodeStream(is);
            if (bitmap != null) {
                System.out.println("测试一:width=" + bitmap.getWidth() + " ,height=" + bitmap.getHeight());
            } else {
                System.out.println("bitmap == null");
            }
        } catch (Exception e) {
            System.out.println("异常信息:" + e.toString());
        }
        return bitmap;
    }

    // 设置渲染视图，TextureView格式
    public int setRendererView(final TextureView view) {
        final TextureView temp = view;

        if (mHandler != null) {
            final CountDownLatch barrier = new CountDownLatch(1);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    doSetRendererView(temp);
                    barrier.countDown();
                }
            });
            try {
                barrier.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            doSetRendererView(temp);
        }
        return 0;
    }

    // 设置Texture.SurfaceTextureListener回调监听
    private void doSetRendererView(TextureView temp) {
        if (mTextureView != null) {
            if (mTextureView.getSurfaceTextureListener().equals(VideoCaptureFromImage.this)) {
                mTextureView.setSurfaceTextureListener(null);
            }
            releasePreviewSurface();
        }

        mTextureView = temp;
        if (mTextureView != null) {
            mTextureView.setSurfaceTextureListener(VideoCaptureFromImage.this);
        }
    }

    // 设置预览视图，SurfaceView格式
    public int setRendererView(final SurfaceView view) {

        final SurfaceView temp = view;
        if (mHandler != null) {
            final CountDownLatch barrier = new CountDownLatch(1);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    doSetRendererView(view);
                    barrier.countDown();
                }
            });
            try {
                barrier.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            doSetRendererView(view);
        }
        return 0;
    }

    // 设置SurfaceView回调监听
    private void doSetRendererView(SurfaceView temp) {
        if (mSurfaceView != null) {
            mSurfaceView.getHolder().removeCallback(VideoCaptureFromImage.this);
            releasePreviewSurface();
        }

        mSurfaceView = temp;
        if (mSurfaceView != null) {
            mSurfaceView.getHolder().addCallback(VideoCaptureFromImage.this);
        }
    }

    // 设置预览视图，TextureView格式
    private void attachTextureView() {
        if (previewEglBase.hasSurface()) {
            return;
        }

        if (!mTextureView.isAvailable()) {
            return;
        }

        mViewWidth = mTextureView.getWidth();
        mViewHeight = mTextureView.getHeight();
        try {
            // 创建用于预览的EGLSurface
            previewEglBase.createSurface(mTextureView.getSurfaceTexture());
        } catch (RuntimeException e) {
            previewEglBase.releaseSurface();
            mViewWidth = 0;
            mViewHeight = 0;
        }
    }

    // 设置预览视图，SurfaceView格式
    private void attachSurfaceView() {
        if (previewEglBase.hasSurface()) {
            return;
        }

        SurfaceHolder holder = mSurfaceView.getHolder();
        if (holder.isCreating() || null == holder.getSurface()) {
            return;
        }

        Rect size = holder.getSurfaceFrame();
        mViewWidth = size.width();
        mViewHeight = size.height();
        try {
            // 创建用于预览的EGLSurface
            previewEglBase.createSurface(holder.getSurface());
        } catch (RuntimeException e) {
            previewEglBase.releaseSurface();
            mViewWidth = 0;
            mViewHeight = 0;
        }
    }

    // 销毁传递给ZEGO SDK的surface
    private void releaseCaptureSurface() {
        if (captureEglBase != null && captureEglBase.hasSurface()) {
            captureEglBase.makeCurrent();
            if (mCaptureTextureId != 0) {
                int[] textures = new int[]{mCaptureTextureId};
                GLES20.glDeleteTextures(1, textures, 0);
                mCaptureTextureId = 0;
            }

            captureEglBase.releaseSurface();
            captureEglBase.detachCurrent();
        }
    }


    // 销毁用于屏幕显示的surface（预览）
    private void releasePreviewSurface() {
        if (previewEglBase != null && previewEglBase.hasSurface()) {
            previewEglBase.makeCurrent();
            if (mPreviewTextureId != 0) {
                int[] textures = new int[]{mPreviewTextureId};
                GLES20.glDeleteTextures(1, textures, 0);
                mPreviewTextureId = 0;
            }

            // 销毁surface
            previewEglBase.releaseSurface();
            previewEglBase.detachCurrent();
        }
    }

    // 释放绘制相关类
    private void release() {
        // 销毁用于屏幕显示的surface
        releasePreviewSurface();

        if (previewDrawer != null) {
            previewDrawer.release();
            previewDrawer = null;
        }

        if (previewEglBase != null) {
            previewEglBase.release();
            previewEglBase = null;
        }

        // 销毁传递给ZEGO SDK的surface
        releaseCaptureSurface();

        if (captureDrawer != null) {
            captureDrawer.release();
            captureDrawer = null;
        }

        if (captureEglBase != null) {
            captureEglBase.release();
            captureEglBase = null;
        }
    }

    // TextureView.SurfaceTextureListener
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // 销毁用于预览的surface
        releasePreviewSurfaceSafe();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        // 销毁用于预览的surface
        releasePreviewSurfaceSafe();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    // SurfaceHolder.Callback
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // 销毁用于预览的surface
        releasePreviewSurfaceSafe();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        releasePreviewSurfaceSafe();
    }

    // 销毁用于预览的surface
    private void releasePreviewSurfaceSafe() {
        if (mHandler == null) {
            releasePreviewSurface();
            return;
        }
        final CountDownLatch barrier = new CountDownLatch(1);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                releasePreviewSurface();
                barrier.countDown();
            }
        });
        try {
            barrier.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

