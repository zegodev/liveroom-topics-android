package com.zego.videoexternalrender.videorender;

import android.annotation.TargetApi;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Choreographer;
import android.view.TextureView;

import com.zego.zegoavkit2.entities.VideoFrame;
import com.zego.zegoavkit2.enums.VideoPixelFormat;
import com.zego.zegoavkit2.videorender.IZegoExternalRenderCallback2;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;


/**
 * Created by robotding on 16/9/23.
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class VideoRenderer implements Choreographer.FrameCallback, IZegoExternalRenderCallback2 {
    private static final String TAG = "VideoRenderer";

    public static final Object lock = new Object();

    public static final int[] CONFIG_RGBA = {
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_NONE
    };

    private EGLContext eglContext;
    private EGLConfig eglConfig;
    private EGLDisplay eglDisplay;
    private boolean mIsRunning = false;
    private int bufferQueueSizeMax = 4;

    private HandlerThread mThread = null;
    private Handler mHandler = null;

    static class PixelBuffer {
        public int width;
        public int height;
        public ByteBuffer[] buffer;
        public int[] strides;
    }

    private ArrayList<PixelBuffer> mProduceQueue = null;
    private int mWriteIndex = 0;
    private int mWriteRemain = 0;
    private ConcurrentHashMap<String, Renderer> rendererMap = null;

    private Map<String, ConcurrentLinkedQueue<PixelBuffer>> mMapConsumeQueue = null;

    private int[] mMaxBufferSize = {0,0,0,0};

    public final int init() {
        mThread = new HandlerThread("VideoRenderer" + hashCode());
        mThread.start();
        mHandler = new Handler(mThread.getLooper());

        final CountDownLatch barrier = new CountDownLatch(1);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                eglDisplay = getEglDisplay();
                eglConfig = getEglConfig(eglDisplay, CONFIG_RGBA);
                eglContext = createEglContext(null, eglDisplay, eglConfig);

                Choreographer.getInstance().postFrameCallback(VideoRenderer.this);
                mIsRunning = true;

                barrier.countDown();
            }
        });
        try {
            barrier.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        rendererMap = new ConcurrentHashMap<>();
        mProduceQueue = new ArrayList<>();
        mMapConsumeQueue = new ConcurrentHashMap<>();
        return 0;
    }

    // 添加需要渲染视图
    public void addView(final String streamID, final TextureView textureView) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (rendererMap.get(streamID) == null) {
                    Log.i(TAG, String.format("new Renderer streamId : %s", streamID));
                    // 添加view
                    Renderer renderer = new Renderer(eglContext, eglDisplay, eglConfig);
                    renderer.setRendererView(textureView);
                    renderer.setStreamID(streamID);
                    rendererMap.put(streamID, renderer);
                } else {
                    rendererMap.get(streamID).setRendererView(textureView);
                    Log.i(TAG, String.format("setRendererView Renderer streamId : %s", streamID));
                }
            }
        });
    }

    // 删除指定流绑定的渲染视图
    public void removeView(final String streamID) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (rendererMap.get(streamID) != null) {
                    Log.i(TAG, String.format("removeView Renderer streamId : %s", streamID));
                    rendererMap.get(streamID).uninitEGLSurface();
                    rendererMap.get(streamID).uninit();
                    rendererMap.remove(streamID);
                }
            }
        });
    }

    // 删除全部需要渲染
    public void removeAllView() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "removeAllView");
                for (Map.Entry<String, Renderer> entry : rendererMap.entrySet()) {
                    Renderer renderer = entry.getValue();
                    renderer.uninitEGLSurface();
                    renderer.uninit();
                    rendererMap.remove(entry.getKey());
                    mMapConsumeQueue.remove(entry.getKey());
                }
                // 重置缓冲区
                mWriteRemain = bufferQueueSizeMax;

            }
        });
    }

    private void release() {

        for (Map.Entry<String, Renderer> entry : rendererMap.entrySet()) {
            Renderer renderer = entry.getValue();
            renderer.uninitEGLSurface();
            renderer.uninit();
        }

        //销毁 EGLContext 对象
        EGL14.eglDestroyContext(eglDisplay, eglContext);
        //释放线程
        EGL14.eglReleaseThread();
        //终止 Display 对象
        EGL14.eglTerminate(eglDisplay);

        eglContext = EGL14.EGL_NO_CONTEXT;
        eglDisplay = EGL14.EGL_NO_DISPLAY;
        eglConfig = null;
    }

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
        mHandler.removeCallbacksAndMessages(null);
        mHandler = null;

        if (Build.VERSION.SDK_INT >= 18) {
            mThread.quitSafely();
        } else {
            mThread.quit();
        }
        mThread = null;

        for (Map.Entry<String, Renderer> entry : rendererMap.entrySet()) {
            Renderer renderer = entry.getValue();
            renderer.uninit();
        }

        rendererMap = null;

        Choreographer.getInstance().removeFrameCallback(VideoRenderer.this);

        for (Map.Entry<String, ConcurrentLinkedQueue<PixelBuffer>> entry : mMapConsumeQueue.entrySet()) {
            ConcurrentLinkedQueue<PixelBuffer> concurrentLinkedQueue = entry.getValue();
            if (concurrentLinkedQueue != null) {
                concurrentLinkedQueue.clear();
            }
        }

        mProduceQueue.clear();

        return 0;
    }

    // Return an EGLDisplay, or die trying.
    private static EGLDisplay getEglDisplay() {
        EGLDisplay eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException(
                    "Unable to get EGL14 display: 0x" + Integer.toHexString(EGL14.eglGetError()));
        }
        int[] version = new int[2];
        // 初始化 EGL
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            throw new RuntimeException(
                    "Unable to initialize EGL14: 0x" + Integer.toHexString(EGL14.eglGetError()));
        }
        return eglDisplay;
    }

    // Return an EGLConfig, or die trying.
    private static EGLConfig getEglConfig(EGLDisplay eglDisplay, int[] configAttributes) {
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        //选择最佳的 Surface 配置
        if (!EGL14.eglChooseConfig(
                eglDisplay, configAttributes, 0, configs, 0, configs.length, numConfigs, 0)) {
            throw new RuntimeException(
                    "eglChooseConfig failed: 0x" + Integer.toHexString(EGL14.eglGetError()));
        }
        if (numConfigs[0] <= 0) {
            throw new RuntimeException("Unable to find any matching EGL config");
        }
        final EGLConfig eglConfig = configs[0];
        if (eglConfig == null) {
            throw new RuntimeException("eglChooseConfig returned null");
        }
        return eglConfig;
    }

    // Return an EGLConfig, or die trying.
    private static EGLContext createEglContext(
            EGLContext sharedContext, EGLDisplay eglDisplay, EGLConfig eglConfig) {
        if (sharedContext != null && sharedContext == EGL14.EGL_NO_CONTEXT) {
            throw new RuntimeException("Invalid sharedContext");
        }
        int[] contextAttributes = {EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE};
        EGLContext rootContext =
                sharedContext == null ? EGL14.EGL_NO_CONTEXT : sharedContext;
        final EGLContext eglContext;
        synchronized (VideoRenderer.lock) {
            // 创建记录 OpenGL ES 状态机信息的对象 EGLContext
            eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, rootContext, contextAttributes, 0);
        }
        if (eglContext == EGL14.EGL_NO_CONTEXT) {
            throw new RuntimeException(
                    "Failed to create EGL context: 0x" + Integer.toHexString(EGL14.eglGetError()));
        }
        return eglContext;
    }


    @Override
    public void doFrame(long frameTimeNanos) {
        if (!mIsRunning) {
            return;
        }
        Choreographer.getInstance().postFrameCallback(this);

        draw();
    }

    private void draw() {

        for (Map.Entry<String, ConcurrentLinkedQueue<PixelBuffer>> entry : mMapConsumeQueue.entrySet()) {
            ConcurrentLinkedQueue<PixelBuffer> concurrentLinkedQueue = entry.getValue();
            if (concurrentLinkedQueue != null) {
                String streamID = entry.getKey();
                PixelBuffer pixelBuffer = concurrentLinkedQueue.poll();
                if (pixelBuffer == null) {
                    continue;
                }

                Renderer renderer = rendererMap.get(streamID);
                if (renderer != null) {
                    renderer.draw(pixelBuffer);
                }

                returnProducerPixelBuffer(pixelBuffer);
            }
        }

    }

    private void createPixelBufferPool(int[] size, int count) {
        for (int i = 0; i < count; i++){
            PixelBuffer pixelBuffer = new PixelBuffer();
            pixelBuffer.buffer = new ByteBuffer[3];
            pixelBuffer.buffer[0] = ByteBuffer.allocateDirect(size[0]);
            pixelBuffer.buffer[1] = ByteBuffer.allocateDirect(size[1]);
            pixelBuffer.buffer[2] = ByteBuffer.allocateDirect(size[2]);

            mProduceQueue.add(pixelBuffer);
        }

        mWriteRemain = count;
        mWriteIndex = -1;
    }

    private int height, width;
    private int strides[];

    private synchronized void returnProducerPixelBuffer(PixelBuffer pixelBuffer) {
        mWriteRemain++;
    }
    // 外部渲染回调
    @Override
    public int dequeueInputBuffer(int width, int height, int[] strides, int[] byteBufferLens) {

        boolean isChange = false;
        for (int i = 0; i < strides.length; i++){
            if (strides[i] * height > mMaxBufferSize[i]) {
                mMaxBufferSize[i] = strides[i] *height;
                isChange = true;
            }
        }
        if (isChange) {
            if (mMaxBufferSize[0]>0){
                mProduceQueue.clear();
            }
            createPixelBufferPool(mMaxBufferSize, 1);
        }

        this.height = height;
        this.width = width;
        this.strides = strides;

        if (mWriteRemain == 0) {
            return -1;
        }

        if (mProduceQueue.size() == 0) {
            return -1;
        }

        mWriteRemain--;
        return (mWriteIndex + 1) % mProduceQueue.size();
    }

    @Override
    public VideoFrame getInputBuffer(int index) {

        if (mProduceQueue.isEmpty()) {
            return null;
        }

        VideoFrame videoFrame = new VideoFrame();
        ByteBuffer[] buffers;

        buffers = new ByteBuffer[3];
        buffers[0] = mProduceQueue.get(index).buffer[0];
        buffers[1] = mProduceQueue.get(index).buffer[1];
        buffers[2] = mProduceQueue.get(index).buffer[2];

        videoFrame.byteBuffers = buffers;
        videoFrame.height = this.height;
        videoFrame.width = this.width;
        videoFrame.strides = this.strides;

        return videoFrame;
    }

    @Override
    public void queueInputBuffer(int bufferIndex, String streamID, VideoPixelFormat videoPixelFormat) {
        if (bufferIndex == -1) {
            return;
        }
        if (mProduceQueue.size() > bufferIndex) {
            VideoRenderer.PixelBuffer pixelBuffer = mProduceQueue.get(bufferIndex);
            pixelBuffer.width = this.width;
            pixelBuffer.height = this.height;
            pixelBuffer.strides = this.strides;

            ConcurrentLinkedQueue<PixelBuffer> concurrentLinkedQueue = mMapConsumeQueue.get(streamID);
            if (concurrentLinkedQueue == null) {
                concurrentLinkedQueue = new ConcurrentLinkedQueue<>();
                concurrentLinkedQueue.add(pixelBuffer);
                mMapConsumeQueue.put(streamID, concurrentLinkedQueue);
            } else {
                concurrentLinkedQueue.add(pixelBuffer);
            }

            mWriteIndex = (mWriteIndex + 1) % mProduceQueue.size();
        }
    }
}
