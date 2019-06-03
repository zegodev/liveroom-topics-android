package com.zego.videoexternalrender.videorender;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * AVCDecoder
 *
 * AVCANNEXB 模式解码器
 * 此类的作用是解码 SDK 抛出的未解码视频数据
 * 开发者可参考该类的代码实现接收 SDK 抛出的未解码数据并渲染
 *
 */
@TargetApi(23)
public class AVCDecoder {

    private final static String TAG = "Zego";
    private final static int CONFIGURE_FLAG_DECODE = 0;

    // 音视频编解码器组件
    private MediaCodec  mMediaCodec;
    // 媒体数据格式信息
    private MediaFormat mMediaFormat;
    private Surface     mSurface;
    // 渲染展示视图宽
    private int         mViewWidth;
    // 渲染展示视图高
    private int         mViewHeight;

    /** 待解码数据信息
     *  包含时间戳和待解码的数据
     */
    static class DecodeInfo {
        public long timeStmp; // 纳秒
        public byte[] inOutData;
    }

    private final static ConcurrentLinkedQueue<DecodeInfo> mInputDatasQueue = new ConcurrentLinkedQueue<DecodeInfo>();
//    private final static ConcurrentLinkedQueue<DecodeInfo> mOutputDatasQueue = new ConcurrentLinkedQueue<DecodeInfo>();

    // 解码器回调
    private MediaCodec.Callback mCallback = new MediaCodec.Callback() {
        // 输入缓冲区回调，等待输入
        @Override
        public void onInputBufferAvailable(@NonNull MediaCodec mediaCodec, int inputBufferId) {
            try {
                // 获取MediaCodec的输入缓冲区buffer地址
                ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferId);
                inputBuffer.clear();

                // 从视频帧数据队列取数据
                DecodeInfo decodeInfo = mInputDatasQueue.poll();

                if (decodeInfo != null) {
                    // 将视频帧数据写入MediaCodec的buffer中
                    inputBuffer.put(decodeInfo.inOutData, 0, decodeInfo.inOutData.length);
                    // 视频帧数据入MediaCodec队列，等待解码，需要传递线性递增的时间戳
                    mediaCodec.queueInputBuffer(inputBufferId, 0, decodeInfo.inOutData.length, decodeInfo.timeStmp * 1000, 0);
                } else {
                    long now = 0;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        now = SystemClock.elapsedRealtimeNanos();
                    } else {
                        now = TimeUnit.MILLISECONDS.toNanos(SystemClock.elapsedRealtime());
                    }
                    // 入空数据进MediaCodec队列
                    mediaCodec.queueInputBuffer(inputBufferId, 0, 0, now * 1000, 0);
                }
            } catch (IllegalStateException exception) {
                Log.d(TAG, "encoder mediaCodec input exception: " + exception.getMessage());
            }
        }

        /**
         * 解码完成回调
         * 渲染由MediaCodec解码完成后进行，所以此回调中不处理解码后的数据
         */
        @Override
        public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int id, @NonNull MediaCodec.BufferInfo bufferInfo) {

            // 根据buffer索引获取MediaCodec的输出缓冲区buffer地址
            ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(id);
            MediaFormat outputFormat = mMediaCodec.getOutputFormat(id);
            int width = outputFormat.getInteger("width");
            int height = outputFormat.getInteger("height");
//            Log.d(TAG, "decoder OutputBuffer, width: "+width+", height: "+height);
            if(mMediaFormat == outputFormat && outputBuffer != null && bufferInfo.size > 0){
                byte [] buffer = new byte[outputBuffer.remaining()];
                outputBuffer.get(buffer);
            }

            boolean doRender = (bufferInfo.size != 0);
            // 处理完成，释放ByteBuffer数据
            mMediaCodec.releaseOutputBuffer(id, doRender);
        }

        @Override
        public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {
            Log.d(TAG, "decoder onError");
        }

        @Override
        public void onOutputFormatChanged(@NonNull MediaCodec mediaCodec, @NonNull MediaFormat mediaFormat) {
            Log.d(TAG, "decoder onOutputFormatChanged");
        }
    };

    /**
     * 初始化解码器
     * @param surface  显示解码内容的surface
     * @param viewwidth 渲染展示视图的宽
     * @param viewheight 渲染展示视图的高
     */
    public AVCDecoder(Surface surface, int viewwidth, int viewheight){
        try {
            // 选用MIME类型为AVC、解码器来构造MediaCodec
            mMediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            mMediaCodec = null;
            return;
        }

        if(surface == null){
            return;
        }

        this.mViewWidth  = viewwidth;
        this.mViewHeight = viewheight;
        this.mSurface = surface;

        // 设置解码器的MediaFormat
        mMediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, mViewWidth, mViewHeight);
    }

    // 为解码器提供视频帧数据
    public void inputFrameToDecoder(byte[] needEncodeData, long timeStmp){
        if (needEncodeData != null) {
            DecodeInfo decodeInfo = new DecodeInfo();
            decodeInfo.inOutData = needEncodeData;
            decodeInfo.timeStmp = timeStmp;
            boolean inputResult = mInputDatasQueue.offer(decodeInfo);
            if (!inputResult) {
                Log.i(TAG, "decoder inputDecoder queue result = " + inputResult + " queue current size = " + mInputDatasQueue.size());
            }
        }
    }

    // 启动解码器
    public void startDecoder(){
        if(mMediaCodec != null && mSurface != null){
            // 设置解码器的回调监听
            mMediaCodec.setCallback(mCallback);
            // 配置MediaCodec，选择采用解码器功能
            mMediaCodec.configure(mMediaFormat, mSurface,null,CONFIGURE_FLAG_DECODE);
            // 启动解码器
            mMediaCodec.start();
        }else{
            throw new IllegalArgumentException("startDecoder failed, please check the MediaCodec is init correct");
        }
    }

    // 释放解码器
    public void stopAndReleaseDecoder(){
        if(mMediaCodec != null){
            try {
                mMediaCodec.stop();
                mMediaCodec.release();
                mInputDatasQueue.clear();
//            mOutputDatasQueue.clear();
                mMediaCodec = null;
            } catch (IllegalStateException e) {
                Log.d(TAG,"MediaCodec decoder stop exception: "+e.getMessage());
            }

        }
    }
}

