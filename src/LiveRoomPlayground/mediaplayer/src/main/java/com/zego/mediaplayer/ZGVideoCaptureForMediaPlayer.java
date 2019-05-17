package com.zego.mediaplayer;

import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;

import com.zego.common.ZGManager;
import com.zego.zegoavkit2.ZegoVideoCaptureDevice;
import com.zego.zegoavkit2.ZegoVideoCaptureFactory;
import com.zego.zegoavkit2.ZegoVideoDataFormat;
import com.zego.zegoliveroom.ZegoLiveRoom;
import com.zego.zegoliveroom.constants.ZegoVideoViewMode;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 外部采集工厂, 需要通过 {@link ZegoLiveRoom#setVideoCaptureFactory(Object)}设置到sdk
 * ZGVideoCaptureForMediaPlayer外部采集的入口，定义了创建、销毁 ZegoVideoCaptureDevice 的接口，
 * 向 SDK 提供管理 ZegoVideoCaptureDevice 生命周期的能力。需要调用
 * <strong>
 * 外部采集工厂设置到Zego SDK的时机必须是在 {@link ZegoLiveRoom#initSDK(long, byte[], Context)} 之前。
 * 否则外部采集功能将不生效
 * <p>
 * </strong>
 */
public class ZGVideoCaptureForMediaPlayer extends ZegoVideoCaptureFactory {

    private ZGMediaPlayerVideoCapture mDevice;

    public ZGMediaPlayerVideoCapture getmDevice() {
        return mDevice;
    }

    public ZGVideoCaptureForMediaPlayer(ZGMediaPlayerVideoCapture device) {
        mDevice = device;
    }

    @Override
    protected ZegoVideoCaptureDevice create(String s) {

        return mDevice;
    }

    @Override
    protected void destroy(ZegoVideoCaptureDevice zegoVideoCaptureDevice) {
        mDevice = null;
    }

    /**
     * ZGMediaPlayerVideoCapture 定义了基本的组件能力
     * 方便管理资源的生命周期
     */
    static class ZGMediaPlayerVideoCapture extends ZegoVideoCaptureDevice implements ZGMediaPlayerDemo.MediaPlayerVideoDataCallback {
        private static final String TAG = "VideoRendererCapture";


        // 因为生命周期回调和mediaPlayer回调是不同线程, 使用原子类
        AtomicBoolean runState = new AtomicBoolean(false);

        private Client mClient = null;

        /**
         * 开发者在 allocateAndStart 中获取到 client
         *
         * @param client 用于通知 SDK 采集结果, 包括设置采集的view模式
         */
        @Override
        protected void allocateAndStart(Client client) {
            Log.e("videoCaptureFrom", "allocateAndStart");
            mClient = client;
            mClient.setFillMode(ZegoVideoViewMode.ScaleToFill);

        }

        /**
         * 用于释放销毁一些资源
         */
        @Override
        protected void stopAndDeAllocate() {
            Log.e("video", "stopAndDeAllocate");
            mClient.destroy();
            mClient = null;
        }

        /**
         * 启用采集，sdk开始推流或开始预览的时候会调用该
         * 方法通知到开发者
         *
         * @return
         */
        @Override
        protected int startCapture() {
            Log.e("video", "startCapture");
            // sdk开始采集
            runState.set(true);
            return 0;
        }


        /**
         * 停止采集，sdk停止推流或停止预览时会调用该方法
         * 通知到开发者
         *
         * @return
         */
        @Override
        protected int stopCapture() {
            Log.e("video", "stopCapture");
            // sdk停止采集, 此时需要修改状态
            runState.set(false);
            return 0;
        }

        @Override
        protected int supportBufferType() {
            // sdk外部采集的方式, 使用内存模式
            return PIXEL_BUFFER_TYPE_MEM;
        }

        private class PixelBuffer {
            public ByteBuffer buffer;
            public VideoCaptureFormat format;
        }

        volatile PixelBuffer pixelBuffer = null;

        private int mMaxBufferSize = 0;

        // mediaPlayer视频帧回调处理
        @Override
        public void onPlayVideoData(byte[] bytes, int size, ZegoVideoDataFormat f) {
            // 获取buffer, 重复利用一个buff
            PixelBuffer pixelBuffer = getPixelBuffer(size, f.width, f.height);
            pixelBuffer.buffer.clear();
            pixelBuffer.buffer.put(bytes, 0, size);
            pixelBuffer.format.width = f.width;
            pixelBuffer.format.height = f.height;
            pixelBuffer.format.strides = f.strides;
            pixelBuffer.format.rotation = 0;
            pixelBuffer.format.pixel_format = PIXEL_FORMAT_RGBA32;

            if (runState.get()) {
                long now = 0;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    now = SystemClock.elapsedRealtimeNanos();
                } else {
                    now = TimeUnit.MILLISECONDS.toNanos(SystemClock.elapsedRealtime());
                }
                // 视频帧传给ZEGO sdk
                mClient.onByteBufferFrameCaptured(pixelBuffer.buffer, size, pixelBuffer.format, now, 1000000000);
            }

        }

        private synchronized PixelBuffer getPixelBuffer(int size, int width, int height) {
            // buff大小变化才进行创建
            if (pixelBuffer == null || size != mMaxBufferSize) {
                if (pixelBuffer != null) {
                    pixelBuffer.buffer.clear();
                } else {
                    pixelBuffer = new PixelBuffer();
                    pixelBuffer.format = new VideoCaptureFormat();
                }
                // 设置编解码分辨率
                ZGManager.sharedInstance().setZegoAvConfig(width, height);
                // TODO 创建buff要用allocateDirect方式创建, sdk才能正常收到数据
                pixelBuffer.buffer = ByteBuffer.allocateDirect(size);
                mMaxBufferSize = size;
            }
            return pixelBuffer;
        }


    /* -------------以下是目前不需要关注的回调------------- */

        @Override
        protected int setFrameRate(int i) {
            return 0;
        }

        @Override
        protected int setResolution(int i, int i1) {
            return 0;
        }

        @Override
        protected int setFrontCam(int i) {
            return 0;
        }

        @Override
        protected int setView(View view) {
            return 0;
        }

        @Override
        protected int setViewMode(int i) {
            return 0;
        }

        @Override
        protected int setViewRotation(int i) {
            return 0;
        }

        @Override
        protected int setCaptureRotation(int i) {
            return 0;
        }

        @Override
        protected int startPreview() {
            return 0;
        }

        @Override
        protected int stopPreview() {
            return 0;
        }

        @Override
        protected int enableTorch(boolean b) {
            return 0;
        }

        @Override
        protected int takeSnapshot() {
            return 0;
        }

        @Override
        protected int setPowerlineFreq(int i) {
            return 0;
        }


    }


}
