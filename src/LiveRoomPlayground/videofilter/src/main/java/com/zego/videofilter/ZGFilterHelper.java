package com.zego.videofilter;

import com.zego.zegoliveroom.ZegoLiveRoom;

public class ZGFilterHelper {

    private ZegoLiveRoom zegoLiveRoom = null;

    private static ZGFilterHelper zgFilterHelper = null;

    public static ZGFilterHelper sharedInstance() {
        synchronized (ZGFilterHelper.class) {
            if (zgFilterHelper == null) {
                zgFilterHelper = new ZGFilterHelper();
            }
        }

        return zgFilterHelper;
    }

    // 获取 ZegoLiveRoom 实例
    public ZegoLiveRoom getZegoLiveRoom(){
        if (null == zegoLiveRoom) {
            zegoLiveRoom = new ZegoLiveRoom();
        }
        return zegoLiveRoom;
    }

    // 销毁 ZegoLiveRoom 实例
    public void releaseZegoLiveRoom(){
        if (null != zegoLiveRoom){
            // 释放 SDK 资源
            zegoLiveRoom.unInitSDK();
            zegoLiveRoom = null;
        }
    }
}
