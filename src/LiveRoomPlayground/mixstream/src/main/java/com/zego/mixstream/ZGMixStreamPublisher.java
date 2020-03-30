package com.zego.mixstream;

import android.view.TextureView;

import com.zego.common.ZGManager;
import com.zego.zegoliveroom.callback.IZegoLivePlayerCallback;
import com.zego.zegoliveroom.callback.IZegoLivePublisherCallback;
import com.zego.zegoliveroom.callback.IZegoRoomCallback;
import com.zego.zegoliveroom.constants.ZegoAvConfig;
import com.zego.zegoliveroom.constants.ZegoVideoViewMode;
import com.zego.zegoliveroom.entity.ZegoPlayStreamQuality;
import com.zego.zegoliveroom.entity.ZegoPublishStreamQuality;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;

import java.util.HashMap;

public class ZGMixStreamPublisher implements IZegoLivePublisherCallback, IZegoLivePlayerCallback, IZegoRoomCallback {

    static private ZGMixStreamPublisher zgMixStreamPublisher;

    public static final String roomName = "MixStreamDemo";

    private MixStreamPublisherCallback mixStreamPublisherCallback = null;

    private ZegoAvConfig zegoAvConfig = null;

    public static ZGMixStreamPublisher sharedInstance() {
        synchronized (ZGMixStreamPublisher.class) {
            if (zgMixStreamPublisher == null) {
                zgMixStreamPublisher = new ZGMixStreamPublisher();
            }
        }
        return zgMixStreamPublisher;
    }

    public void setMixStreamPublisherCallback(MixStreamPublisherCallback callback){
        // 设置ZegoRoomCallback监听
        ZGManager.sharedInstance().api().setZegoRoomCallback(this);
        // 设置推流回调监听
        ZGManager.sharedInstance().api().setZegoLivePublisherCallback(this);
        // 设置拉流回调监听
        ZGManager.sharedInstance().api().setZegoLivePlayerCallback(this);

        mixStreamPublisherCallback = callback;
    }

    public void startPublish(String publishID, int flag, TextureView view){

        ZGManager.sharedInstance().api().setPreviewView(view);
        ZGManager.sharedInstance().api().setPreviewViewMode(ZegoVideoViewMode.ScaleAspectFill);
        ZGManager.sharedInstance().api().enableCamera(true);
        // 设置推流分辨率
        setZegoAvConfig(new ZegoAvConfig(ZegoAvConfig.Level.High));

        ZGManager.sharedInstance().api().startPreview();

        // 推流
        ZGManager.sharedInstance().api().startPublishing(publishID, roomName, flag);
    }

    public void stopPublish() {
        //停止推流
        ZGManager.sharedInstance().api().stopPreview();
        ZGManager.sharedInstance().api().setPreviewView(null);
        ZGManager.sharedInstance().api().stopPublishing();
    }

    public void unInit(){

        ZGManager.sharedInstance().api().setZegoLivePlayerCallback(null);
        ZGManager.sharedInstance().api().setZegoRoomCallback(null);
        ZGManager.sharedInstance().api().setZegoLivePublisherCallback(null);
        mixStreamPublisherCallback = null;

        ZGManager.sharedInstance().api().logoutRoom();
    }

    public ZegoAvConfig getZegoAvConfig() {
        return zegoAvConfig;
    }

    public void setZegoAvConfig(ZegoAvConfig zegoAvConfig) {
        this.zegoAvConfig = zegoAvConfig;
        ZGManager.sharedInstance().api().setAVConfig(zegoAvConfig);
    }

    public interface MixStreamPublisherCallback {
        // 推流状态
        void onPublishStateUpdate(int stateCode, String streamID);
        // 连麦请求
        void onJoinLiveRequest(int seq, String fromUserID, String roomID);
        // 推流质量
        void onPublishQualityUpdate(String quality);

        // 与服务器断开
        void onDisconnect(int errorCode);
        // 房间流更新
        void onStreamUpdated(int type, ZegoStreamInfo[] zegoStreamInfos, String roomID);
        // 流的附加信息更新
        void onStreamExtraInfoUpdated(ZegoStreamInfo[] zegoStreamInfos, String roomID);

        // 拉流状态
        void onPlayStateUpdate(int stateCode, String streamID);
        // 拉流质量
        void onPlayQualityUpdate(String quality);
        // 结束连麦
        void onRecvEndJoinLiveCommand(String fromUserId, String fromUserName, String roomID);

    }

    // 播放回调
    @Override
    public void onPlayStateUpdate(int stateCode, String streamID) {
        if (mixStreamPublisherCallback != null){
            mixStreamPublisherCallback.onPlayStateUpdate(stateCode, streamID);
        }
    }

    @Override
    public void onPlayQualityUpdate(String s, ZegoPlayStreamQuality zegoPlayStreamQuality) {
        String netQualityStr = "";
        switch (zegoPlayStreamQuality.quality) {
            case 0:
                netQualityStr = "优";
                break;
            case 1:
                netQualityStr = "良";
                break;
            case 2:
                netQualityStr = "中";
                break;
            case 3:
                netQualityStr = "差";
            default:
                break;
        }

        double bitRate = Math.round(zegoPlayStreamQuality.vkbps*10)/10;
        double fps = Math.round(zegoPlayStreamQuality.vnetFps*10)/10;

        String qualityStr = "当前网络质量："+netQualityStr+"；码率："+bitRate+"kb/s"+"；帧率："+fps;

        if (mixStreamPublisherCallback != null){
            mixStreamPublisherCallback.onPlayQualityUpdate(qualityStr);
        }
    }

    @Override
    public void onInviteJoinLiveRequest(int i, String s, String s1, String s2) {

    }

    @Override
    public void onRecvEndJoinLiveCommand(String fromUserId, String fromUserName, String roomID) {
        if (mixStreamPublisherCallback != null){
            mixStreamPublisherCallback.onRecvEndJoinLiveCommand(fromUserId, fromUserName, roomID);
        }
    }

    @Override
    public void onVideoSizeChangedTo(String s, int i, int i1) {

    }

    // 推流回调
    @Override
    public void onPublishStateUpdate(int stateCode, String streamID, HashMap<String, Object> hashMap) {
        if (mixStreamPublisherCallback != null) {
            mixStreamPublisherCallback.onPublishStateUpdate(stateCode, streamID);
        }
    }

    @Override
    public void onJoinLiveRequest(int seq, String fromUserID, String fromUserName, String roomID) {
        if (mixStreamPublisherCallback != null){
            mixStreamPublisherCallback.onJoinLiveRequest(seq, fromUserID, roomID);
        }
    }

    @Override
    public void onPublishQualityUpdate(String streamID, ZegoPublishStreamQuality zegoPublishStreamQuality) {
        String netQualityStr = "";
        switch (zegoPublishStreamQuality.quality) {
            case 0:
                netQualityStr = "优";
                break;
            case 1:
                netQualityStr = "良";
                break;
            case 2:
                netQualityStr = "中";
                break;
            case 3:
                netQualityStr = "差";
            default:
                break;
        }

        double bitRate = Math.round(zegoPublishStreamQuality.vkbps*10)/10;
        double fps = Math.round(zegoPublishStreamQuality.vcapFps*10)/10;

        String qualityStr = "当前网络质量："+netQualityStr+"；码率："+bitRate+"kb/s"+"；帧率："+fps;

        if (mixStreamPublisherCallback != null){
            mixStreamPublisherCallback.onPublishQualityUpdate(qualityStr);
        }
    }

    @Override
    public void onCaptureVideoSizeChangedTo(int i, int i1) {

    }

    @Override
    public void onCaptureVideoFirstFrame() {

    }

    @Override
    public void onCaptureAudioFirstFrame() {
        // 当SDK音频采集设备捕获到第一帧时会回调该方法
    }

    // 房间回调
    @Override
    public void onKickOut(int reason, String roomID, String customReason) {

    }

    @Override
    public void onDisconnect(int errorcode, String s) {
        if (mixStreamPublisherCallback != null){
            mixStreamPublisherCallback.onDisconnect(errorcode);
        }
    }

    @Override
    public void onReconnect(int i, String s) {

    }

    @Override
    public void onTempBroken(int i, String s) {

    }

    @Override
    public void onStreamUpdated(int type, ZegoStreamInfo[] zegoStreamInfos, String roomID) {

        if (mixStreamPublisherCallback != null) {
            mixStreamPublisherCallback.onStreamUpdated(type, zegoStreamInfos, roomID);
        }
    }

    @Override
    public void onStreamExtraInfoUpdated(ZegoStreamInfo[] zegoStreamInfos, String roomID) {
        if (mixStreamPublisherCallback != null){
            mixStreamPublisherCallback.onStreamExtraInfoUpdated(zegoStreamInfos, roomID);
        }
    }

    @Override
    public void onRecvCustomCommand(String s, String s1, String s2, String s3) {

    }
}
