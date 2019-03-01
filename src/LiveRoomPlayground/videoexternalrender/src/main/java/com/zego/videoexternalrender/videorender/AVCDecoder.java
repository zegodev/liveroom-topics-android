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

@TargetApi(23)
public class AVCDecoder {

    private final static String TAG = "Zego";
    private final static int CONFIGURE_FLAG_DECODE = 0;

    private MediaCodec  mMediaCodec;
    private MediaFormat mMediaFormat;
    private Surface     mSurface;
    private int         mViewWidth;
    private int         mViewHeight;
    
    static class DecodeInfo {
        public long timeStmp;
        public byte[] inOutData;
    }

    private final static ConcurrentLinkedQueue<DecodeInfo> mInputDatasQueue = new ConcurrentLinkedQueue<DecodeInfo>();
//    private final static ConcurrentLinkedQueue<DecodeInfo> mOutputDatasQueue = new ConcurrentLinkedQueue<DecodeInfo>();

    private MediaCodec.Callback mCallback = new MediaCodec.Callback() {
        @Override
        public void onInputBufferAvailable(@NonNull MediaCodec mediaCodec, int inputBufferId) {
            try {
                ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferId);
                inputBuffer.clear();

                DecodeInfo decodeInfo = mInputDatasQueue.poll();

                if (decodeInfo != null) {
                    inputBuffer.put(decodeInfo.inOutData, 0, decodeInfo.inOutData.length);
                    mediaCodec.queueInputBuffer(inputBufferId, 0, decodeInfo.inOutData.length, decodeInfo.timeStmp * 1000, 0);
                } else {
                    long now = 0;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        now = SystemClock.elapsedRealtimeNanos();
                    } else {
                        now = TimeUnit.MILLISECONDS.toNanos(SystemClock.elapsedRealtime());
                    }
                    mediaCodec.queueInputBuffer(inputBufferId, 0, 0, now * 1000, 0);
                }
            } catch (IllegalStateException exception) {
                Log.d(TAG, "encoder mediaCodec input exception: " + exception.getMessage());
            }
        }

        @Override
        public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int id, @NonNull MediaCodec.BufferInfo bufferInfo) {

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

    public AVCDecoder(Surface surface, int viewwidth, int viewheight){
        try {
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

        mMediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, mViewWidth, mViewHeight);
    }

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
    public void startDecoder(){
        if(mMediaCodec != null && mSurface != null){
            mMediaCodec.setCallback(mCallback);
            mMediaCodec.configure(mMediaFormat, mSurface,null,CONFIGURE_FLAG_DECODE);
            mMediaCodec.start();
        }else{
            throw new IllegalArgumentException("startDecoder failed, please check the MediaCodec is init correct");
        }
    }

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

