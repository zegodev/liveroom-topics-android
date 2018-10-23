package com.zego.common;

import com.zego.zegoavkit2.ZegoExternalVideoCapture;
import com.zego.zegoavkit2.ZegoVideoCaptureFactory;
import com.zego.zegoliveroom.ZegoLiveRoom;
import com.zego.zegoliveroom.constants.ZegoAvConfig;
import com.zego.zegoliveroom.constants.ZegoConstants;

/**
 * Created by zego on 2018/10/15.
 */

public class ZGManager {

    private ZegoLiveRoom zegoliveRoom;



    public ZegoLiveRoom api() {
        if (zegoliveRoom == null) {
            // TODO 这里会编译错误, 请在这里填写自己的appId 和 appKey
            byte[] signKey = GetAppIdConfig.Key;
            long appId = GetAppIdConfig.appId;
            zegoliveRoom = new ZegoLiveRoom();
            zegoliveRoom.initSDK(appId, signKey);
        }
        return zegoliveRoom;
    }

    public static ZGManager zgManager;

    public static ZGManager sharedInstance() {
        synchronized (ZGManager.class) {
            if (zgManager == null) {
                zgManager = new ZGManager();
            }
        }
        return zgManager;
    }


    public void enableExternalVideoCapture(ZegoVideoCaptureFactory zegoVideoCaptureFactory) {
        /* 释放ZegoSDK */
        unInitSDK();
        ZegoExternalVideoCapture.setVideoCaptureFactory(zegoVideoCaptureFactory, ZegoConstants.PublishChannelIndex.MAIN);
    }

    public void setZegoAvConfig(int width, int height){
        ZegoAvConfig mZegoAvConfig = new ZegoAvConfig(ZegoAvConfig.Level.High);
        mZegoAvConfig.setVideoEncodeResolution(width, height);
        mZegoAvConfig.setVideoCaptureResolution(width, height);
        mZegoAvConfig.setVideoFPS(25);
        api().setAVConfig(mZegoAvConfig);
    }

    public void unInitSDK() {
        if (zegoliveRoom != null) {
            zegoliveRoom.unInitSDK();
            zegoliveRoom = null;
        }
    }
}
