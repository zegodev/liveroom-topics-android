package com.zego.mediaplayer;

import android.annotation.SuppressLint;
import android.content.Context;

import com.zego.common.ZGManager;
import com.zego.common.util.DeviceInfoManager;
import com.zego.zegoliveroom.ZegoLiveRoom;
import com.zego.zegoliveroom.callback.IZegoLivePublisherCallback;
import com.zego.zegoliveroom.callback.IZegoRoomCallback;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.entity.ZegoPublishStreamQuality;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;

import java.util.HashMap;

//import com.zego.zegoliveroom.entity.ZegoStreamQuality;

/**
 * Created by zego on 2018/10/17.
 */

public class ZGMediaPlayerPublishingHelper implements IZegoLivePublisherCallback, IZegoRoomCallback {

    private ZGMediaPlayerPublishingState zgMediaPlayerPublishingState = null;
    // zego推流时需要用到的流ID，推流时唯一ID
    private String mPublishStreamID = null;

    /**
     * 开始推流
     */
    public void startPublishing(Context context, final ZGMediaPlayerPublishingState zgMediaPlayerPublishingState) {
        String mUserName = android.os.Build.MODEL.replaceAll(" ", "");
        String deviceId = DeviceInfoManager.generateDeviceId(context);
        ZegoLiveRoom.setUser(deviceId, mUserName);
        this.zgMediaPlayerPublishingState = zgMediaPlayerPublishingState;
        ZGManager.sharedInstance().api().setZegoLivePublisherCallback(this);

        ZGManager.sharedInstance().api().loginRoom(deviceId, ZegoConstants.RoomRole.Anchor, (errorCode, zegoStreamInfos) -> {
            // 硬件编码开关
            ZegoLiveRoom.requireHardwareEncoder(false);
            if (errorCode == 0) {
                // 开始推流
                ZGManager.sharedInstance().api().startPublishing(deviceId, mUserName, ZegoConstants.PublishChannelIndex.MAIN);
            } else {
                zgMediaPlayerPublishingState.onPublishingState("LOGIN FAILED!");
            }
        });
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onPublishStateUpdate(int stateCode, String s, HashMap<String, Object> hashMap) {

        if (zgMediaPlayerPublishingState == null) {
            return;
        }

        String state;

        if (stateCode == 0) {
            String[] hlsList = new String[1];
            String[] rtmpList = new String[1];
            String[] flvList = new String[1];
            String streamID = "";
            if (hashMap.containsKey("flvList")) {
                flvList = (String[]) hashMap.get("flvList");
            }
            if (hashMap.containsKey("hlsList")) {
                hlsList = (String[]) hashMap.get("hlsList");
            }
            if (hashMap.containsKey("rtmpList")) {
                rtmpList = (String[]) hashMap.get("rtmpList");
            }
            if (hashMap.containsKey("streamID")) {
                streamID = (String) hashMap.get("streamID");
            }

            state = String.format("PUBLISH STARTED:%s \n%s\n%s\n%s", streamID,
                    hlsList.length >=1 ? hlsList[0] : ""
                    , rtmpList.length >=1 ? rtmpList[0] : "", flvList.length >=1 ? flvList[0] : "");

        } else {
            state = String.format("PUBLISH STOP: %d",
                    stateCode);
        }
        zgMediaPlayerPublishingState.onPublishingState(state);
    }

    @Override
    public void onJoinLiveRequest(int i, String s, String s1, String s2) {

    }

    @Override
    public void onPublishQualityUpdate(String s, ZegoPublishStreamQuality zegoPublishStreamQuality) {

    }

//    @Override
//    public void onPublishQualityUpdate(String s, ZegoStreamQuality zegoStreamQuality) {
//
//    }

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

    @Override
    public void onKickOut(int reason, String roomID, String customReason) {

    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onDisconnect(int errorCode, String s) {
        if (zgMediaPlayerPublishingState != null) {
            zgMediaPlayerPublishingState.onPublishingState(String.format("ROOM DISCONNECTED: %d", errorCode));
        }
    }

    @Override
    public void onReconnect(int i, String s) {

    }

    @Override
    public void onTempBroken(int i, String s) {

    }

    @Override
    public void onStreamUpdated(int i, ZegoStreamInfo[] zegoStreamInfos, String s) {

    }

    @Override
    public void onStreamExtraInfoUpdated(ZegoStreamInfo[] zegoStreamInfos, String s) {

    }

    @Override
    public void onRecvCustomCommand(String s, String s1, String s2, String s3) {

    }

    public interface ZGMediaPlayerPublishingState {

        void onPublishingState(String msg);

    }


}
