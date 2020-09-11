package com.zego.joinlive.ui;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.zego.common.ZGBaseHelper;
import com.zego.common.entity.SDKConfigInfo;
import com.zego.common.ui.BaseActivity;
import com.zego.common.util.AppLogger;
import com.zego.common.util.ZegoUtil;
import com.zego.common.widgets.CustomDialog;
import com.zego.joinlive.R;
import com.zego.joinlive.ZGJoinLiveHelper;
import com.zego.joinlive.constants.JoinLiveView;
import com.zego.joinlive.databinding.ActivityJoinLiveAudienceBinding;
import com.zego.zegoliveroom.callback.IZegoAudioRouteCallback;
import com.zego.zegoliveroom.callback.IZegoLivePlayerCallback;
import com.zego.zegoliveroom.callback.IZegoLivePublisherCallback;
import com.zego.zegoliveroom.callback.IZegoLoginCompletionCallback;
import com.zego.zegoliveroom.callback.IZegoRoomCallback;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.constants.ZegoVideoViewMode;
import com.zego.zegoliveroom.entity.ZegoPlayStreamQuality;
import com.zego.zegoliveroom.entity.ZegoPublishStreamQuality;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 观众界面以及拉主播流/连麦者流、推流的一些操作
 * 1. 此 demo 未展示观众向主播申请连麦的一个过程，观众点击"视频连麦"则和主播进行连麦，不用经过主播的同意
 * 用户可根据自己的实际业务需求，增加观众向主播进行连麦申请的操作（发送信令实现），在收到主播同意连麦的信令后再推流。
 * 2. 此 demo 未展示主播邀请观众连麦的过程，只能观众自行上麦即推流
 * 用户可根据业务需求，增加主播邀请观众连麦信令的收发处理，比如主播向观众发送邀请连麦的信令后，观众在收到主播的邀请连麦信令同意之后才推流，实现观众与主播连麦。
 */
public class JoinLiveAudienceUI extends BaseActivity {

    private ActivityJoinLiveAudienceBinding binding;

    // SDK配置，麦克风和摄像头
    private SDKConfigInfo sdkConfigInfo = new SDKConfigInfo();

    // 主播房间ID
    private String mRoomID;
    // 主播ID
    private String mAnchorID;

    // 推流流名
    private String mPublishStreamID = ZegoUtil.getPublishStreamID();
    // 是否连麦
    private boolean isJoinedLive = false;
    // 已拉流流名列表
    private ArrayList<String> mPlayStreamIDs = new ArrayList<>();

    // 大view
    private JoinLiveView mBigView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_join_live_audience);

        binding.setConfig(sdkConfigInfo);

        mRoomID = getIntent().getStringExtra("roomID");
        mAnchorID = getIntent().getStringExtra("anchorID");

        // 设置当前 UI 界面左上角的点击事件，点击之后结束当前 Activity 并停止拉流/推流、退出房间
        binding.goBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // 监听摄像头开关
        binding.swCamera.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.isPressed()) {
                    sdkConfigInfo.setEnableCamera(isChecked);
                    ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().enableCamera(isChecked);
                }
            }
        });

        // 监听麦克风开关
        binding.swMic.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.isPressed()) {
                    sdkConfigInfo.setEnableMic(isChecked);
                    ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().enableMic(isChecked);
                }

            }
        });

        // 设置拉流的视图列表
        initViewList();
        // 设置SDK相关的回调监听
        initSDKCallback();

        // 登录房间并拉流
        startPlay();

    }

    @Override
    public void finish(){
        super.finish();

        // 停止正在拉的流
        if (mPlayStreamIDs.size() > 0) {
            for (String streamID : mPlayStreamIDs) {
                ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().stopPlayingStream(streamID);
            }
        }

        // 清空拉流列表
        mPlayStreamIDs.clear();

        // 退出页面时如果是连麦状态则停止推流
        if (isJoinedLive) {
            ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().stopPublishing();
            ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().stopPreview();
            isJoinedLive = false;
        }
        // 设置所有视图可用
        ZGJoinLiveHelper.sharedInstance().freeAllJoinLiveView();

        // 退出房间
        ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().logoutRoom();
        // 去除SDK相关的回调监听
        releaseSDKCallback();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    // 设置拉流的视图列表
    protected void initViewList(){

        // 全屏视图用于展示主播流
        mBigView = new JoinLiveView(binding.playView, false, "");
        mBigView.setZegoLiveRoom(ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom());

        // 添加可用的连麦者视图，共三个视图
        ArrayList<JoinLiveView> mJoinLiveView = new ArrayList<>();
        final JoinLiveView view1 = new JoinLiveView(binding.audienceViewOne, false, "");
        view1.setZegoLiveRoom(ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom());

        final JoinLiveView view2 = new JoinLiveView(binding.audienceViewTwo, false, "");
        view2.setZegoLiveRoom(ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom());

        final JoinLiveView view3 = new JoinLiveView(binding.audienceViewThree, false, "");
        view3.setZegoLiveRoom(ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom());

        mJoinLiveView.add(mBigView);
        mJoinLiveView.add(view1);
        mJoinLiveView.add(view2);
        mJoinLiveView.add(view3);
        ZGJoinLiveHelper.sharedInstance().addTextureView(mJoinLiveView);

        /**
         * 设置视图的点击事件
         * 点击小视图时，切换到大视图上展示画面，大视图的画面展示到小视图上
         */
        view1.textureView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                view1.exchangeView(mBigView);
            }
        });

        view2.textureView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                view2.exchangeView(mBigView);
            }
        });
        view3.textureView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                view3.exchangeView(mBigView);
            }
        });
    }

    /**
     * 连麦/结束连麦
     * 此 demo 中不展示观众向主播申请连麦的一个过程，观众点击连麦则和主播进行连麦，不用经过主播的同意
     * 用户可根据自己的实际业务需求，增加观众向主播进行连麦申请的操作（发送信令实现），在收到主播同意连麦的信令后再推流
     */
    public void onClickApplyJoinLive(View view){

        if (binding.btnApplyJoinLive.getText().toString().equals(getString(R.string.tx_joinLive))){
            // button 说明为"视频连麦"时，执行推流的操作

            if (mPlayStreamIDs.size() == ZGJoinLiveHelper.MaxJoinLiveNum + 1){
                // 判断连麦人数是否达到上限，此demo只支持展示三人连麦；达到连麦上限时的拉流总数 = 1条主播流 + 三条连麦者的流
                Toast.makeText(JoinLiveAudienceUI.this, getString(R.string.join_live_count_overflow), Toast.LENGTH_SHORT).show();
            } else {
                // 不满足上述情况则开始连麦，即推流

                // 获取可用的视图
                JoinLiveView freeView = ZGJoinLiveHelper.sharedInstance().getFreeTextureView();

                if (freeView != null){
                    // 设置预览视图模式，此处采用 SDK 默认值--等比缩放填充整View，可能有部分被裁减。
                    ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().setPreviewViewMode(ZegoVideoViewMode.ScaleAspectFill);
                    // 设置预览 view
                    ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().setPreviewView(freeView.textureView);
                    // 启动预览
                    ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().startPreview();
                    // 开始推流，flag 使用连麦场景
                    ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().startPublishing(mPublishStreamID, "audienceJoinLive", ZegoConstants.PublishFlag.JoinPublish);

                    // 修改视图信息
                    freeView.streamID = mPublishStreamID;
                    freeView.isPublishView = true;
                    ZGJoinLiveHelper.sharedInstance().modifyTextureViewInfo(freeView);
                } else {
                    Toast.makeText(JoinLiveAudienceUI.this, getString(R.string.has_no_textureView), Toast.LENGTH_LONG).show();
                }

            }
        } else {
            // button 说明为"结束连麦"时，停止推流

            // 停止推流
            ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().stopPublishing();
            // 停止预览
            ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().stopPreview();

            isJoinedLive = false;
            // 设置视图可用
            ZGJoinLiveHelper.sharedInstance().setJoinLiveViewFree(mPublishStreamID);
            // 修改 button 的说明为"视频连麦"
            binding.btnApplyJoinLive.setText(getString(R.string.tx_joinLive));

            AppLogger.getInstance().i(JoinLiveAudienceUI.class, "观众结束连麦");
        }
    }

    // 登录房间并拉流
    public void startPlay(){
        AppLogger.getInstance().i(JoinLiveAudienceUI.class, "登录房间 %s", mRoomID);
        // 防止用户点击，弹出加载对话框
        CustomDialog.createDialog("登录房间中...", this).show();
        ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().setZegoAudioRouteCallback(new IZegoAudioRouteCallback() {
            @Override
            public void onAudioRouteChange(int i) {
                String devices = "";
                switch (i) {
                    case com.zego.zegoavkit2.ZegoConstants.AudioRouteType.Bluetooth:
                        devices = String.format("onAudioRouteChange回调：%s", "蓝牙");
                        break;
                    case com.zego.zegoavkit2.ZegoConstants.AudioRouteType.EarPhone:
                        devices = String.format("onAudioRouteChange回调：%s", "耳机");
                        break;
                    case com.zego.zegoavkit2.ZegoConstants.AudioRouteType.LoudSpeaker:
                        devices = String.format("onAudioRouteChange回调：%s", "扬声器");
                        break;
                    case com.zego.zegoavkit2.ZegoConstants.AudioRouteType.Receiver:
                        devices = String.format("onAudioRouteChange回调：%s", "听筒");
                        break;
                    case com.zego.zegoavkit2.ZegoConstants.AudioRouteType.UsbAudio:
                        devices = String.format("onAudioRouteChange回调：%s", "USB设备");
                        break;
                }
                AppLogger.getInstance().e(JoinLiveAudienceUI.class, devices);
            }
        });

        // 开始拉流前需要先登录房间，此处是观众登录主播所在的房间
        ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().loginRoom(mRoomID, ZegoConstants.RoomRole.Audience, new IZegoLoginCompletionCallback() {
            @Override
            public void onLoginCompletion(int errorCode, ZegoStreamInfo[] zegoStreamInfos) {
                CustomDialog.createDialog(JoinLiveAudienceUI.this).cancel();
                if (errorCode == 0) {
                    AppLogger.getInstance().i(JoinLiveAudienceUI.class, "登录房间成功 roomId : %s", mRoomID);

                    // 筛选主播流，主播流采用全屏的视图
                    for (ZegoStreamInfo streamInfo:zegoStreamInfos){

                        if (streamInfo.userID.equals(mAnchorID)){
                            // 主播流采用全屏的视图，开始拉流
                            ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().startPlayingStream(streamInfo.streamID, mBigView.textureView);
                            // 设置拉流视图模式，此处采用 SDK 默认值--等比缩放填充整View，可能有部分被裁减。
                            ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().setViewMode(ZegoVideoViewMode.ScaleAspectFill, streamInfo.streamID);

                            // 向拉流流名列表中添加流名
                            mPlayStreamIDs.add(streamInfo.streamID);
                            
                            // 修改视图信息
                            mBigView.streamID = streamInfo.streamID;
                            ZGJoinLiveHelper.sharedInstance().modifyTextureViewInfo(mBigView);

                            // 将 "视频连麦" button 置为可见
                            binding.btnApplyJoinLive.setVisibility(View.VISIBLE);

                            break;
                        }
                    }

                    // 拉副主播流（即连麦者的流）
                    for (ZegoStreamInfo streamInfo:zegoStreamInfos) {

                        if (!streamInfo.userID.equals(mAnchorID)){
                            // 获取可用的视图
                            JoinLiveView freeView = ZGJoinLiveHelper.sharedInstance().getFreeTextureView();
                            if (freeView != null) {
                                // 开始拉流
                                ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().startPlayingStream(streamInfo.streamID, freeView.textureView);
                                // 设置拉流视图模式，此处采用 SDK 默认值--等比缩放填充整个 View，可能有部分被裁减。
                                ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().setViewMode(ZegoVideoViewMode.ScaleAspectFill, streamInfo.streamID);

                                // 向拉流流名列表中添加流名
                                mPlayStreamIDs.add(streamInfo.streamID);

                                // 修改视图信息
                                freeView.streamID = streamInfo.streamID;
                                ZGJoinLiveHelper.sharedInstance().modifyTextureViewInfo(freeView);
                            }
                        }
                    }

                } else {
                    AppLogger.getInstance().i(JoinLiveAudienceUI.class, "登录房间失败, errorCode : %d", errorCode);
                    Toast.makeText(JoinLiveAudienceUI.this, getString(com.zego.common.R.string.tx_login_room_failure), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * 供其他Activity调用，进入本专题模块的方法
     *
     * @param activity
     */
    public static void actionStart(Activity activity, String roomID, String anchorID) {
        Intent intent = new Intent(activity, JoinLiveAudienceUI.class);
        intent.putExtra("roomID", roomID);
        intent.putExtra("anchorID", anchorID);
        activity.startActivity(intent);
    }

    // 设置 SDK 相关回调的监听
    public void initSDKCallback(){
        // 设置房间回调监听
        ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().setZegoRoomCallback(new IZegoRoomCallback() {
            @Override
            public void onKickOut(int reason, String roomID, String customReason) {

            }

            @Override
            public void onDisconnect(int errorcode, String roomID) {

            }

            @Override
            public void onReconnect(int errorcode, String roomID) {

            }

            @Override
            public void onTempBroken(int errorcode, String roomID) {

            }

            @Override
            public void onStreamUpdated(int type, ZegoStreamInfo[] zegoStreamInfos, String roomID) {
                // 房间流列表更新

                if (roomID.equals(mRoomID)){
                    // 当登录房间成功后，如果房间内中途有人推流或停止推流。房间内其他人就能通过该回调收到流更新通知。

                    for (ZegoStreamInfo streamInfo : zegoStreamInfos) {
                        // 当有流新增的时候，拉流
                        if (type == ZegoConstants.StreamUpdateType.Added) {
                            AppLogger.getInstance().i(JoinLiveAudienceUI.class, "房间内收到流新增通知. streamID : %s, userName : %s, extraInfo : %s", streamInfo.streamID, streamInfo.userName, streamInfo.extraInfo);

                            // 获取可用的视图
                            JoinLiveView freeView = ZGJoinLiveHelper.sharedInstance().getFreeTextureView();

                            if (freeView != null) {
                                if (!streamInfo.userID.equals(mAnchorID)) {
                                    // 开始拉流
                                    ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().startPlayingStream(streamInfo.streamID, freeView.textureView);
                                    // 设置拉流视图模式，此处采用 SDK 默认值--等比缩放填充整个View，可能有部分被裁减。
                                    ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().setViewMode(ZegoVideoViewMode.ScaleAspectFill, streamInfo.streamID);

                                    // 向拉流流名列表中添加流名
                                    mPlayStreamIDs.add(streamInfo.streamID);

                                    // 修改视图信息
                                    freeView.streamID = streamInfo.streamID;
                                    ZGJoinLiveHelper.sharedInstance().modifyTextureViewInfo(freeView);
                                } else {
                                    // 开始拉流，此处处理主播中途断流后重新推流
                                    ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().startPlayingStream(streamInfo.streamID, mBigView.textureView);
                                    // 设置拉流视图模式，此处采用 SDK 默认值--等比缩放填充整个View，可能有部分被裁减。
                                    ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().setViewMode(ZegoVideoViewMode.ScaleAspectFill, streamInfo.streamID);

                                    // 向拉流流名列表中添加流名
                                    mPlayStreamIDs.add(streamInfo.streamID);

                                    // 修改视图信息
                                    mBigView.streamID = streamInfo.streamID;
                                    ZGJoinLiveHelper.sharedInstance().modifyTextureViewInfo(mBigView);
                                }
                            }
                        }
                        // 当有其他流关闭的时候，停止拉流
                        else if (type == ZegoConstants.StreamUpdateType.Deleted) {
                            AppLogger.getInstance().i(JoinLiveAudienceUI.class, "房间内收到流删除通知. streamID : %s, userName : %s, extraInfo : %s", streamInfo.streamID, streamInfo.userName, streamInfo.extraInfo);
                            for (String playStreamID:mPlayStreamIDs){
                                if (playStreamID.equals(streamInfo.streamID)){

                                    // 停止拉流
                                    ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().stopPlayingStream(streamInfo.streamID);
                                    mPlayStreamIDs.remove(streamInfo.streamID);

                                    // 修改视图信息
                                    ZGJoinLiveHelper.sharedInstance().setJoinLiveViewFree(streamInfo.streamID);

                                    // 判断该条关闭流是否为主播
                                    if (streamInfo.userID.equals(mAnchorID)) {
                                        // 界面提示主播已停止直播
                                        Toast.makeText(JoinLiveAudienceUI.this, getString(R.string.tx_anchor_stoppublish), Toast.LENGTH_SHORT).show();

                                        // 在已连麦的情况下，主播停止直播连麦观众也停止推流
                                        if (isJoinedLive) {
                                            ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().stopPublishing();
                                            // 停止预览
                                            ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().stopPreview();

                                            isJoinedLive = false;
                                            // 设置视图可用
                                            ZGJoinLiveHelper.sharedInstance().setJoinLiveViewFree(mPublishStreamID);
                                        }
                                        // 将 "视频连麦" button 置为不可见
                                        binding.btnApplyJoinLive.setVisibility(View.INVISIBLE);
                                    }

                                    break;
                                }
                            }
                        }
                    }
                }

            }

            @Override
            public void onStreamExtraInfoUpdated(ZegoStreamInfo[] zegoStreamInfos, String roomID) {
                // 流的额外信息更新

            }

            @Override
            public void onRecvCustomCommand(String userID, String userName, String content, String roomID) {
                // 收到自定义信息

            }
        });

        // 设置拉流回调监听
        ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().setZegoLivePlayerCallback(new IZegoLivePlayerCallback() {
            @Override
            public void onPlayStateUpdate(int stateCode, String streamID) {
                // 拉流状态更新，errorCode 非0 则说明拉流失败
                // 拉流常见错误码请看文档: <a>https://doc.zego.im/CN/491.html</a>

                if (stateCode == 0) {
                    AppLogger.getInstance().i(JoinLiveAudienceUI.class, "拉流成功, streamID : %s", streamID);
                    Toast.makeText(JoinLiveAudienceUI.this, getString(com.zego.common.R.string.tx_play_success), Toast.LENGTH_SHORT).show();

                } else {
                    AppLogger.getInstance().i(JoinLiveAudienceUI.class, "拉流失败, streamID : %s, errorCode : %d", streamID, stateCode);
                    Toast.makeText(JoinLiveAudienceUI.this, getString(com.zego.common.R.string.tx_play_fail), Toast.LENGTH_SHORT).show();

                    // 解除视图占用
                    ZGJoinLiveHelper.sharedInstance().setJoinLiveViewFree(streamID);

                    // 从已拉流列表中移除该流名
                    mPlayStreamIDs.remove(streamID);
                }
            }

            @Override
            public void onPlayQualityUpdate(String streamID, ZegoPlayStreamQuality zegoPlayStreamQuality) {

            }

            @Override
            public void onInviteJoinLiveRequest(int seq, String fromUserID, String fromUserName, String roomID) {

            }

            @Override
            public void onRecvEndJoinLiveCommand(String fromUserID, String fromUserName, String roomID ) {

            }

            @Override
            public void onVideoSizeChangedTo(String streamID, int i, int i1) {

            }
        });

        // 设置推流回调监听
        ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().setZegoLivePublisherCallback(new IZegoLivePublisherCallback() {
            @Override
            public void onPublishStateUpdate(int errorCode, String streamID, HashMap<String, Object> hashMap) {
                // 推流状态更新，errorCode 非0 则说明推流失败
                // 推流常见错误码请看文档: <a>https://doc.zego.im/CN/308.html</a>

                if (errorCode == 0) {
                    AppLogger.getInstance().i(JoinLiveAudienceUI.class, "推流成功, streamID : %s", streamID);
                    Toast.makeText(JoinLiveAudienceUI.this, getString(R.string.tx_publish_success), Toast.LENGTH_SHORT).show();

                    isJoinedLive = true;

                    // 修改button的标识为 结束连麦
                    binding.btnApplyJoinLive.setText(getString(R.string.tx_end_join_live));
                } else {
                    AppLogger.getInstance().i(JoinLiveAudienceUI.class, "推流失败, streamID : %s, errorCode : %d", streamID, errorCode);
                    Toast.makeText(JoinLiveAudienceUI.this, getString(R.string.tx_publish_fail), Toast.LENGTH_SHORT).show();
                    // 解除视图占用
                    ZGJoinLiveHelper.sharedInstance().setJoinLiveViewFree(streamID);
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

            }
        });
    }

    // 去除SDK相关的回调监听
    public void releaseSDKCallback(){
        ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().setZegoLivePublisherCallback(null);
        ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().setZegoLivePlayerCallback(null);
        ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().setZegoRoomCallback(null);
    }
}
