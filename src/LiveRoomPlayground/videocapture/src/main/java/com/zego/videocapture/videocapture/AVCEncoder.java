package com.zego.videocapture.videocapture;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

@TargetApi(23)
public class AVCEncoder {

    private final static String TAG = "Zego";
    private final static int CONFIGURE_FLAG_ENCODE = MediaCodec.CONFIGURE_FLAG_ENCODE;

    private MediaCodec mMediaCodec;
    private MediaFormat mMediaFormat;
    private int         mViewWidth;
    private int         mViewHeight;

    private ByteBuffer configData = ByteBuffer.allocateDirect(1);

    static class TransferInfo {
        public long timeStmp;
        public byte[] inOutData;
        public boolean isKeyFrame;
    }

    private final static ConcurrentLinkedQueue<TransferInfo> mInputDatasQueue = new ConcurrentLinkedQueue<TransferInfo>();
    private final static ConcurrentLinkedQueue<TransferInfo> mOutputDatasQueue = new ConcurrentLinkedQueue<TransferInfo>();

    private MediaCodec.Callback mCallback = new MediaCodec.Callback() {
        @Override
        public void onInputBufferAvailable(@NonNull MediaCodec mediaCodec, int inputBufferId) {
            try {
                ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferId);
                inputBuffer.clear();

                TransferInfo transferInfo = mInputDatasQueue.poll();

                if(transferInfo != null) {
                    inputBuffer.put(transferInfo.inOutData, 0, transferInfo.inOutData.length);
                    mediaCodec.queueInputBuffer(inputBufferId,0, transferInfo.inOutData.length,transferInfo.timeStmp*1000,0);
                } else {
                    long now = 0;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        now = SystemClock.elapsedRealtimeNanos();
                    } else {
                        now = TimeUnit.MILLISECONDS.toNanos(SystemClock.elapsedRealtime());
                    }
                    mediaCodec.queueInputBuffer(inputBufferId,0, 0,now*1000,0);
                }
            } catch (IllegalStateException exception) {
                Log.e(TAG,"encoder mediaCodec input exception: "+exception.getMessage());
            }
        }

        @Override
        public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int outputBufferId, @NonNull MediaCodec.BufferInfo bufferInfo) {
            ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(outputBufferId);
            MediaFormat outputFormat = mMediaCodec.getOutputFormat(outputBufferId);

            ByteBuffer keyFrameBuffer;

            if(outputBuffer != null && bufferInfo.size > 0){
                TransferInfo transferInfo = new TransferInfo();
                transferInfo.timeStmp = bufferInfo.presentationTimeUs/1000;

                boolean isConfigFrame = (bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0;
                if (isConfigFrame) {
                    Log.d(TAG, "Config frame generated. Offset: " + bufferInfo.offset +
                            ", Size: " + bufferInfo.size + ", num: "+(bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG));

                    outputBuffer.position(bufferInfo.offset);
                    outputBuffer.limit(bufferInfo.offset+bufferInfo.size);
                    if (configData.capacity() < bufferInfo.size) {
                        configData = ByteBuffer.allocateDirect(bufferInfo.size);
                    }
                    configData.put(outputBuffer);
                }

                boolean isKeyFrame = (bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0;
                if (isKeyFrame){

                    Log.d(TAG, "Appending config frame of size " + configData.capacity() +
                            " to output buffer with offset " + bufferInfo.offset + ", size " +
                            bufferInfo.size);
                            // For H.264 key frame append SPS and PPS NALs at the start
                    keyFrameBuffer = ByteBuffer.allocateDirect(
                            configData.capacity() + bufferInfo.size);
                    configData.rewind();
                    keyFrameBuffer.put(configData);
                    keyFrameBuffer.put(outputBuffer);
                    keyFrameBuffer.position(0);

                    byte[] buffer = new byte[keyFrameBuffer.remaining()];
                    keyFrameBuffer.get(buffer);

                    transferInfo.inOutData = buffer;
                    transferInfo.isKeyFrame = true;

                } else {

                    byte [] buffer = new byte[outputBuffer.remaining()];
                    outputBuffer.get(buffer);

                    transferInfo.inOutData = buffer;
                    transferInfo.isKeyFrame = false;
                }

                boolean result = mOutputDatasQueue.offer(transferInfo);

                if(!result){
                    Log.e(TAG, "encoder offer to queue failed, queue in full state");
                }
            }
            mMediaCodec.releaseOutputBuffer(outputBufferId, false);
        }

        @Override
        public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {
            Log.e(TAG, "encoder onError");
        }

        @Override
        public void onOutputFormatChanged(@NonNull MediaCodec mediaCodec, @NonNull MediaFormat mediaFormat) {
            Log.d(TAG, "encoder onOutputFormatChanged, mediaFormat: "+mediaFormat);
        }
    };

    private static MediaCodecInfo selectCodec(String mimeType) {

        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {
                continue;
            }

            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    Log.d(TAG, "selectCodec OK, get "+mimeType);
                    return codecInfo;
                }
            }
        }
        return null;
    }

    public static boolean isSupportI420() {
        boolean isSupport = false;
        int colorFormat = 0;
        MediaCodecInfo codecInfo = selectCodec("video/avc");
        if (codecInfo != null){
            MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType("video/avc");
            for (int i = 0; i < capabilities.colorFormats.length && colorFormat == 0; i++) {
                int format = capabilities.colorFormats[i];
                switch (format) {
                    //support color formats
                    case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:    /*I420 --- YUV4:2:0 --- Nvidia Tegra 3, Samsu */
                        colorFormat = format;
                        break;
                    default:
                        Log.d("Zego", " AVCEncoder unsupported color format " + format);
                        break;
                }
            }
            if (colorFormat != 0) {
                isSupport = true;
            } else {
                isSupport = false;
            }
        }

        return isSupport;
    }

    public AVCEncoder(int viewwidth, int viewheight){

        try {
            mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            mMediaCodec = null;
            return;
        }

        this.mViewWidth = viewwidth;
        this.mViewHeight = viewheight;

        mMediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, mViewWidth, mViewHeight);
        mMediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar); //COLOR_FormatYUV420Planar COLOR_FormatYUV420PackedSemiPlanar
        mMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 4000000);
        mMediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
        mMediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
//            mMediaFormat.setInteger(MediaFormat.KEY_ROTATION, 90);
    }

    /**
     * Input Video stream which need encode to Queue
     * @param needEncodeData I420 format stream
     */
    public void inputFrameToEncoder(byte[] needEncodeData, long timeStmp){

        if (needEncodeData != null) {
            TransferInfo transferInfo = new TransferInfo();
            transferInfo.inOutData = needEncodeData;
            transferInfo.timeStmp = timeStmp;
            boolean inputResult = mInputDatasQueue.offer(transferInfo);
            if (!inputResult) {
                Log.d(TAG, "inputEncoder queue result = " + inputResult + " queue current size = " + mInputDatasQueue.size());
            }
        }
    }

    /**
     * Get Encoded frame from queue
     * @return a encoded frame; it would be null when the queue is empty.
     */
    public TransferInfo pollFrameFromEncoder(){
        return mOutputDatasQueue.poll();
    }

    /**
     * start the MediaCodec to encode video data
     */
    public void startEncoder(){
        if(mMediaCodec != null){
            mMediaCodec.setCallback(mCallback);
            mMediaCodec.configure(mMediaFormat, null, null, CONFIGURE_FLAG_ENCODE);
            mMediaCodec.start();
        }else{
            throw new IllegalArgumentException("startEncoder failed,is the MediaCodec has been init correct?");
        }
    }

    /**
     * stop encode the video data
     */
    public void stopEncoder(){
        if(mMediaCodec != null){
            mMediaCodec.stop();
        }
    }

    /**
     * release all resource that used in Encoder
     */
    public void releaseEncoder(){
        if(mMediaCodec != null){
            mInputDatasQueue.clear();
            mOutputDatasQueue.clear();
            mMediaCodec.release();
            mMediaCodec = null;
        }
    }
}
