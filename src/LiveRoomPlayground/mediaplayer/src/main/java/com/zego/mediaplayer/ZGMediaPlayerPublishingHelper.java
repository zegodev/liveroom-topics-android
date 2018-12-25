package com.zego.mediaplayer;

import android.annotation.SuppressLint;
import android.content.Context;

import com.zego.common.ZGHelper;
import com.zego.common.ZGManager;
import com.zego.zegoliveroom.ZegoLiveRoom;
import com.zego.zegoliveroom.callback.IZegoLivePublisherCallback;
import com.zego.zegoliveroom.callback.IZegoRoomCallback;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.entity.AuxData;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;
import com.zego.zegoliveroom.entity.ZegoStreamQuality;

import java.util.HashMap;

/**
 * Created by zego on 2018/10/17.
 */

public class ZGMediaPlayerPublishingHelper implements IZegoLivePublisherCallback, IZegoRoomCallback {

    private ZGMediaPlayerPublishingState zgMediaPlayerPublishingState = null;

    /**
     * 开始推流
     */
    public void startPublishing(Context context, final ZGMediaPlayerPublishingState zgMediaPlayerPublishingState) {
        String mUserName = android.os.Build.MODEL.replaceAll(" ", "");
        String deviceId = ZGHelper.generateDeviceId(context);
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
            String[] hlsList = (String[]) hashMap.get("hlsList");
            String[] rtmpList = (String[]) hashMap.get("rtmpList");
            String[] flvList = (String[]) hashMap.get("flvList");
            state = String.format("PUBLISH STARTED:%s \n%s\n%s\n%s", hashMap.get("streamID"),
                    hlsList[0]
                    , rtmpList[0], flvList[0]);

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
    public void onPublishQualityUpdate(String s, ZegoStreamQuality zegoStreamQuality) {

    }

    @Override
    public AuxData onAuxCallback(int i) {
        return null;
    }

    @Override
    public void onCaptureVideoSizeChangedTo(int i, int i1) {

    }

    @Override
    public void onMixStreamConfigUpdate(int i, String s, HashMap<String, Object> hashMap) {

    }

    @Override
    public void onKickOut(int i, String s) {

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
