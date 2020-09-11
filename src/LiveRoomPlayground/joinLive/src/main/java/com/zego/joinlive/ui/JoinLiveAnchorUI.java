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
import com.zego.joinlive.constants.JoinLiveUserInfo;
import com.zego.joinlive.constants.JoinLiveView;
import com.zego.joinlive.databinding.ActivityJoinLiveAnchorBinding;
import com.zego.zegoliveroom.callback.IZegoAudioRouteCallback;
import com.zego.zegoliveroom.callback.IZegoLivePlayerCallback;
import com.zego.zegoliveroom.callback.IZegoLivePublisherCallback;
import com.zego.zegoliveroom.callback.IZegoLoginCompletionCallback;
import com.zego.zegoliveroom.callback.IZegoRoomCallback;
import com.zego.zegoliveroom.callback.im.IZegoIMCallback;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.constants.ZegoIM;
import com.zego.zegoliveroom.constants.ZegoVideoViewMode;
import com.zego.zegoliveroom.entity.ZegoBigRoomMessage;
import com.zego.zegoliveroom.entity.ZegoPlayStreamQuality;
import com.zego.zegoliveroom.entity.ZegoPublishStreamQuality;
import com.zego.zegoliveroom.entity.ZegoRoomMessage;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;
import com.zego.zegoliveroom.entity.ZegoUserState;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 主播界面以及推流、拉连麦者流的一些操作
 * 1. 此 demo 未展示观众连麦需获得主播同意后再连麦的过程，观众上麦时，不需要主播同意就能推流；
 * 用户可根据实际的业务需求，增加观众向主播申请连麦信令的收发处理，比如观众向主播发送申请连麦的信令后，观众在收到主播的同意连麦信令后才能开始推流，否则不能推流。
 * 2. 此 demo 未展示主播邀请观众连麦的过程，只能观众自行上麦
 * 用户可根据实际的业务需求，增加主播邀请观众连麦信令的收发处理，比如主播向观众发送邀请连麦的信令后，观众在收到主播的邀请连麦信令同意之后才推流，实现观众与主播连麦。
 */
public class JoinLiveAnchorUI extends BaseActivity {

    private ActivityJoinLiveAnchorBinding binding;

    // SDK配置，麦克风和摄像头
    private SDKConfigInfo sdkConfigInfo = new SDKConfigInfo();

    // 主播流名
    private String anchorStreamID = ZegoUtil.getPublishStreamID();

    private String mRoomID = "";

    // 大view
    private JoinLiveView mBigView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_join_live_anchor);

        binding.setConfig(sdkConfigInfo);

        mRoomID = getIntent().getStringExtra("roomID");

        // 设置当前 UI 界面左上角的点击事件，点击之后结束当前 Activity 并停止推流/拉流、退出房间
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

        // 设置视图列表
        initViewList();

        // 设置SDK相关的回调监听
        initSDKCallback();

        // 登录房间并推流
        startPublish();
    }

    @Override
    public void finish() {
        super.finish();

        // 在退出页面时停止推流
        ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().stopPublishing();
        // 停止预览
        ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().stopPreview();

        // 将所有视图设置为可用
        ZGJoinLiveHelper.sharedInstance().freeAllJoinLiveView();
        // 清空已连麦列表
        ZGJoinLiveHelper.sharedInstance().resetJoinLiveAudienceList();

        // 登出房间
        ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().logoutRoom();
        // 去除 SDK 相关的回调监听
        releaseSDKCallback();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    // 设置视图列表
    protected void initViewList() {

        mBigView = new JoinLiveView(binding.preview, false, "");
        mBigView.setZegoLiveRoom(ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom());

        // 添加可用的视图，此demo展示共四个视图，一个全屏视图+3个小视图
        ArrayList<JoinLiveView> mJoinLiveView = new ArrayList<>();

        final JoinLiveView view1 = new JoinLiveView(binding.audienceView1, false, "");
        view1.setZegoLiveRoom(ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom());

        final JoinLiveView view2 = new JoinLiveView(binding.audienceView2, false, "");
        view2.setZegoLiveRoom(ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom());

        final JoinLiveView view3 = new JoinLiveView(binding.audienceView3, false, "");
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


    // 登录房间并推流
    public void startPublish() {
        // 设置房间配置，监听房间内用户状态的变更通知
        ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().setRoomConfig(true, true);
        AppLogger.getInstance().i(JoinLiveAnchorUI.class, "设置房间配置 audienceCreateRoom:%d, userStateUpdate:%d", 0, 1);
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
                AppLogger.getInstance().e(JoinLiveAnchorUI.class, devices);
            }
        });

        // 防止用户点击，弹出加载对话框
        CustomDialog.createDialog("登录房间中...", this).show();
        AppLogger.getInstance().i(JoinLiveAnchorUI.class, getString(R.string.tx_login_room));

        // 开始推流前需要先登录房间，此处是主播登录房间，成功登录后开始推流
        ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().loginRoom(mRoomID, ZegoConstants.RoomRole.Anchor, new IZegoLoginCompletionCallback() {
            @Override
            public void onLoginCompletion(int errorCode, ZegoStreamInfo[] zegoStreamInfos) {
                CustomDialog.createDialog(JoinLiveAnchorUI.this).cancel();
                if (errorCode == 0) {
                    AppLogger.getInstance().i(JoinLiveAnchorUI.class, "登录房间成功 roomId : %s", mRoomID);

                    // 设置预览视图模式，此处采用 SDK 默认值--等比缩放填充整View，可能有部分被裁减。
                    ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().setPreviewViewMode(ZegoVideoViewMode.ScaleAspectFill);
                    // 设置预览 view，主播自己推流采用全屏视图
                    ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().setPreviewView(mBigView.textureView);
                    // 启动预览
                    ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().startPreview();

                    // 开始推流，flag 使用连麦场景
                    ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().startPublishing(anchorStreamID, "anchor", ZegoConstants.PublishFlag.JoinPublish);

                    // 修改视图列表中的视图信息
                    mBigView.streamID = anchorStreamID;
                    mBigView.isPublishView = true;
                    ZGJoinLiveHelper.sharedInstance().modifyTextureViewInfo(mBigView);

                    // 拉取其它主播流，即进入其它主播房间进行直播
                    for (ZegoStreamInfo streamInfo : zegoStreamInfos) {

                        if (streamInfo != null) {
                            // 修改已连麦用户列表，此demo只展示三人连麦，此处根据连麦人数限制进行拉流
                            if (ZGJoinLiveHelper.sharedInstance().getHasJoinedUsers().size() < ZGJoinLiveHelper.MaxJoinLiveNum) {

                                // 获取可用的视图
                                JoinLiveView freeView = ZGJoinLiveHelper.sharedInstance().getFreeTextureView();

                                if (freeView != null) {
                                    // 拉流
                                    ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().startPlayingStream(streamInfo.streamID, freeView.textureView);
                                    // 设置拉流视图模式，此处采用 SDK 默认值--等比缩放填充整个View，可能有部分被裁减。
                                    ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().setViewMode(ZegoVideoViewMode.ScaleAspectFill, streamInfo.streamID);

                                    // 向已连麦列表中添加连麦者
                                    JoinLiveUserInfo userInfo = new JoinLiveUserInfo(streamInfo.userID, streamInfo.streamID);
                                    ZGJoinLiveHelper.sharedInstance().addJoinLiveAudience(userInfo);

                                    // 修改视图信息
                                    freeView.streamID = streamInfo.streamID;
                                    ZGJoinLiveHelper.sharedInstance().modifyTextureViewInfo(freeView);
                                } else {
                                    Toast.makeText(JoinLiveAnchorUI.this, getString(R.string.has_no_textureView), Toast.LENGTH_LONG).show();
                                }
                            } else {
                                Toast.makeText(JoinLiveAnchorUI.this, R.string.join_live_count_overflow, Toast.LENGTH_LONG).show();
                            }
                        }
                    }

                } else {
                    AppLogger.getInstance().i(JoinLiveAnchorUI.class, "登录房间失败, errorCode : %d", errorCode);
                    Toast.makeText(JoinLiveAnchorUI.this, getString(com.zego.common.R.string.tx_login_room_failure), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * 供其他Activity调用，进入本专题的方法
     *
     * @param activity
     */
    public static void actionStart(Activity activity, String roomID) {
        Intent intent = new Intent(activity, JoinLiveAnchorUI.class);
        intent.putExtra("roomID", roomID);
        activity.startActivity(intent);
    }

    // 设置 SDK 相关的回调监听
    public void initSDKCallback() {
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
                // 当登录房间成功后，如果房间内中途有人推流或停止推流。房间内其他人就能通过该回调收到流更新通知。

                if (roomID.equals(mRoomID)) {

                    for (ZegoStreamInfo streamInfo : zegoStreamInfos) {
                        // 当有流新增的时候，拉流
                        if (type == ZegoConstants.StreamUpdateType.Added) {
                            AppLogger.getInstance().i(JoinLiveAnchorUI.class, "房间: %s 内收到流新增通知. streamID : %s, userName : %s, extraInfo : %s", roomID, streamInfo.streamID, streamInfo.userName, streamInfo.extraInfo);

                            // 修改已连麦用户列表，此demo只展示三人连麦，此处根据连麦人数限制进行拉流
                            if (ZGJoinLiveHelper.sharedInstance().getHasJoinedUsers().size() < ZGJoinLiveHelper.MaxJoinLiveNum) {

                                // 获取可用的视图
                                JoinLiveView freeView = ZGJoinLiveHelper.sharedInstance().getFreeTextureView();

                                if (freeView != null) {
                                    // 拉流
                                    ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().startPlayingStream(streamInfo.streamID, freeView.textureView);
                                    // 设置拉流视图模式，此处采用 SDK 默认值--等比缩放填充整个View，可能有部分被裁减。
                                    ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().setViewMode(ZegoVideoViewMode.ScaleAspectFill, streamInfo.streamID);

                                    // 向已连麦列表中添加连麦者
                                    JoinLiveUserInfo userInfo = new JoinLiveUserInfo(streamInfo.userID, streamInfo.streamID);
                                    ZGJoinLiveHelper.sharedInstance().addJoinLiveAudience(userInfo);

                                    // 修改视图信息
                                    freeView.streamID = streamInfo.streamID;
                                    ZGJoinLiveHelper.sharedInstance().modifyTextureViewInfo(freeView);
                                } else {
                                    Toast.makeText(JoinLiveAnchorUI.this, getString(R.string.has_no_textureView), Toast.LENGTH_LONG).show();
                                }
                            } else {
                                Toast.makeText(JoinLiveAnchorUI.this, R.string.join_live_count_overflow, Toast.LENGTH_LONG).show();
                            }
                        }
                        // 当有其他流关闭的时候，停止拉流
                        else if (type == ZegoConstants.StreamUpdateType.Deleted) {
                            AppLogger.getInstance().i(JoinLiveAnchorUI.class, "房间：%s 内收到流删除通知. streamID : %s, userName : %s, extraInfo : %s", roomID, streamInfo.streamID, streamInfo.userName, streamInfo.extraInfo);
                            // 如果此条流删除信息是连麦者的流，做停止拉流的处理
                            for (JoinLiveUserInfo userInfo : ZGJoinLiveHelper.sharedInstance().getHasJoinedUsers()) {
                                if (userInfo.userID.equals(streamInfo.userID)) {
                                    // 停止拉流
                                    ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().stopPlayingStream(streamInfo.streamID);

                                    // 移除此连麦者
                                    ZGJoinLiveHelper.sharedInstance().removeJoinLiveAudience(userInfo);
                                    // 修改视图信息
                                    ZGJoinLiveHelper.sharedInstance().setJoinLiveViewFree(streamInfo.streamID);

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

        // 设置推流回调监听
        ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().setZegoLivePublisherCallback(new IZegoLivePublisherCallback() {
            // 推流回调文档说明: <a>https://doc.zego.im/API/ZegoLiveRoom/Android/html/index.html</a>

            @Override
            public void onPublishStateUpdate(int errorCode, String streamID, HashMap<String, Object> hashMap) {
                // 推流状态更新，errorCode 非0 则说明推流失败
                // 推流常见错误码请看文档: <a>https://doc.zego.im/CN/308.html</a>

                if (errorCode == 0) {
                    AppLogger.getInstance().i(JoinLiveAnchorUI.class, "推流成功, streamID : %s", streamID);
                    Toast.makeText(JoinLiveAnchorUI.this, getString(R.string.tx_publish_success), Toast.LENGTH_SHORT).show();
                } else {
                    AppLogger.getInstance().i(JoinLiveAnchorUI.class, "推流失败, streamID : %s, errorCode : %d", streamID, errorCode);
                    Toast.makeText(JoinLiveAnchorUI.this, getString(R.string.tx_publish_fail), Toast.LENGTH_SHORT).show();

                    // 推流失败就解除视图的占用
                    ZGJoinLiveHelper.sharedInstance().setJoinLiveViewFree(streamID);
                }
            }

            @Override
            public void onJoinLiveRequest(int seq, String fromUserID, String fromUserName, String roomID) {
                /**
                 * 房间内有人申请加入连麦时会回调该方法
                 */
            }

            @Override
            public void onPublishQualityUpdate(String s, ZegoPublishStreamQuality zegoPublishStreamQuality) {
                /**
                 * 推流质量更新, 回调频率默认3秒一次
                 * 可通过 {@link com.zego.zegoliveroom.ZegoLiveRoom#setPublishQualityMonitorCycle(long)} 修改回调频率
                 */
            }

            @Override
            public void onCaptureVideoSizeChangedTo(int width, int height) {

            }

            @Override
            public void onCaptureVideoFirstFrame() {
                // 当SDK采集摄像头捕获到第一帧时会回调该方法

            }

            @Override
            public void onCaptureAudioFirstFrame() {
                // 当SDK音频采集设备捕获到第一帧时会回调该方法
            }
        });

        // 设置拉流回调监听
        ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().setZegoLivePlayerCallback(new IZegoLivePlayerCallback() {
            @Override
            public void onPlayStateUpdate(int stateCode, String streamID) {
                // 拉流状态更新，errorCode 非0 则说明拉流失败
                // 拉流常见错误码请看文档: <a>https://doc.zego.im/CN/491.html</a>

                if (stateCode == 0) {
                    AppLogger.getInstance().i(JoinLiveAnchorUI.class, "拉流成功, streamID : %s", streamID);
                    Toast.makeText(JoinLiveAnchorUI.this, getString(com.zego.common.R.string.tx_play_success), Toast.LENGTH_SHORT).show();
                } else {
                    AppLogger.getInstance().i(JoinLiveAnchorUI.class, "拉流失败, streamID : %s, errorCode : %d", streamID, stateCode);
                    Toast.makeText(JoinLiveAnchorUI.this, getString(com.zego.common.R.string.tx_play_fail), Toast.LENGTH_SHORT).show();

                    // 拉流失败，解除视图占用
                    ZGJoinLiveHelper.sharedInstance().setJoinLiveViewFree(streamID);

                    // 移除此连麦者
                    for (JoinLiveUserInfo userInfo : ZGJoinLiveHelper.sharedInstance().getHasJoinedUsers()) {
                        if (streamID.equals(userInfo.streamID)) {

                            ZGJoinLiveHelper.sharedInstance().removeJoinLiveAudience(userInfo);
                            break;
                        }
                    }
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
        });

        // 设置房间人数相关信息的回调监听
        ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().setZegoIMCallback(new IZegoIMCallback() {
            @Override
            public void onUserUpdate(ZegoUserState[] listUser, int updateType) {
                AppLogger.getInstance().i(JoinLiveAnchorUI.class, "收到房间成员更新通知");
                // 房间成员更新回调，可根据此回调来管理观众列表
                for (ZegoUserState userInfo : listUser) {

                    if (ZegoIM.UserUpdateFlag.Added == userInfo.updateFlag) {
                        // 房间增加成员，可进行业务相关的处理

                    } else if (ZegoIM.UserUpdateFlag.Deleted == userInfo.updateFlag) {
                        // 成员退出房间，可进行业务相关的处理

                    }
                }
            }

            @Override
            public void onRecvRoomMessage(String s, ZegoRoomMessage[] zegoRoomMessages) {

            }

            @Override
            public void onUpdateOnlineCount(String s, int i) {

            }

            @Override
            public void onRecvBigRoomMessage(String s, ZegoBigRoomMessage[] zegoBigRoomMessages) {

            }
        });
    }

    // 去除SDK相关的回调监听
    public void releaseSDKCallback() {
        ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().setZegoLivePublisherCallback(null);
        ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().setZegoLivePlayerCallback(null);
        ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().setZegoRoomCallback(null);
        ZGJoinLiveHelper.sharedInstance().getZegoLiveRoom().setZegoIMCallback(null);
    }
}
