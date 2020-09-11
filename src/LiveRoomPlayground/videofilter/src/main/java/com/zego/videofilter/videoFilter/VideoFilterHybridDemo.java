package com.zego.videofilter.videoFilter;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.zego.videofilter.faceunity.FURenderer;
import com.zego.videofilter.videoFilter.ve_gl.EglBase;
import com.zego.videofilter.videoFilter.ve_gl.GlUtil;
import com.zego.zegoavkit2.videofilter.ZegoVideoFilter;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

/**
 * 外部滤镜采用 BUFFER_TYPE_HYBRID_MEM_GL_TEXTURE_2D（异步传递 texture2d）方式传递数据给 SDK。
 *
 * Created by robotding on 17/2/9.
 */
public class VideoFilterHybridDemo extends ZegoVideoFilter {

    // faceunity 美颜处理类
    private FURenderer mFURenderer;

    // SDK 内部实现 ZegoVideoFilter.Client 协议的对象
    private ZegoVideoFilter.Client mClient = null;

    private HandlerThread mThread = null;
    private volatile Handler mHandler = null;

    // 图像数据信息
    static class PixelBuffer {
        public int width;
        public int height;
        public int stride;
        public long timestamp_100n;
        public ByteBuffer buffer;
    }
    private ArrayList<PixelBuffer> mProduceQueue = new ArrayList<PixelBuffer>();
    private int mWriteIndex = 0;
    private int mWriteRemain = 0;
    private ConcurrentLinkedQueue<PixelBuffer> mConsumeQueue = new ConcurrentLinkedQueue<PixelBuffer>();
    private int mMaxBufferSize = 0;

    private EglBase captureEglBase;
    private int mTextureId = 0;

    public VideoFilterHybridDemo(FURenderer fuRenderer) {
        this.mFURenderer = fuRenderer;
    }

    /**
     * 初始化资源，比如图像绘制（openGL）、美颜组件等
     * @param client SDK 内部实现 ZegoVideoFilter.Client 协议的对象
     *
     * 注意：client 必须保存为强引用对象，在 stopAndDeAllocate 被调用前必须一直被保存。
     *      SDK 不负责管理 client 的生命周期。
     */
    @Override
    protected void allocateAndStart(Client client) {
        mClient = client;
        mThread = new HandlerThread("video-filter");
        mThread.start();
        mHandler = new Handler(mThread.getLooper());

        final CountDownLatch barrier = new CountDownLatch(1);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                captureEglBase = EglBase.create(null, EglBase.CONFIG_PIXEL_BUFFER);
                try {
                    captureEglBase.createDummyPbufferSurface();
                    captureEglBase.makeCurrent();
                } catch (RuntimeException e) {
                    // Clean up before rethrowing the exception.
                    captureEglBase.releaseSurface();
                    throw e;
                }

                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                mTextureId = GlUtil.generateTexture(GLES20.GL_TEXTURE_2D);

                // 创建及初始化 faceunity 相应的资源
                mFURenderer.onSurfaceCreated();

                barrier.countDown();
            }
        });
        try {
            barrier.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mProduceQueue.clear();
        mConsumeQueue.clear();
        mWriteIndex = 0;
        mWriteRemain = 0;
        mMaxBufferSize = 0;
    }

    /**
     * 释放资源
     * 注意：必须调用 client 的 destroy 方法，否则会造成内存泄漏。
     */
    @Override
    protected void stopAndDeAllocate() {

        final CountDownLatch barrier = new CountDownLatch(1);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                // 建议在同步停止滤镜任务后再清理 client 对象，保证 SDK 调用 stopAndDeAllocate 后，没有残留的异步任务导致野指针 crash
                mClient.destroy();
                mClient = null;

                // 销毁 faceunity 相关的资源
                mFURenderer.onSurfaceDestroyed();

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

        mThread.quit();
        mThread = null;
    }

    /**
     * 指定滤镜的传递数据类型，异步传递 texture2d
     * SDK 需要根据 supportBufferType 返回的类型值创建不同的 client 对象。
     * @return
     */
    @Override
    protected int supportBufferType() {
        return BUFFER_TYPE_HYBRID_MEM_GL_TEXTURE_2D;
    }

    /**
     * SDK 通知外部滤镜当前采集图像的宽高并请求内存池下标
     * @param width 采集图像宽
     * @param height 采集图像高
     * @param stride
     * @return
     */
    @Override
    protected synchronized int dequeueInputBuffer(int width, int height, int stride) {
        if (stride * height > mMaxBufferSize) {
            if (mMaxBufferSize != 0) {
                mProduceQueue.clear();
            }

            mMaxBufferSize = stride * height;
            createPixelBufferPool(3);
        }

        if (mWriteRemain == 0) {
            return -1;
        }

        mWriteRemain--;
        return (mWriteIndex + 1) % mProduceQueue.size();
    }

    /**
     * SDK 请求外部滤镜返回对应内存池下标的 ByteBuffer（ByteBuffer.allocateDirect方式分配的）
     * @param index dequeueInputBuffer 返回的内存池下标即 SDK 获得的内存池下标
     * @return
     */
    @Override
    protected synchronized ByteBuffer getInputBuffer(int index) {
        if (mProduceQueue.isEmpty()) {
            return null;
        }
        ByteBuffer buffer = mProduceQueue.get(index).buffer;
        buffer.position(0);
        return buffer;
    }

    private byte[] mModiBuffer = new byte[0];
    /**
     * SDK 抛出图像数据，外部滤镜进行处理
     *
     * @param bufferIndex
     * @param width
     * @param height
     * @param stride
     * @param timestamp_100n
     */
    @Override
    protected synchronized void queueInputBuffer(int bufferIndex, final int width, final int height, int stride, long timestamp_100n) {
        if (bufferIndex == -1) {
            return ;
        }

        // 根据 SDK 返回的 bufferIndex 索引取相应的图像数据
        PixelBuffer pixelBuffer = mProduceQueue.get(bufferIndex);
        pixelBuffer.width = width;
        pixelBuffer.height = height;
        pixelBuffer.stride = stride;
        pixelBuffer.timestamp_100n = timestamp_100n;
        pixelBuffer.buffer.limit(height * stride);
        mConsumeQueue.add(pixelBuffer);
        mWriteIndex++;

        // 切换线程，异步进行前处理
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                // 取出图像数据
                PixelBuffer pixelBuffer = getConsumerPixelBuffer();

                if (pixelBuffer == null) {
                    return;
                }

                long start = System.currentTimeMillis();

                captureEglBase.makeCurrent();

                // 创建一个纹理 ID
                GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
                // 绑定纹理 ID
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);

                pixelBuffer.buffer.position(0);
                // 上传贴图
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, pixelBuffer.width, pixelBuffer.height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelBuffer.buffer);

                if (pixelBuffer.buffer.limit() > mModiBuffer.length) {
                    mModiBuffer = null;
                    mModiBuffer = new byte[pixelBuffer.buffer.limit()];
                }

                pixelBuffer.buffer.position(0);
                pixelBuffer.buffer.get(mModiBuffer, 0, pixelBuffer.buffer.limit());
                // 调用 faceunity 进行美颜
//                int textureID = mFURenderer.onDrawFrame(mTextureId, pixelBuffer.width, pixelBuffer.height);
                int textureID = mFURenderer.onDrawFrame(mModiBuffer, mTextureId, pixelBuffer.width, pixelBuffer.height);

                // 此步骤会使用此 textureID 用做本地预览的视图渲染，并将美颜后的 textureID 传给 SDK（拉该条流时是美颜后的视频）
                mClient.onProcessCallback(textureID, pixelBuffer.width, pixelBuffer.height, pixelBuffer.timestamp_100n);

                captureEglBase.detachCurrent();

                long end = System.currentTimeMillis();

                Log.i("Hybrid", "time:" + (end - start));

                returnProducerPixelBuffer(pixelBuffer);
            }
        });
    }

    @Override
    protected SurfaceTexture getSurfaceTexture() {
        return null;
    }

    @Override
    protected void onProcessCallback(int textureId, int width, int height, long timestamp_100n) {

    }

    // 创建存放图像数据的 buffer
    private void createPixelBufferPool(int count) {
        for (int i = 0; i < count; i++) {
            PixelBuffer pixelBuffer = new PixelBuffer();
            pixelBuffer.buffer = ByteBuffer.allocateDirect(mMaxBufferSize);
            mProduceQueue.add(pixelBuffer);
        }

        mWriteRemain = count;
        mWriteIndex = -1;
    }

    private PixelBuffer getConsumerPixelBuffer() {
        if (mConsumeQueue.isEmpty()) {
            return null;
        }
        return mConsumeQueue.poll();
    }

    private synchronized void returnProducerPixelBuffer(PixelBuffer pixelBuffer) {
        if (pixelBuffer.buffer.capacity() == mMaxBufferSize) {
            mWriteRemain++;
        }
    }

    // 销毁 openGL
    private void release() {
        if (captureEglBase.hasSurface()) {
            captureEglBase.makeCurrent();
            if (mTextureId != 0) {
                int[] textures = new int[] {mTextureId};
                GLES20.glDeleteTextures(1, textures, 0);
                mTextureId = 0;
            }
        }

        captureEglBase.release();
        captureEglBase = null;
    }

}
