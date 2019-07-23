package com.zego.videocommunication.ui;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Toast;

import com.zego.common.ZGBaseHelper;
import com.zego.common.ZGManager;
import com.zego.common.ZGPlayHelper;
import com.zego.common.ZGPublishHelper;
import com.zego.common.util.AppLogger;
import com.zego.videocommunicaton.R;
import com.zego.videocommunicaton.databinding.VideoCommunicationMainBinding;
import com.zego.common.ui.BaseActivity;
import com.zego.zegoliveroom.callback.IZegoInitSDKCompletionCallback;
import com.zego.zegoliveroom.callback.IZegoLivePlayerCallback;
import com.zego.zegoliveroom.callback.IZegoLivePublisherCallback;
import com.zego.zegoliveroom.callback.IZegoRoomCallback;
import com.zego.zegoliveroom.constants.ZegoAvConfig;
import com.zego.zegoliveroom.entity.AuxData;
import com.zego.zegoliveroom.entity.ZegoPlayStreamQuality;
import com.zego.zegoliveroom.entity.ZegoPublishStreamQuality;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;

import java.util.HashMap;


/**
 * 视频通话专题入口
 *
 */
public class VideoCommunicationMainUI extends BaseActivity {

    private VideoCommunicationMainBinding videoCommunicationMainBinding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 这里使用Google官方的MVVM框架来实现UI的控制逻辑，开发者可以根据情况选择使用此框架
        videoCommunicationMainBinding = DataBindingUtil.setContentView(this, R.layout.video_communication_main);
        // 点击左上方的返回控件之后销毁当前Activity
        videoCommunicationMainBinding.goBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VideoCommunicationMainUI.this.finish();
            }
        });
        // 点击"登录房间"的按钮进入房间并进行推拉流
        videoCommunicationMainBinding.btnJoinRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String roomid = videoCommunicationMainBinding.edRoomId.getText().toString().trim();
                if(0 != roomid.length()){
                    PublishStreamAndPlayStreamUI.actionStart(VideoCommunicationMainUI.this, roomid);
                }else {
                    AppLogger.getInstance().i(VideoCommunicationMainUI.class, getString(com.zego.common.R.string.tx_room_id_is_no_null));
                    Toast.makeText(VideoCommunicationMainUI.this, getString(com.zego.common.R.string.tx_room_id_is_no_null), Toast.LENGTH_SHORT).show();

                }
            }
        });

        // 在进入当前Activity之后马上初始化SDK
        initSDK();



    }

    /**
     * 初始化SDK逻辑，由于这里在释放SDK时会释放对应回调，因此在每次初始化时应重新设置
     *
     */
    private void initSDK() {
        // 初始化SDK
        ZGBaseHelper.sharedInstance().initZegoSDK(ZGManager.appId, ZGManager.appSign, true, new IZegoInitSDKCompletionCallback() {
            @Override
            public void onInitSDK(int errorCode) {
                if (errorCode == 0) {
                    VideoCommunicationMainUI.this.setSDKCallback();

                    AppLogger.getInstance().i(VideoCommunicationMainUI.class, "初始化zegoSDK成功");
                    // 由于多路实时视频需要的设备性能和带宽都比较高，这里里使用低分辨率来降低性能
                    ZegoAvConfig mZegoAvConfig = new ZegoAvConfig(ZegoAvConfig.Level.VeryLow);
                    mZegoAvConfig.setVideoEncodeResolution(90, 160);
                    mZegoAvConfig.setVideoCaptureResolution(90, 160);
                    ZGManager.sharedInstance().api().setAVConfig(mZegoAvConfig);
                } else {
                    AppLogger.getInstance().i(VideoCommunicationMainUI.class, "初始化zegoSDK失败 errorCode : %d", errorCode);
                }
            }
        });

    }

    /**
     * 退出当前Activity的时候释放SDK，对于这里的方式，开发者无需照搬，可根据需要将SDK一直设置为初始化状态
     *
     * */
    @Override
    public void onBackPressed() {

        unInitSDK();

        super.onBackPressed();
    }

    /**
     * 释放SDK，释放SDK应释放对应回调，对应的初始化SDK应该重新设置对应回调
     */
    private void unInitSDK(){
        releaseSDKCallback();
        ZGBaseHelper.sharedInstance().unInitZegoSDK();

    }

    private void releaseSDKCallback(){
        ZGBaseHelper.sharedInstance().releaseZegoRoomCallback();
        ZGPlayHelper.sharedInstance().releasePlayerCallback();
        ZGPublishHelper.sharedInstance().releasePublisherCallback();
    }

    /**
     * 设置相关回调
     */
    private void setSDKCallback(){

        ZGBaseHelper.sharedInstance().setZegoRoomCallback(new IZegoRoomCallback() {
            @Override
            public void onKickOut(int i, String s) {

            }

            @Override
            public void onDisconnect(int i, String s) {

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
        });

        ZGPublishHelper.sharedInstance().setPublisherCallback(new IZegoLivePublisherCallback() {
            @Override
            public void onPublishStateUpdate(int i, String s, HashMap<String, Object> hashMap) {

            }

            @Override
            public void onJoinLiveRequest(int i, String s, String s1, String s2) {

            }

            @Override
            public void onPublishQualityUpdate(String s, ZegoPublishStreamQuality zegoPublishStreamQuality) {

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
            public void onCaptureVideoFirstFrame() {

            }
            @Override
            public void onCaptureAudioFirstFrame() {
                // 当SDK音频采集设备捕获到第一帧时会回调该方法
            }
        });

        ZGPlayHelper.sharedInstance().setPlayerCallback(new IZegoLivePlayerCallback() {
            @Override
            public void onPlayStateUpdate(int i, String s) {

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
        });

    }



    /**
     * 供其他Activity调用，进入本专题的方法
     *
     * @param activity
     */
    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, VideoCommunicationMainUI.class);
        activity.startActivity(intent);
    }
}
