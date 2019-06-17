package com.zego.videocommunication.ui;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.CompoundButton;

import com.zego.common.ZGBaseHelper;
import com.zego.common.ZGConfigHelper;
import com.zego.common.ZGPlayHelper;
import com.zego.common.ZGPublishHelper;
import com.zego.common.ui.BaseActivity;
import com.zego.common.util.AppLogger;
import com.zego.videocommunication.model.PublishStreamAndPlayStreamLayoutModel;
import com.zego.videocommunicaton.R;
import com.zego.videocommunicaton.databinding.PublishStreamAndPlayStreamBinding;
import com.zego.zegoliveroom.callback.IZegoLoginCompletionCallback;
import com.zego.zegoliveroom.callback.IZegoRoomCallback;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.constants.ZegoVideoViewMode;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;

import java.util.ArrayList;
import java.util.Date;

public class PublishStreamAndPlayStreamUI extends BaseActivity {

    /**
     * 获取当前Activity绑定的DataBinding
     *
     * @return
     */
    public PublishStreamAndPlayStreamBinding getPublishStreamAndPlayStreamBinding() {
        return publishStreamAndPlayStreamBinding;
    }

    //这里使用Google官方的MVVM框架DataBinding来实现UI逻辑，开发者可以根据自己的情况使用别的方式编写UI逻辑
    protected PublishStreamAndPlayStreamBinding publishStreamAndPlayStreamBinding;

    // 这里为防止多个设备测试时相同流id冲推导致的推流失败，这里使用时间戳的后4位来作为随机的流id，开发者可根据业务需要定义业务上的流id
    private String mPublishStreamid = "s-streamid-" + new Date().getTime()%(new Date().getTime()/10000);

    // 推拉流布局模型
    PublishStreamAndPlayStreamLayoutModel mPublishStreamAndPlayStreamLayoutModel;
    // 当拉多条流时，把流id的引用放到ArrayList里
    private ArrayList<String> playStreamids = new ArrayList<>();


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 使用DataBinding加载布局
        publishStreamAndPlayStreamBinding = DataBindingUtil.setContentView(this, R.layout.publish_stream_and_play_stream);


        // 设置麦克风和摄像头的点击事件
        setCameraAndMicrophoneStateChangedOnClickEvent();

        // 从VideoCommunicationMainUI的Activtity中获取传过来的RoomID，以便登录登录房间并马上推流
        Intent it = getIntent();
        String roomid = it.getStringExtra("roomID");

        // 设置当前UI界面左上角的点击事件，点击之后结束当前Activity，退出房间，SDK内部在退出房间的时候会自动停止推拉流
        publishStreamAndPlayStreamBinding.goBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // 设置房间代理，本示例中主要用于监听onStreamUpdated流更新回调，以便当房间有新增推流或停推流的时候拉这条流或停拉这条流
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
            public void onStreamUpdated(int type, ZegoStreamInfo[] zegoStreamInfos, String roomID) {

                // 当登陆房间成功后，如果房间内中途有人推流或停止推流。房间内其他人就能通过该回调收到流更新通知。
                for (ZegoStreamInfo streamInfo : zegoStreamInfos) {
                    // 当有流新增的时候，拉流
                    if (type == ZegoConstants.StreamUpdateType.Added) {
                        AppLogger.getInstance().i(ZGBaseHelper.class, "房间内收到流新增通知. streamID : %s, userName : %s, extraInfo : %s", streamInfo.streamID, streamInfo.userName, streamInfo.extraInfo);
                        TextureView playRenderView = PublishStreamAndPlayStreamUI.this.mPublishStreamAndPlayStreamLayoutModel.addStreamToViewInLayout(streamInfo.streamID);
                        ZGPlayHelper.sharedInstance().startPlaying(streamInfo.streamID, playRenderView);
                        ZGConfigHelper.sharedInstance().setPlayViewMode(ZegoVideoViewMode.ScaleAspectFill, streamInfo.streamID);
                        PublishStreamAndPlayStreamUI.this.playStreamids.add(streamInfo.streamID);

                    }
                    // 当有其他流关闭的时候，停拉
                    else if (type == ZegoConstants.StreamUpdateType.Deleted) {
                        AppLogger.getInstance().i(ZGBaseHelper.class, "房间内收到流删除通知. streamID : %s, userName : %s, extraInfo : %s", streamInfo.streamID, streamInfo.userName, streamInfo.extraInfo);
                        ZGPlayHelper.sharedInstance().stopPlaying(streamInfo.streamID);
                        PublishStreamAndPlayStreamUI.this.mPublishStreamAndPlayStreamLayoutModel.removeStreamToViewInLayout(streamInfo.streamID);
                        PublishStreamAndPlayStreamUI.this.playStreamids.remove(streamInfo.streamID);
                    }

                }

            }

            @Override
            public void onStreamExtraInfoUpdated(ZegoStreamInfo[] zegoStreamInfos, String s) {

            }

            @Override
            public void onRecvCustomCommand(String s, String s1, String s2, String s3) {

            }
        });

        // 这里创建多人连麦的Model的实例
        this.mPublishStreamAndPlayStreamLayoutModel = new PublishStreamAndPlayStreamLayoutModel(this);

        // 这里进入当前Activty之后马上登录房间，在登录房间的回调中，若房间已经有其他流在推，从登录回调中获取拉流信息并拉这些流
        ZGBaseHelper.sharedInstance().loginRoom(roomid, ZegoConstants.RoomRole.Anchor, new IZegoLoginCompletionCallback() {
            @Override
            public void onLoginCompletion(int i, ZegoStreamInfo[] zegoStreamInfos) {

                // 判断登录房间是否正常
                if(i == 0){

                    AppLogger.getInstance().i(PublishStreamAndPlayStreamUI.class, "登录房间成功");

                    //若该房间内有其他流在推，拉这些流并在推拉流的Model中渲染出来
                    for(ZegoStreamInfo zegoStreamInfo : zegoStreamInfos){

                        TextureView playRenderView = PublishStreamAndPlayStreamUI.this.mPublishStreamAndPlayStreamLayoutModel.addStreamToViewInLayout(zegoStreamInfo.streamID);
                        ZGPlayHelper.sharedInstance().startPlaying(zegoStreamInfo.streamID, playRenderView);
                        ZGConfigHelper.sharedInstance().setPlayViewMode(ZegoVideoViewMode.ScaleAspectFill, zegoStreamInfo.streamID);

                        playStreamids.add(zegoStreamInfo.streamID);

                        AppLogger.getInstance().i(PublishStreamAndPlayStreamUI.class, "当前房间存在：" + zegoStreamInfo.streamID);
                    }

                }else{
                    AppLogger.getInstance().i(PublishStreamAndPlayStreamUI.class, "登录房间失败 errorcode：" + i);

                }

            }
        });

        // 这里在登录房间之后马上推流并做本地推流的渲染
        TextureView localPreviewView = this.mPublishStreamAndPlayStreamLayoutModel.addStreamToViewInLayout(this.mPublishStreamid);
        ZGPublishHelper.sharedInstance().startPreview(localPreviewView);
        ZGConfigHelper.sharedInstance().setPreviewViewMode(ZegoVideoViewMode.ScaleAspectFill);
        ZGPublishHelper.sharedInstance().startPublishing(mPublishStreamid, mPublishStreamid + "-title", ZegoConstants.PublishFlag.JoinPublish);

    }

    /**
     * 定义设置麦克风和摄像头开关状态的点击事件
     *
     */
    private void setCameraAndMicrophoneStateChangedOnClickEvent() {

        // 设置摄像头开关的点击事件
        this.publishStreamAndPlayStreamBinding.CameraState.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if(isChecked){
                    ZGConfigHelper.sharedInstance().enableCamera(true);
                }else {
                    ZGConfigHelper.sharedInstance().enableCamera(false);

                }

            }
        });

        // 设置麦克风开关的点击事件
        this.publishStreamAndPlayStreamBinding.MicrophoneState.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if(isChecked){
                    ZGConfigHelper.sharedInstance().enableMic(true);
                }else {
                    ZGConfigHelper.sharedInstance().enableMic(false);
                }

            }
        });
    }

    /**
     * 当返回当前Activity的时候应该停止推拉流并退出房间，此处作为参考
     *
     */
    @Override
    public void onBackPressed() {

        ZGPublishHelper.sharedInstance().stopPublishing();
        this.mPublishStreamAndPlayStreamLayoutModel.removeAllStreamToViewInLayout();
        for(String playStreamid : this.playStreamids){
            ZGPlayHelper.sharedInstance().stopPlaying(playStreamid);
        }
        ZGBaseHelper.sharedInstance().loginOutRoom();

        super.onBackPressed();
    }

    /**
     * 供其他Activity调用，进入当前Activity进行推拉流
     *
     * @param activity
     */
    public static void actionStart(Activity activity, String roomID) {
        Intent intent = new Intent(activity, PublishStreamAndPlayStreamUI.class);
        intent.putExtra("roomID", roomID);

        activity.startActivity(intent);
    }


}
