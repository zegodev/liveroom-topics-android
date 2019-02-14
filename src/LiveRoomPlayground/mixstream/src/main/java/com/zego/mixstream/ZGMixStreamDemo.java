package com.zego.mixstream;

import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zego.common.ZGManager;
import com.zego.zegoavkit2.mixstream.IZegoMixStreamExCallback;
import com.zego.zegoavkit2.mixstream.IZegoSoundLevelInMixStreamCallback;
import com.zego.zegoavkit2.mixstream.ZegoMixStreamConfig;
import com.zego.zegoavkit2.mixstream.ZegoMixStreamInfo;
import com.zego.zegoavkit2.mixstream.ZegoMixStreamOutput;
import com.zego.zegoavkit2.mixstream.ZegoMixStreamOutputResult;
import com.zego.zegoavkit2.mixstream.ZegoMixStreamResultEx;
import com.zego.zegoavkit2.mixstream.ZegoMixStreamWatermark;
import com.zego.zegoavkit2.mixstream.ZegoSoundLevelInMixStreamInfo;
import com.zego.zegoavkit2.mixstream.ZegoStreamMixer;
import com.zego.zegoliveroom.constants.ZegoAvConfig;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ZGMixStreamDemo implements IZegoMixStreamExCallback, IZegoSoundLevelInMixStreamCallback {

    static private ZGMixStreamDemo zgMixStreamDemo;

    public static final String mixStreamPrefix = "MixStream_";
    public static final long anchorSoundLevelID = 1001;
    public static final long audienceSoundLevelID = 1002;

    private ZegoStreamMixer zegoStreamMixer = null;
    private ZegoMixStreamConfig mixStreamConfig = null;
    private MixStreamCallback mixStreamCallback = null;
    private List<ZegoMixStreamInfo> mMixStreamInfos = new ArrayList<>();

    public static ZGMixStreamDemo sharedInstance() {
        synchronized (ZGMixStreamDemo.class) {
            if (zgMixStreamDemo == null) {
                zgMixStreamDemo = new ZGMixStreamDemo();

            }
        }
        return zgMixStreamDemo;
    }

    public void setMixStreamCallback(MixStreamCallback mixStreamCallback){
        zegoStreamMixer = new ZegoStreamMixer();
        //设置混流配置回调监听
        zegoStreamMixer.setMixStreamExCallback(this);
        //设置混流音量回调监听
        zegoStreamMixer.setSoundLevelInMixStreamCallback(this);

        this.mixStreamCallback = mixStreamCallback;
    }

    public void startMixStream(String mixStreamID) {
        if (zegoStreamMixer == null){
            zegoStreamMixer = new ZegoStreamMixer();
        }

        int size = mMixStreamInfos.size();
        ZegoMixStreamInfo[] inputStreamList = new ZegoMixStreamInfo[size];

        for (int i = 0; i < size; i++) {
            inputStreamList[i] = mMixStreamInfos.get(i);
        }
        mixStreamConfig = new ZegoMixStreamConfig();
        mixStreamConfig.channels = 1; // 默认值
        mixStreamConfig.outputBitrate = 800000;
        mixStreamConfig.outputFps = 15;

        mixStreamConfig.outputAudioBitrate = 48000;

        // 混流画面分辨率
        ZegoAvConfig zegoAvConfig = ZGMixStreamPublisher.sharedInstance().getZegoAvConfig();
        if (zegoAvConfig != null) {
            mixStreamConfig.outputHeight = zegoAvConfig.getVideoCaptureResolutionHeight();
            mixStreamConfig.outputWidth = zegoAvConfig.getVideoCaptureResolutionWidth();
        } else {
            mixStreamConfig.outputHeight = 1280;
            mixStreamConfig.outputWidth = 720;
        }

        // 输入流
        mixStreamConfig.inputStreamList = inputStreamList;

        ZegoMixStreamOutput mixStreamOutput1 = new ZegoMixStreamOutput();
        mixStreamOutput1.isUrl = false;
        mixStreamOutput1.target = mixStreamID;//"rtmp://wsdemo.zego.im/livestream/"+mixStreamID;
        ZegoMixStreamOutput[] mixStreamOutputs = {mixStreamOutput1};
        // 输出流
        mixStreamConfig.outputList = mixStreamOutputs;

        //混流背景图
        mixStreamConfig.outputBackgroundImage = "preset-id://zegobg.png"; //preset-id://inke-bg
        //开启音浪
        mixStreamConfig.withSoundLevel = true;

        // 水印布局
        ZegoMixStreamWatermark watermark = new ZegoMixStreamWatermark();
        watermark.left = mixStreamConfig.outputWidth/3;
        watermark.top = mixStreamConfig.outputHeight/3;
        watermark.right = mixStreamConfig.outputWidth/3+mixStreamConfig.outputWidth/2;
        watermark.bottom = mixStreamConfig.outputHeight/3+mixStreamConfig.outputHeight/5;
        watermark.image = "preset-id://zegowp.png";
        mixStreamConfig.watermark = watermark;

        // 背景颜色
        mixStreamConfig.outputBackgroundColor = 0x87CEFA00; //此处采用淡蓝色做背景，#87CEFA 是淡蓝色，后两位用00补充

        if (zegoStreamMixer != null) {
            int seq = zegoStreamMixer.mixStreamEx(mixStreamConfig, mixStreamID);
        }
    }

    public void handleMixStreamAdded(final ZegoStreamInfo[] listStream, String mixStreamID) {

        int width = 240;
        int height = 320;
        if (listStream != null && listStream.length > 0) {
            for (ZegoStreamInfo streamInfo : listStream) {

                if (mMixStreamInfos.size() == 1) {
                    ZegoMixStreamInfo mixStreamInfo = new ZegoMixStreamInfo();
                    mixStreamInfo.streamID = streamInfo.streamID;
                    mixStreamInfo.left = 0;
                    mixStreamInfo.top = 0;
                    mixStreamInfo.right = width;
                    mixStreamInfo.bottom = height;
                    mMixStreamInfos.add(mixStreamInfo);
                }
            }

            startMixStream(mixStreamID);
        }
    }

    public void handleMixStreamDeleted(final ZegoStreamInfo[] listStream, final String mixStreamID) {
        if (listStream != null && listStream.length > 0) {
            for (ZegoStreamInfo bizStream : listStream) {

                for (ZegoMixStreamInfo info : mMixStreamInfos) {
                    if (bizStream.streamID.equals(info.streamID)) {
                        mMixStreamInfos.remove(info);
                        break;
                    }
                }
            }

            startMixStream(mixStreamID);
        }
    }

    public void stopMixStream(String mixStreamID) {
        mMixStreamInfos.clear();
        startMixStream(mixStreamID);

//        mixStreamConfig.inputStreamList = new ZegoMixStreamInfo[0];
//        if (zegoStreamMixer != null){
//            zegoStreamMixer.mixStreamEx(mixStreamConfig, mixStreamID);
//        }
    }

    public void prepareMixStreamInfo(ZegoMixStreamInfo mixStreamInfo) {
        mMixStreamInfos.add(mixStreamInfo);
    }

    public String getMixStreamID(ZegoStreamInfo[] zegoStreamInfos) {
        String mixStreamID = "";
        for (ZegoStreamInfo info : zegoStreamInfos) {
            final HashMap<String, String> mapExtraInfo =
                    (new Gson()).fromJson(info.extraInfo, new TypeToken<HashMap<String, String>>() {
                    }.getType());

            if (mapExtraInfo != null && mapExtraInfo.size() > 0) {

                boolean firstAnchor = Boolean.valueOf(mapExtraInfo.get(ZGMixStreamDemoHelper.Constants.FIRST_ANCHOR));
                String tmpStreamID = String.valueOf(mapExtraInfo.get(ZGMixStreamDemoHelper.Constants.KEY_MIX_STREAM_ID));
                if (firstAnchor && !TextUtils.isEmpty(tmpStreamID)) {
                    mixStreamID = tmpStreamID;
                }
            }
        }
        return mixStreamID;
    }

    public void unInit(){

        if (zegoStreamMixer != null) {
            zegoStreamMixer = null;
        }

        if (mixStreamCallback != null) {
            mixStreamCallback = null;
        }

        zgMixStreamDemo = null;
    }

    public interface MixStreamCallback{

        void onMixStreamCallback(int errorcode, String mixStreamID);
        void onSoundLevelInMixStream(long anchorSoundLevel, long audienceSoundLevel);
    }

    @Override
    public void onMixStreamExConfigUpdate(int stateCode, String mixStreamID, ZegoMixStreamResultEx streamInfo) {

        if (mixStreamCallback != null) {
            mixStreamCallback.onMixStreamCallback(stateCode, mixStreamID);
        }

        if (streamInfo.outputList.size()>0){
            for (ZegoMixStreamOutputResult outputResult: streamInfo.outputList) {

                if (!outputResult.streamID.equals("")){
                    Map<String, String> mapUrls = new HashMap<>();
                    mapUrls.put(ZGMixStreamDemoHelper.Constants.FIRST_ANCHOR, String.valueOf(true));
                    mapUrls.put(ZGMixStreamDemoHelper.Constants.KEY_MIX_STREAM_ID, mixStreamID);
                    mapUrls.put(ZGMixStreamDemoHelper.Constants.KEY_HLS, outputResult.hlsList.get(0));
                    mapUrls.put(ZGMixStreamDemoHelper.Constants.KEY_RTMP, outputResult.rtmpList.get(0));
                    mapUrls.put(ZGMixStreamDemoHelper.Constants.KEY_FLV, outputResult.flvList.get(0));

                    Gson gson = new Gson();
                    String json = gson.toJson(mapUrls);

                    // 将混流信息通知给房间观众
                    boolean ret = ZGManager.sharedInstance().api().updateStreamExtraInfo(json);

                    break;
                }
            }
        }
    }

    // 混流音量回调
    @Override
    public void onSoundLevelInMixStream(ArrayList<ZegoSoundLevelInMixStreamInfo> arrayList) {

        long anchorSoundLevel = 0;
        long audienceSoundLevel = 0;
        for (ZegoSoundLevelInMixStreamInfo soundInfo: arrayList){
            if (soundInfo.soundLevelID == anchorSoundLevelID) {
                anchorSoundLevel = soundInfo.soundLevel;
            } else if (soundInfo.soundLevelID == audienceSoundLevelID){
                audienceSoundLevel = soundInfo.soundLevel;
            }
        }

        if (mixStreamCallback != null){
            mixStreamCallback.onSoundLevelInMixStream(anchorSoundLevel, audienceSoundLevel);
        }
    }
}
