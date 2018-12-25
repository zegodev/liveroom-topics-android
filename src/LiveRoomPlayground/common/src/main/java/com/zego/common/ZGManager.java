package com.zego.common;

import android.util.Log;

import com.zego.zegoavkit2.ZegoExternalVideoCapture;
import com.zego.zegoavkit2.ZegoVideoCaptureFactory;
import com.zego.zegoliveroom.ZegoLiveRoom;
import com.zego.zegoliveroom.callback.IZegoInitSDKCompletionCallback;
import com.zego.zegoliveroom.constants.ZegoAvConfig;
import com.zego.zegoliveroom.constants.ZegoConstants;

/**
 * Created by zego on 2018/10/15.
 */

public class ZGManager {

    private ZegoLiveRoom zegoliveRoom;

    private static String mUserID;
    private static String mUserName;

    public ZegoLiveRoom api() {
        if (zegoliveRoom == null) {
            /**  请开发者联系 ZEGO support 获取各自业务的 AppID 与 signKey
             Demo 默认使用 UDP 模式，请填充该模式下的 AppID 与 signKey,其他模式不需要可不用填
             AppID 填写样式示例：1234567890
             signKey 填写样式示例：{0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,
             0x08,0x09,0x00,0x01,0x02,0x03,0x04,0x05,
             0x06,0x07,0x08,0x09,0x00,0x01,0x02,0x03,
             0x04,0x05,0x06,0x07,0x08,0x09,0x00,0x01} **/
            byte[] signKey = GetAppIdConfig.Key;
            long appId = GetAppIdConfig.appId;

            zegoliveRoom = new ZegoLiveRoom();
            zegoliveRoom.setTestEnv(true);
            zegoliveRoom.setUser(mUserID,mUserName);
//            ZegoLiveRoom.setVerbose(true);
            zegoliveRoom.initSDK(appId, signKey, new IZegoInitSDKCompletionCallback() {
                @Override
                public void onInitSDK(int errorcode) {
                    Log.e("Zego","zego init err: "+errorcode);
                }
            });
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

    public static void setLoginUser(String userID, String userName) {
        mUserID = userID;
        mUserName = userName;
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
