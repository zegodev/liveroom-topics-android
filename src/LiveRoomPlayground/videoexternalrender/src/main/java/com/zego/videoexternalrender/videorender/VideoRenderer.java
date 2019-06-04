package com.zego.videoexternalrender.videorender;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;
import android.view.Choreographer;
import android.view.Surface;
import android.view.TextureView;

import com.zego.zegoavkit2.entities.VideoFrame;
import com.zego.zegoavkit2.enums.VideoPixelFormat;
import com.zego.zegoavkit2.videorender.IZegoExternalRenderCallback2;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * VideoRenderer
 * 渲染类 Renderer 的封装层，接口更利于上层调用
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class VideoRenderer implements Choreographer.FrameCallback, IZegoExternalRenderCallback2 {
    private static final String TAG = "VideoRenderer";

    public static final Object lock = new Object();

    // opengl 颜色配置
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

    //  AVCANNEXB 模式解码器
    private AVCDecoder mAVCDecoder = null;

    private int mViewWidth = 540;
    private int mViewHeight = 960;

    private HandlerThread mThread = null;
    private Handler mHandler = null;


    /** 单帧视频数据
     *  包含视频画面的宽、高、数据、strides
     */
    static class PixelBuffer {
        public int width;
        public int height;
        public ByteBuffer[] buffer;
        public int[] strides;
    }

    // 生产队列
    private ArrayList<PixelBuffer> mProduceQueue = null;
    private int mWriteIndex = 0;
    private int mWriteRemain = 0;
    // 流名、渲染对象的键值map
    private ConcurrentHashMap<String, Renderer> rendererMap = null;

    // 消费队列
    private Map<String, ConcurrentLinkedQueue<PixelBuffer>> mMapConsumeQueue = null;

    // 视频数据buffer的最大size
    private int[] mMaxBufferSize = {0,0,0,0};

    // 初始化，包含线程启动，视频帧回调监听，opengl相关参数的设置等
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

    // 添加解码 AVCANNEXB 格式视频帧的渲染视图
    public void addDecodView(final TextureView textureView){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mAVCDecoder == null){
                    // 创建解码器
                    mAVCDecoder = new AVCDecoder(new Surface(textureView.getSurfaceTexture()), mViewWidth, mViewHeight);
                    // 启动解码器
                    mAVCDecoder.startDecoder();
                }
            }
        });
    }

    // 根据流名添加渲染视图
    public void addView(final String streamID, final TextureView textureView) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (rendererMap.get(streamID) == null) {
                    Log.i(TAG, String.format("new Renderer streamId : %s", streamID));
                    // 创建渲染类对象
                    Renderer renderer = new Renderer(eglContext, eglDisplay, eglConfig);
                    // 设置渲染view
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
                    // 释放 EGL Surface
                    rendererMap.get(streamID).uninitEGLSurface();
                    // 释放 Render
                    rendererMap.get(streamID).uninit();
                    rendererMap.remove(streamID);
                }
            }
        });
    }

    // 删除全部渲染视图
    public void removeAllView() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "removeAllView");
                for (Map.Entry<String, Renderer> entry : rendererMap.entrySet()) {
                    Renderer renderer = entry.getValue();
                    // 释放 EGL Surface
                    renderer.uninitEGLSurface();
                    // 释放 Render
                    renderer.uninit();
                    rendererMap.remove(entry.getKey());
                    mMapConsumeQueue.remove(entry.getKey());
                }
                // 重置缓冲区
                mWriteRemain = bufferQueueSizeMax;

            }
        });
    }

    // 释放渲染类 Render
    private void release() {

        for (Map.Entry<String, Renderer> entry : rendererMap.entrySet()) {
            Renderer renderer = entry.getValue();
            renderer.uninitEGLSurface();
            renderer.uninit();
        }

        // 销毁 EGLContext 对象
        EGL14.eglDestroyContext(eglDisplay, eglContext);
        // 释放线程
        EGL14.eglReleaseThread();
        // 终止 Display 对象
        EGL14.eglTerminate(eglDisplay);

        eglContext = EGL14.EGL_NO_CONTEXT;
        eglDisplay = EGL14.EGL_NO_DISPLAY;
        eglConfig = null;
    }

    // 处理释放相关操作，线程停止、移除视频帧回调监听等
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

        // 移除视频帧回调监听
        Choreographer.getInstance().removeFrameCallback(VideoRenderer.this);

        for (Map.Entry<String, ConcurrentLinkedQueue<PixelBuffer>> entry : mMapConsumeQueue.entrySet()) {
            ConcurrentLinkedQueue<PixelBuffer> concurrentLinkedQueue = entry.getValue();
            if (concurrentLinkedQueue != null) {
                concurrentLinkedQueue.clear();
            }
        }

        mProduceQueue.clear();

        // 释放MediaCodec
        if (mAVCDecoder != null) {
            mAVCDecoder.stopAndReleaseDecoder();
            mAVCDecoder = null;
        }

        printCount = 0;
        return 0;
    }

    // 获取 EGLDisplay
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

    // 获取 EGLConfig
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

    // 创建 EGLContext
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


    // 视频帧回调实现
    @Override
    public void doFrame(long frameTimeNanos) {
        if (!mIsRunning) {
            return;
        }
        Choreographer.getInstance().postFrameCallback(this);

        // 对视频帧进行绘制
        draw();
    }

    // 使用渲染类型进行绘制
    private void draw() {

        for (Map.Entry<String, ConcurrentLinkedQueue<PixelBuffer>> entry : mMapConsumeQueue.entrySet()) {
            ConcurrentLinkedQueue<PixelBuffer> concurrentLinkedQueue = entry.getValue();
            if (concurrentLinkedQueue != null) {
                String streamID = entry.getKey();
                // 从消费队列获取视频帧数据
                PixelBuffer pixelBuffer = concurrentLinkedQueue.poll();
                if (pixelBuffer == null) {
                    continue;
                }

                // 获取流名对应的渲染类对象
                Renderer renderer = rendererMap.get(streamID);
                if (renderer != null) {
                    // 渲染类根据视频帧数据进行绘制
                    renderer.draw(pixelBuffer);
                }

                // 修改生产者队列的待写buffer index
                returnProducerPixelBuffer(pixelBuffer);
            }
        }
    }

    // 创建count个视频帧buffer
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

    /** SDK 向 App 获取 Buffer 索引
     * SDK 会通过这个返回值向 App 请求对应的 ByteBuffer 地址，用于填充 SDK 采集到的视频数据。
     * @param width 视频宽，此值由推流时设置的分辨率的宽决定
     * @param height 视频高，此值由推流时设置的分辨率的宽决定
     * @param strides 内存对齐宽度，即视频帧数据每一行字节数
     * @param byteBufferLens buffer大小，未解码视频帧数据只用到了VideoFrame#byteBuffers[0] 需要创建byteBufferLens[0]大小的内存，用于填充 SDK未解码数据。
     * @return Buffer 索引
     */
    @Override
    public int dequeueInputBuffer(int width, int height, int[] strides, int[] byteBufferLens) {
        boolean isBufferLensChange = false;
        for (int i=0; i<byteBufferLens.length;i++){
            if (byteBufferLens[i]>mMaxBufferSize[i]) {
                mMaxBufferSize[i] = byteBufferLens[i];
                isBufferLensChange = true;
            }
        }
        // buffer长度较原buffer长度有增长时，重新创建提供给SDK的视频帧buffer
        if (isBufferLensChange) {
            if (mMaxBufferSize[0]>0){
                mProduceQueue.clear();
            }
            // 创建一个视频帧buffer供SDK写视频数据
            createPixelBufferPool(mMaxBufferSize, 1);
        }

        // 为解码 AVCANNEXB 格式视频帧数据，提供分辨率
        if ((strides[0] == 0) && (byteBufferLens[0] > 0)) {
            mViewHeight = height;
            mViewWidth = width;
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

    /**
     * SDK 向 App 申请 VideoFrame 内存用于将采集的数据返回给 App 渲染。
     * @param index VideoFrame 索引，SDK 通过 {@link #dequeueInputBuffer(int, int, int[], int[])} 获得的索引值
     * @return App 给 SDK 分配的 VideoFrame 内存，SDK 拿到这块内存后，会将 index 对应的实际数据填充到这块内存中。
     */
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

        // 为videoFrame分配内存，指定其所需参数
        videoFrame.byteBuffers = buffers;
        videoFrame.height = this.height;
        videoFrame.width = this.width;
        videoFrame.strides = this.strides;

        return videoFrame;
    }

    private int printCount = 0;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.ms");

    /**
     * SDK 通知 App 拷贝数据，并返回流 ID 等数据信息。
     * SDK 通过此方法通知 App，对应索引的 VideoFrame 数据拷贝已完毕，并将流 ID 等信息返回。
     *
     * @param bufferIndex VideoFrame 索引，SDK 通过 {@link #dequeueInputBuffer(int, int, int[], int[])} 获得的索引值
     * @param streamID 流名 当外部渲染拉流数据时，streamID 为拉流流名；
     *                 当外部渲染推流数据，streamID 为 com.zego.zegoavkit2.ZegoConstants.ZegoVideoDataMainPublishingStream 常量时表示第一路推流数据，此常量值为空字符串；
     *                 streamID 为com.zego.zegoavkit2.ZegoConstants.ZegoVideoDataAuxPublishingStream 常量时表示第二路推流数据，此常量值为空格
     * @param videoPixelFormat 视频帧格式
     */
    @Override
    public void queueInputBuffer(int bufferIndex, String streamID, VideoPixelFormat videoPixelFormat) {
        if (bufferIndex == -1) {
            return;
        }
        if (printCount == 0) {
            Date date = new Date(System.currentTimeMillis());
            Log.d("Zego","encode data transfer time: "+simpleDateFormat.format(date));
            printCount++;
        }
        if (mProduceQueue.size() > bufferIndex) {
            // 根据 buffer 索引取出 SDK 填充的视频数据
            VideoRenderer.PixelBuffer pixelBuffer = mProduceQueue.get(bufferIndex);
            pixelBuffer.width = this.width;
            pixelBuffer.height = this.height;
            pixelBuffer.strides = this.strides;

            // 处理 AVCANNEXB 格式视频帧数据，进行解码
            if ((pixelBuffer.strides[0] == 0) && (pixelBuffer.buffer[0].capacity() > 0)) {
                byte[] tmpData = new byte[pixelBuffer.buffer[0].capacity()];
                pixelBuffer.buffer[0].position(0); // 缺少此行，解码后的渲染画面会卡住
                pixelBuffer.buffer[0].get(tmpData);

                // 系统启动到现在的纳秒数，包含休眠时间
                long now = 0;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    now = SystemClock.elapsedRealtimeNanos();
                } else {
                    now = TimeUnit.MILLISECONDS.toNanos(SystemClock.elapsedRealtime());
                }
                if (mAVCDecoder != null) {
                    // 为解码提供视频数据，时间戳
                    mAVCDecoder.inputFrameToDecoder(tmpData, now);
                }
            }

            ConcurrentLinkedQueue<PixelBuffer> concurrentLinkedQueue = mMapConsumeQueue.get(streamID);
            if (concurrentLinkedQueue == null) {
                concurrentLinkedQueue = new ConcurrentLinkedQueue<>();
                concurrentLinkedQueue.add(pixelBuffer);
                mMapConsumeQueue.put(streamID, concurrentLinkedQueue);
            } else {
                concurrentLinkedQueue.add(pixelBuffer);
            }

            // 修改生产者队列的待写buffer index
            mWriteIndex = (mWriteIndex + 1) % mProduceQueue.size();
        }
    }
}
