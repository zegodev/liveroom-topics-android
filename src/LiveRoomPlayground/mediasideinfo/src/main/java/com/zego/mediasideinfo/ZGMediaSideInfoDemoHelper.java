package com.zego.mediasideinfo;

import android.content.Context;
import android.view.TextureView;

import com.zego.common.ZGManager;
import com.zego.common.util.DeviceInfoManager;
import com.zego.zegoliveroom.callback.IZegoLivePlayerCallback;
import com.zego.zegoliveroom.callback.IZegoLivePublisherCallback;
import com.zego.zegoliveroom.callback.IZegoLoginCompletionCallback;
import com.zego.zegoliveroom.callback.IZegoRoomCallback;
import com.zego.zegoliveroom.entity.ZegoPlayStreamQuality;
import com.zego.zegoliveroom.entity.ZegoPublishStreamQuality;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;

import java.util.HashMap;

//import com.zego.zegoliveroom.entity.ZegoStreamQuality;

public class ZGMediaSideInfoDemoHelper implements IZegoLivePublisherCallback, IZegoLivePlayerCallback, IZegoRoomCallback {

    static private ZGMediaSideInfoDemoHelper zgMediaSideInfoDemoHelper;
    private ZGMediaSideTopicStatus mTopicStatus = ZGMediaSideTopicStatus.ZGMediaSideTopicStatus_None;

    private String mDeviceID;
    private String mChannelID = "ZEGO_TOPIC_MEDIA_SIDE_INFO";

    private TextureView mPlayView;

    private StatusChangedNotify mStatusChangedNotifier = null;

    public enum ZGMediaSideTopicStatus {
        ZGMediaSideTopicStatus_None,
        ZGMediaSideTopicStatus_Logining_Room,
        ZGMediaSideTopicStatus_Login_OK,
        ZGMediaSideTopicStatus_Login_Fail,
        ZGMediaSideTopicStatus_Start_Publishing,
        ZGMediaSideTopicStatus_Start_Playing,
        ZGMediaSideTopicStatus_Ready_For_Messaging
    }

    public static ZGMediaSideInfoDemoHelper sharedInstance() {
        synchronized (ZGMediaSideInfoDemoHelper.class) {
            if (zgMediaSideInfoDemoHelper == null) {
                zgMediaSideInfoDemoHelper = new ZGMediaSideInfoDemoHelper();
            }
        }
        return zgMediaSideInfoDemoHelper;
    }

    public void loginRoom() {
        ZGManager.sharedInstance().api().setZegoRoomCallback(this);

        // role：1-主播，2-观众
        boolean ret = ZGManager.sharedInstance().api().loginRoom(mChannelID, 1, new IZegoLoginCompletionCallback() {
            @Override
            public void onLoginCompletion(int errorcode, ZegoStreamInfo[] channelSteamList) {

                if (0 != errorcode) {
                    setTopicStatus(ZGMediaSideTopicStatus.ZGMediaSideTopicStatus_None);
                    return;
                }

                setTopicStatus(ZGMediaSideTopicStatus.ZGMediaSideTopicStatus_Login_OK);
            }
        });

        if (ret) {
            setTopicStatus(ZGMediaSideTopicStatus.ZGMediaSideTopicStatus_Logining_Room);
        } else {
            setTopicStatus(ZGMediaSideTopicStatus.ZGMediaSideTopicStatus_Login_Fail);
        }

    }

    public void publishAndPlayWithCongfig(Context context, boolean onlyAudioPublish, TextureView view){

        if (ZGMediaSideTopicStatus.ZGMediaSideTopicStatus_Login_OK != getTopicStatus()){
            return;
        }
        ZGManager.sharedInstance().api().setZegoLivePublisherCallback(this);
        ZGManager.sharedInstance().api().setZegoLivePlayerCallback(this);

        // 设置编解码分辨率
        ZGManager.sharedInstance().setZegoAvConfig(540, 960);

        mDeviceID = DeviceInfoManager.generateDeviceId(context);

        if (onlyAudioPublish){
            ZGManager.sharedInstance().api().enableCamera(false);
        }else {
            ZGManager.sharedInstance().api().setPreviewView(view);
            ZGManager.sharedInstance().api().enableCamera(true);
            ZGManager.sharedInstance().api().startPreview();
        }

        boolean publishRet = ZGManager.sharedInstance().api().startPublishing(mDeviceID, "MSI", 0); // flag 0-连麦，1-混流，2-单主播

        if (publishRet) {
            setTopicStatus(ZGMediaSideTopicStatus.ZGMediaSideTopicStatus_Start_Publishing);
        } else {
            setTopicStatus(ZGMediaSideTopicStatus.ZGMediaSideTopicStatus_None);
        }
    }

    // 修改登录状态、推拉流状态或者媒体次要信息发送状态
    public void setTopicStatus(ZGMediaSideTopicStatus status){
        mTopicStatus = status;

        if (null != mStatusChangedNotifier) {
            mStatusChangedNotifier.onStatusChanged(status);
        }
        if (ZGMediaSideTopicStatus.ZGMediaSideTopicStatus_None == mTopicStatus){
            // 登出房间之前 停止拉流，推流和预览
            ZGManager.sharedInstance().api().stopPlayingStream(mDeviceID);
            ZGManager.sharedInstance().api().stopPreview();
            ZGManager.sharedInstance().api().setPreviewView(null);
            ZGManager.sharedInstance().api().stopPublishing();
            // 登出房间
            ZGManager.sharedInstance().api().logoutRoom();
            ZGManager.sharedInstance().api().setZegoRoomCallback(null);
            ZGManager.sharedInstance().api().setZegoLivePublisherCallback(null);
            ZGManager.sharedInstance().api().setZegoLivePlayerCallback(null);
        }
    }

    public ZGMediaSideTopicStatus getTopicStatus() {
        return mTopicStatus;
    }

    public void setPlayView(TextureView view) {

        mPlayView = view;
    }

    public String stringOfTopicStatus(ZGMediaSideTopicStatus status) {
        String statusStr = "";

        switch (status) {
            case ZGMediaSideTopicStatus_None:
                statusStr = "None";
                break;
            case ZGMediaSideTopicStatus_Logining_Room:
                statusStr = "Logining";
                break;
            case ZGMediaSideTopicStatus_Login_OK:
                statusStr = "LoginSuccess";
                break;
            case ZGMediaSideTopicStatus_Login_Fail:
                statusStr = "LoginFail";
                break;
            case ZGMediaSideTopicStatus_Start_Publishing:
                statusStr = "Starting Publishing";
                break;
            case ZGMediaSideTopicStatus_Start_Playing:
                statusStr = "Starting Playing";
                break;
            case ZGMediaSideTopicStatus_Ready_For_Messaging:
                statusStr = "Ready";
                break;
            default:
                break;

        }
        return statusStr;
    }

    public interface StatusChangedNotify {
        void onStatusChanged(ZGMediaSideTopicStatus status);
    }

    public void setRecvStatusChangedNotify(StatusChangedNotify notifier) {
        mStatusChangedNotifier = notifier;
    }

    public void unSetRecvStatusChangedNotify() {
        mStatusChangedNotifier = null;
    }

    // 拉流回调
    @Override
    public void onPlayStateUpdate(int stateCode, String streamID) {

        if (0 == stateCode){

            if (ZGMediaSideTopicStatus.ZGMediaSideTopicStatus_Start_Playing != getTopicStatus()){
                return;
            }
            setTopicStatus(ZGMediaSideTopicStatus.ZGMediaSideTopicStatus_Ready_For_Messaging);

        } else {
            setTopicStatus(ZGMediaSideTopicStatus.ZGMediaSideTopicStatus_None);
        }
    }

    @Override
    public void onPlayQualityUpdate(String s, ZegoPlayStreamQuality zegoPlayStreamQuality) {

    }

    @Override
    public void onInviteJoinLiveRequest(int i, String s, String s1, String s2) {

    }

    @Override
    public void onRecvEndJoinLiveCommand(String s, String s1, String s2) {

    }

    @Override
    public void onVideoSizeChangedTo(String s, int i, int i1) {

    }

    //推流回调
    @Override
    public void onPublishStateUpdate(int stateCode, String streamID, HashMap<String, Object> streamInfo) {
        if (0 == stateCode) {
            if (ZGMediaSideTopicStatus.ZGMediaSideTopicStatus_Start_Publishing != getTopicStatus()) {
                return;
            }

            // 拉流
            boolean ret = ZGManager.sharedInstance().api().startPlayingStream(mDeviceID, mPlayView);

            if (ret) {
                setTopicStatus(ZGMediaSideTopicStatus.ZGMediaSideTopicStatus_Start_Playing);
            }
        } else {
            setTopicStatus(ZGMediaSideTopicStatus.ZGMediaSideTopicStatus_None);
        }

    }

    @Override
    public void onJoinLiveRequest(int i, String s, String s1, String s2) {

    }

    @Override
    public void onPublishQualityUpdate(String s, ZegoPublishStreamQuality zegoPublishStreamQuality) {

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

    //房间回调
    @Override
    public void onKickOut(int reason, String roomID, String customReason) {

    }

    @Override
    public void onDisconnect(int errorcode, String roomID) {
        setTopicStatus(ZGMediaSideTopicStatus.ZGMediaSideTopicStatus_None);
    }

    @Override
    public void onReconnect(int i, String s) {

    }

    @Override
    public void onTempBroken(int i, String s) {

    }

    @Override
    public void onStreamUpdated(int type, ZegoStreamInfo[] listStream, String roomID) {

    }

    @Override
    public void onStreamExtraInfoUpdated(ZegoStreamInfo[] zegoStreamInfos, String s) {

    }

    @Override
    public void onRecvCustomCommand(String s, String s1, String s2, String s3) {

    }
}
