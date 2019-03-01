package com.zego.mediasideinfo;

import com.zego.zegoavkit2.mediaside.IZegoMediaSideCallback;
import com.zego.zegoavkit2.mediaside.ZegoMediaSideInfo;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class ZGMediaSideInfoDemo implements IZegoMediaSideCallback {
    static private ZGMediaSideInfoDemo zgMediaSideInfoDemo;

    private RecvMediaSideInfoCallback mediaSideInfoCallback = null;

    private ZegoMediaSideInfo zegoMediaSideInfo = new ZegoMediaSideInfo();

    private boolean mIsUseCustomPacket = false;

    private ArrayList<String> mSendMsgArr = new ArrayList<String>();
    private ArrayList<String> mRecvMsgArr = new ArrayList<String>();

    public static ZGMediaSideInfoDemo sharedInstance() {
        synchronized (ZGMediaSideInfoDemo.class) {
            if (zgMediaSideInfoDemo == null) {
                zgMediaSideInfoDemo = new ZGMediaSideInfoDemo();
            }
        }
        return zgMediaSideInfoDemo;
    }

    /**
     激活媒体次要信息通道

     @param onlyAudioPublish 是否只推音频流
     @param channelIndex 推流通道
     @discassion 在创建了 ZegoLiveRoomApi 对象之后，推流之前调用
     */
    public void activateMediaSideInfoForPublishChannel(boolean onlyAudioPublish, int channelIndex) {

        zegoMediaSideInfo.setMediaSideFlags(true, onlyAudioPublish, channelIndex);

        zegoMediaSideInfo.setZegoMediaSideCallback(this);
    }

    /**
     设置使用自定义包头

     @param useCutomPacket 是否使用自定义包头，true-自定义包头，false-内部包头
     @discussion 等同于 [-sendMediaSideInfo:data toPublishChannel:ZEGOAPI_CHN_MAIN]
     */
    public void setUseCutomPacket(boolean useCutomPacket) {
        mIsUseCustomPacket = useCutomPacket;
    }

    /**
     发送媒体次要信息

     @param content 待发送的媒体次要信息
     @discussion 等同于 [-sendMediaSideInfo:data toPublishChannel:ZEGOAPI_CHN_MAIN]
     */
    public void sendMediaSideInfo(String content) {

        if (content.getBytes().length > 1000) {
            return;
        }

        ByteBuffer inData = ByteBuffer.allocateDirect(content.getBytes().length);
        inData.put(content.getBytes(), 0,content.getBytes().length);
        inData.flip();

        zegoMediaSideInfo.sendMediaSideInfo(inData, content.getBytes().length, mIsUseCustomPacket,0);

        mSendMsgArr.add(content);
    }

    /**
     发送媒体次要信息

     @param content 待发送的媒体次要信息
     @param channelIndex 推流通道
     */
    public void sendMediaSideInfo(String content, int channelIndex) {
        if (mIsUseCustomPacket) {

            // 采用外部封装包头

            // * packet length: 4 bytes
            byte[] dataLen = intToBytesBig(content.getBytes().length+1);
            // * packet NAL type  [26, 31)  1 bytes
            byte[] packetType = {26};

            int firstLength = dataLen.length+packetType.length;  // 5 bytes
            int totalLength = firstLength + content.getBytes().length;

            if (totalLength > 1000) {
                return;
            }

            byte[] tmpData = new byte[totalLength];
            System.arraycopy(dataLen,0,tmpData,0,dataLen.length);
            System.arraycopy(packetType,0,tmpData,dataLen.length,packetType.length);
            System.arraycopy(content.getBytes(),0,tmpData,firstLength,content.getBytes().length);

            ByteBuffer inData = ByteBuffer.allocateDirect(totalLength);
            inData.put(tmpData,0,totalLength);
            inData.flip();

            zegoMediaSideInfo.sendMediaSideInfo(inData, totalLength, mIsUseCustomPacket, channelIndex);

        } else {

            if (content.getBytes().length > 1000) {
                return;
            }
            ByteBuffer inData = ByteBuffer.allocateDirect(content.getBytes().length);
            inData.put(content.getBytes(), 0,content.getBytes().length);
            inData.flip();

            zegoMediaSideInfo.sendMediaSideInfo(inData, content.getBytes().length, mIsUseCustomPacket, channelIndex);
        }

        mSendMsgArr.add(content);
    }

    /**
     设置媒体次要信息回调监听

     @param callback 媒体次要信息回调接口
     */
    public void setMediaSideInfoCallback(RecvMediaSideInfoCallback callback){
        mediaSideInfoCallback = callback;
    }


    public void unSetMediaSideInfoCallback(){
        mediaSideInfoCallback = null;
        zegoMediaSideInfo.setZegoMediaSideCallback(null);
    }

    /**
     媒体次要信息回调

     @param streamID 拉流流名
     @param inData   媒体次要信息
     @param dataLen  媒体次要信息长度
     */
    @Override
    public void onRecvMediaSideInfo(String streamID, ByteBuffer inData, int dataLen) {

        /* basic format
         * +--------+--------+--------+--------+----------------------+
         * |        |        |        |        |                      |
         * |             MediaType             |       DATA...        |
         * |        |     4 Bytes     |        |                      |
         * +--------+-----------------+--------+----------------------+
         */
//        int mediaType = inData.getInt();
        int mediaType = getIntFrom(inData, dataLen);

        if (1001 == mediaType){
            //SDK packet
            String mediaSideInfoStr = getStringFrom(inData, dataLen);

            if (null != mediaSideInfoCallback) {
                mediaSideInfoCallback.onRecvMediaSideInfo(streamID, mediaSideInfoStr);
            }
            mRecvMsgArr.add(mediaSideInfoStr);

        }else if (1002 == mediaType){
            //mix stream user data
            String mediaSideInfoStr = getStringFrom(inData, dataLen);

            if (null != mediaSideInfoCallback) {
                mediaSideInfoCallback.onRecvMixStreamUserData(streamID, mediaSideInfoStr);
            }

            mRecvMsgArr.add(mediaSideInfoStr);

        }else {
            //custom packet

            /* custom packet format
             * +--------+--------+--------+--------+--------+----------------------+
             * |        |        |        |        |        |                      |
             * |             MediaType             |NALTYPE |       DATA...        |
             * |        |     4 Bytes     |        | 1 Byte |                      |
             * +--------+-----------------+--------+--------+----------------------+
             */

            byte[] tmpData = new byte[dataLen-5];

            for (int i = 0; i < dataLen - 5; i++) {
                tmpData[i] = inData.get(i + 5);
            }

            String mediaSideInfoStr = new String(tmpData);

            if (null != mediaSideInfoCallback) {
                mediaSideInfoCallback.onRecvMediaSideInfo(streamID, mediaSideInfoStr);
            }
            mRecvMsgArr.add(mediaSideInfoStr);
        }
    }

    public String checkSendRecvMsgs() {
        if (mSendMsgArr.size() <= 0){
            return "";
        }
        if (mSendMsgArr.size() != mRecvMsgArr.size()) {
            return "Count not equal";
        }

        for (int i = 0; i < mRecvMsgArr.size(); i++) {

            if (!mRecvMsgArr.get(i).equals(mSendMsgArr.get(i))) {
                String showText = String.valueOf(i) + ", recv: " + mRecvMsgArr.get(i) + " -- send: " + mSendMsgArr.get(i);
                return showText;
            }
        }

        return "the same";
    }

    public interface RecvMediaSideInfoCallback {
        /**
         接收到媒体次要信息回调

         @param content 接收到的数据
         @param streamID 流ID，标记当前回调的信息所属媒体流
         */
        void onRecvMediaSideInfo(String streamID, String content);

        void onRecvMixStreamUserData(String streamID, String content);
    }

    /**
     * 以大端模式将int转成byte[]
     */
    public static byte[] intToBytesBig(int value) {
        byte[] src = new byte[4];

        src[0] = (byte) ((value >> 24) & 0xFF);
        src[1] = (byte) ((value >> 16) & 0xFF);
        src[2] = (byte) ((value >> 8) & 0xFF);
        src[3] = (byte) (value & 0xFF);
        return src;
    }

    public String getStringFrom(ByteBuffer byteBuffer, int dataLen) {
        if (dataLen == 0) {
            return "";
        }

        byte[] temp = new byte[dataLen - 4];
        for (int i = 0; i < dataLen - 4; i++) {
            temp[i] = byteBuffer.get(i + 4);
        }

        return new String(temp);
    }

    // 获取mediaType类型值
    public int getIntFrom(ByteBuffer byteBuffer, int dataLen) {

        if (dataLen == 0) {
            return -1;
        }

        int result = (byteBuffer.get(0) & 0xFF) << 24 | (byteBuffer.get(1) & 0xFF) << 16 | (byteBuffer.get(2) & 0xFF) << 8 | (byteBuffer.get(3) & 0xFF);

        return result;
    }
}
