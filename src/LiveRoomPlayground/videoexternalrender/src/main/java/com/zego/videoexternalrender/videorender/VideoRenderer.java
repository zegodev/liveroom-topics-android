package com.zego.videoexternalrender.videorender;

import android.annotation.TargetApi;
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
import com.zego.zegoavkit2.videorender.IZegoExternalRenderCallback3;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * VideoRenderer
 * 渲染类 Renderer 的封装层，接口更利于上层调用
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class VideoRenderer implements Choreographer.FrameCallback, IZegoExternalRenderCallback3 {
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

    //  AVCANNEXB 模式解码器
    private AVCDecoder mAVCDecoder = null;

    private int mViewWidth = 540;
    private int mViewHeight = 960;

    private HandlerThread mThread = null;
    private Handler mHandler = null;

    public static class VideoFrameBuffer {
        public int[] byteBufferLens = {0, 0, 0, 0};
        // 给到ZEGO的视频帧数据，避免重复创建在这只创建1次
        private VideoFrame zgVideoFrame = new VideoFrame();
        private ArrayList<ByteBuffer[]> mProduceQueue = new ArrayList<>();
        private int mWriteIndex = 0;
        private int mWriteRemain = 0;
        public byte[] data;
    }

    /** 单帧视频数据
     *  包含视频画面的宽、高、数据、strides
     */
    static class PixelBuffer {
        public int width;
        public int height;
        public ByteBuffer[] buffer;
        public int[] strides;
    }

    private ConcurrentHashMap<String, VideoFrameBuffer> getFrameMap() {
        return frameMap;
    }
    private ConcurrentHashMap<String, VideoFrameBuffer> frameMap = new ConcurrentHashMap<>();

    // 流名、渲染对象的键值map
    private ConcurrentHashMap<String, Renderer> rendererMap = null;

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

        return 0;
    }

    private void checkNotNull() {
        synchronized (lock) {
            if (rendererMap == null) {
                rendererMap = new ConcurrentHashMap<>();
            }
        }
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
                checkNotNull();
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
                checkNotNull();
                if (rendererMap.get(streamID) != null) {
                    Log.i(TAG, String.format("removeView Renderer streamId : %s", streamID));
                    // 释放 EGL Surface
                    rendererMap.get(streamID).uninitEGLSurface();
                    // 释放 Render
                    rendererMap.get(streamID).uninit();
                    rendererMap.remove(streamID);
                }
                if (getFrameMap().get(streamID) != null) {
                    Log.i(TAG, String.format("removeView frameMap streamId : %s", streamID));
                    getFrameMap().remove(streamID);
                }
            }
        });
    }

    // 删除全部渲染视图
    public void removeAllView() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                checkNotNull();
                Log.i(TAG, "removeAllView");
                for (Map.Entry<String, Renderer> entry : rendererMap.entrySet()) {
                    Renderer renderer = entry.getValue();
                    // 释放 EGL Surface
                    renderer.uninitEGLSurface();
                    // 释放 Render
                    renderer.uninit();
                    rendererMap.remove(entry.getKey());
                }


                for (Map.Entry<String, VideoFrameBuffer> entry : getFrameMap().entrySet()) {
                    getFrameMap().remove(entry.getKey());
                }
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
        frameMap = null;

        // 移除视频帧回调监听
        Choreographer.getInstance().removeFrameCallback(VideoRenderer.this);

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

        for (Map.Entry<String, VideoFrameBuffer> entry : frameMap.entrySet()) {
            // 获取视频帧数据
            VideoFrameBuffer frameBuffer = entry.getValue();
            if (frameBuffer != null) {
                String streamID = entry.getKey();

                // 获取流名对应的渲染类对象
                Renderer renderer = rendererMap.get(streamID);
                PixelBuffer pixelBuffer = new PixelBuffer();
                pixelBuffer.buffer = frameBuffer.zgVideoFrame.byteBuffers;
                pixelBuffer.strides = frameBuffer.zgVideoFrame.strides;
                pixelBuffer.height = frameBuffer.zgVideoFrame.height;
                pixelBuffer.width = frameBuffer.zgVideoFrame.width;

                if (renderer != null) {
                    // 渲染类根据视频帧数据进行绘制
                    renderer.draw(pixelBuffer);
                }

                // 修改生产者队列的待写buffer index
                returnProducerPixelBuffer(frameBuffer);
            }
        }
    }

    // 创建count个视频帧buffer
    private void createPixelBufferPool(VideoFrameBuffer frameBuffer, int[] size, int count) {
        for (int i = 0; i < count; i++){
            ByteBuffer[] buffer = new ByteBuffer[3];
            buffer[0] = ByteBuffer.allocateDirect((int) (size[0] * 1.1));
            buffer[1] = ByteBuffer.allocateDirect((int) (size[1] * 1.1));
            buffer[2] = ByteBuffer.allocateDirect((int) (size[2] * 1.1));
            frameBuffer.mProduceQueue.add(buffer);
        }

        frameBuffer.mWriteRemain = count;
        frameBuffer.mWriteIndex = -1;
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
    public int dequeueInputBuffer(int width, int height, int[] strides, int[] byteBufferLens, String streamID) {
        boolean isBufferLensChange = false;
        VideoFrameBuffer videoFrameBuffer = getFrameMap().get(streamID);
        if (videoFrameBuffer == null) {
            videoFrameBuffer = new VideoFrameBuffer();
            getFrameMap().put(streamID, videoFrameBuffer);
        }

        for (int i = 0; i < byteBufferLens.length; i++) {
            if (byteBufferLens[i] > videoFrameBuffer.byteBufferLens[i]) {
                videoFrameBuffer.byteBufferLens[i] = byteBufferLens[i];
                isBufferLensChange = true;
            }
        }

        videoFrameBuffer.zgVideoFrame.height = height;
        videoFrameBuffer.zgVideoFrame.width = width;
        videoFrameBuffer.zgVideoFrame.strides = strides;

        // buffer长度较原buffer长度有增长时，重新创建提供给SDK的视频帧buffer
        if (isBufferLensChange) {
            createPixelBufferPool(videoFrameBuffer, byteBufferLens, 1);
        }

        // 为解码 AVCANNEXB 格式视频帧数据，提供分辨率
        if ((strides[0] == 0) && (byteBufferLens[0] > 0)) {
            mViewHeight = height;
            mViewWidth = width;
        }

        if (videoFrameBuffer.mWriteRemain == 0) {
            return -1;
        }

        if (videoFrameBuffer.mProduceQueue != null && videoFrameBuffer.mProduceQueue.size() == 0) {
            return -1;
        }

        videoFrameBuffer.mWriteRemain--;
        return (videoFrameBuffer.mWriteIndex + 1) % videoFrameBuffer.mProduceQueue.size();
    }

    /**
     * SDK 向 App 申请 VideoFrame 内存用于将采集的数据返回给 App 渲染。
     * @param index VideoFrame 索引，SDK 通过 {@link #dequeueInputBuffer(int, int, int[], int[], String)} 获得的索引值
     * @return App 给 SDK 分配的 VideoFrame 内存，SDK 拿到这块内存后，会将 index 对应的实际数据填充到这块内存中。
     */
    @Override
    public VideoFrame getInputBuffer(int index, String streamID) {

        if (getFrameMap().isEmpty()) {
            return null;
        }
        VideoFrameBuffer videoFrameBuffer = getFrameMap().get(streamID);

        if (videoFrameBuffer != null) {
            if (videoFrameBuffer.mProduceQueue.isEmpty()) {
                return null;
            }
            ByteBuffer[] byteBuffers = videoFrameBuffer.mProduceQueue.get(index);
            videoFrameBuffer.zgVideoFrame.byteBuffers = byteBuffers;
            return videoFrameBuffer.zgVideoFrame;
        }
        return null;
    }

    private int printCount = 0;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.ms");

    /**
     * SDK 通知 App 拷贝数据，并返回流 ID 等数据信息。
     * SDK 通过此方法通知 App，对应索引的 VideoFrame 数据拷贝已完毕，并将流 ID 等信息返回。
     *
     * @param bufferIndex VideoFrame 索引，SDK 通过 {@link #dequeueInputBuffer(int, int, int[], int[], String)} 获得的索引值
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
        VideoFrameBuffer videoFrameBuffer = getFrameMap().get(streamID);
        if (videoFrameBuffer != null && videoFrameBuffer.mProduceQueue.size() > bufferIndex) {
            ByteBuffer[] buffers = videoFrameBuffer.mProduceQueue.get(bufferIndex);

            //增加 width 与 strides 的判断
            if (videoFrameBuffer.zgVideoFrame.width > videoFrameBuffer.zgVideoFrame.strides[0]) {
                videoFrameBuffer.zgVideoFrame.width = videoFrameBuffer.zgVideoFrame.strides[0];
            }

            // 处理 AVCANNEXB 格式视频帧数据，进行解码
            if ((videoFrameBuffer.zgVideoFrame.strides[0] == 0) && (buffers[0].capacity() > 0)) {
                byte[] tmpData = new byte[buffers[0].capacity()];
                buffers[0].position(0); // 缺少此行，解码后的渲染画面会卡住
                buffers[0].get(tmpData);

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

            videoFrameBuffer.mWriteIndex = (videoFrameBuffer.mWriteIndex + 1) % videoFrameBuffer.mProduceQueue.size();

            returnProducerPixelBuffer(videoFrameBuffer);
        }
    }

    private synchronized void returnProducerPixelBuffer(VideoFrameBuffer videoFrameBuffer) {
        videoFrameBuffer.mWriteRemain++;
    }
}
