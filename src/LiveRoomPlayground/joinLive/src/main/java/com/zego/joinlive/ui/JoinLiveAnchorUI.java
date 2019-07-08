package com.zego.joinlive.ui;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.zego.common.ZGBaseHelper;
import com.zego.common.ZGConfigHelper;
import com.zego.common.ZGPlayHelper;
import com.zego.common.ZGPublishHelper;
import com.zego.common.entity.SDKConfigInfo;
import com.zego.common.ui.BaseActivity;
import com.zego.common.util.AppLogger;
import com.zego.common.util.ZegoUtil;
import com.zego.common.widgets.CustomDialog;
import com.zego.joinlive.R;
import com.zego.joinlive.ZGJoinLiveHelper;
import com.zego.joinlive.adapter.AudienceListAdapter;
import com.zego.joinlive.constants.JoinLiveUserInfo;
import com.zego.joinlive.constants.JoinLiveView;
import com.zego.joinlive.databinding.ActivityAudienceListBinding;
import com.zego.joinlive.databinding.ActivityJoinLiveAnchorBinding;
import com.zego.zegoliveroom.callback.IZegoLivePlayerCallback;
import com.zego.zegoliveroom.callback.IZegoLivePublisherCallback;
import com.zego.zegoliveroom.callback.IZegoLoginCompletionCallback;
import com.zego.zegoliveroom.callback.IZegoRoomCallback;
import com.zego.zegoliveroom.callback.im.IZegoIMCallback;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.constants.ZegoIM;
import com.zego.zegoliveroom.constants.ZegoVideoViewMode;
import com.zego.zegoliveroom.entity.AuxData;
import com.zego.zegoliveroom.entity.ZegoBigRoomMessage;
import com.zego.zegoliveroom.entity.ZegoConversationMessage;
import com.zego.zegoliveroom.entity.ZegoPlayStreamQuality;
import com.zego.zegoliveroom.entity.ZegoPublishStreamQuality;
import com.zego.zegoliveroom.entity.ZegoRoomMessage;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;
import com.zego.zegoliveroom.entity.ZegoUserState;

import java.util.ArrayList;
import java.util.HashMap;

public class JoinLiveAnchorUI extends BaseActivity {

    private ActivityJoinLiveAnchorBinding binding;

    private ActivityAudienceListBinding audienceListBinding;
    // SDK配置，麦克风和摄像头
    private SDKConfigInfo sdkConfigInfo = new SDKConfigInfo();

    // 主播流名
    private String anchorStreamID = ZegoUtil.getPublishStreamID();

    private String mRoomID = "";

    // 大view
    private JoinLiveView mBigView = null;

    // 观众列表适配器
    private AudienceListAdapter audienceListAdapter = new AudienceListAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_join_live_anchor);
        audienceListBinding = binding.audienceListLayout;

        binding.setConfig(sdkConfigInfo);

        mRoomID = getIntent().getStringExtra("roomID");

        // 监听摄像头与麦克风开关
        binding.swCamera.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.isPressed()) {
                    sdkConfigInfo.setEnableCamera(isChecked);
                    ZGConfigHelper.sharedInstance().enableCamera(isChecked);
                }
            }
        });

        binding.swMic.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.isPressed()) {
                    sdkConfigInfo.setEnableMic(isChecked);
                    ZGConfigHelper.sharedInstance().enableMic(isChecked);
                }

            }
        });

        // 设置视图列表
        initViewList();

        // 设置SDK相关的回调监听
        initSDKCallback();

        // 设置连麦相关的回调监听
        initJoinLiveCallback();

        // 登录房间并推流
        startPublish();

        // 处理观众列表相关的操作
        handleAudienceList();

        // 设置踢人的点击事件
        binding.kickout1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // 获取该视图的连麦者ID
                String joinLiveUserID = getJoinLiveUserID(binding.kickout1);

                if (!joinLiveUserID.equals("")) {
                    AppLogger.getInstance().i(JoinLiveAnchorUI.class, "结束 %s 连麦", joinLiveUserID);
                    // 结束连麦
                    ZGJoinLiveHelper.sharedInstance().endJoinLive(joinLiveUserID);
                }
            }
        });

        binding.kickout2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 获取该视图的连麦者ID
                String joinLiveUserID = getJoinLiveUserID(binding.kickout2);

                if (!joinLiveUserID.equals("")) {
                    AppLogger.getInstance().i(JoinLiveAnchorUI.class, "结束 %s 连麦", joinLiveUserID);
                    // 结束连麦
                    ZGJoinLiveHelper.sharedInstance().endJoinLive(joinLiveUserID);
                }
            }
        });

        binding.kickout3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 获取该视图的连麦者ID
                String joinLiveUserID = getJoinLiveUserID(binding.kickout3);

                if (!joinLiveUserID.equals("")) {
                    AppLogger.getInstance().i(JoinLiveAnchorUI.class, "结束 %s 连麦", joinLiveUserID);
                    // 结束连麦
                    ZGJoinLiveHelper.sharedInstance().endJoinLive(joinLiveUserID);
                }
            }
        });


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 停止推流
        ZGPublishHelper.sharedInstance().stopPublishing();
        ZGPublishHelper.sharedInstance().stopPreviewView();

        // 将所有视图设置为可用
        ZGJoinLiveHelper.sharedInstance().freeAllJoinLiveView();

        // 清空已连麦列表
        ZGJoinLiveHelper.sharedInstance().resetJoinLiveAudienceList();
        // 登出房间
        ZGBaseHelper.sharedInstance().loginOutRoom();
        // 去除SDK相关的回调监听
        releaseSDKCallback();
        // 去除连麦相关的回调监听
        releaseJoinLiveCallback();
    }

    public String getJoinLiveUserID(Button btn){
        String joinLiveUserID = "";
        for (JoinLiveView joinLiveView:ZGJoinLiveHelper.sharedInstance().getJoinLiveViewList()){
            if (joinLiveView.kickOutBtn == btn) {

                for (JoinLiveUserInfo userInfo:ZGJoinLiveHelper.sharedInstance().getHasJoinedUsers()){
                    if (userInfo.streamID.equals(joinLiveView.streamID)) {
                        joinLiveUserID = userInfo.userID;

                        break;
                    }
                }
                break;
            }
        }

        return joinLiveUserID;
    }

    // 设置视图列表
    protected void initViewList(){

        mBigView = new JoinLiveView(binding.preview, null, false, "");

        // 添加可用的视图
        ArrayList<JoinLiveView> mJoinLiveView = new ArrayList<>();

        final JoinLiveView view1 = new JoinLiveView(binding.audienceView1, binding.kickout1, false, "");
        view1.setZegoLiveRoom(ZGBaseHelper.sharedInstance().getZegoLiveRoom());
        final JoinLiveView view2 = new JoinLiveView(binding.audienceView2, binding.kickout2,false, "");
        view2.setZegoLiveRoom(ZGBaseHelper.sharedInstance().getZegoLiveRoom());
        final JoinLiveView view3 = new JoinLiveView(binding.audienceView3, binding.kickout3,false, "");
        view3.setZegoLiveRoom(ZGBaseHelper.sharedInstance().getZegoLiveRoom());

        mJoinLiveView.add(mBigView);
        mJoinLiveView.add(view1);
        mJoinLiveView.add(view2);
        mJoinLiveView.add(view3);
        ZGJoinLiveHelper.sharedInstance().addTextureView(mJoinLiveView);

        // 设置视图的点击事件
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
    public void startPublish(){
        // 设置房间配置，观众不可以创建房间，监听房间内用户状态的变更通知
        ZGBaseHelper.sharedInstance().getZegoLiveRoom().setRoomConfig(false, true);
        AppLogger.getInstance().i(JoinLiveAnchorUI.class, "设置房间配置 audienceCreateRoom:%d, userStateUpdate:%d",0, 1);

        // 防止用户点击，弹出加载对话框
        CustomDialog.createDialog("登录房间中...", this).show();
        AppLogger.getInstance().i(JoinLiveAnchorUI.class, getString(R.string.tx_login_room));

        // 开始推流前需要先登录房间，此处是主播登录房间
        ZGBaseHelper.sharedInstance().loginRoom(mRoomID, ZegoConstants.RoomRole.Anchor, new IZegoLoginCompletionCallback() {
            @Override
            public void onLoginCompletion(int errorCode, ZegoStreamInfo[] zegoStreamInfos) {
                CustomDialog.createDialog(JoinLiveAnchorUI.this).cancel();
                if (errorCode == 0) {
                    AppLogger.getInstance().i(JoinLiveAnchorUI.class, "登录房间成功 roomId : %s", mRoomID);

                    // 设置预览视图模式
                    ZGBaseHelper.sharedInstance().getZegoLiveRoom().setPreviewViewMode(ZegoVideoViewMode.ScaleAspectFill);

                    // 调用sdk 开始预览接口 设置view 并启用预览
                    ZGPublishHelper.sharedInstance().startPreview(mBigView.textureView);
                    // 开始推流
                    ZGPublishHelper.sharedInstance().startPublishing(anchorStreamID, "anchor", ZegoConstants.PublishFlag.JoinPublish);

                    // 修改视图信息
                    mBigView.streamID = anchorStreamID;
                    mBigView.isPublishView = true;
                    ZGJoinLiveHelper.sharedInstance().modifyTextureViewInfo(mBigView);
                } else {
                    AppLogger.getInstance().i(JoinLiveAnchorUI.class, "登录房间失败, errorCode : %d", errorCode);
                    Toast.makeText(JoinLiveAnchorUI.this, getString(com.zego.common.R.string.tx_login_room_failure), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // 对观众列表的相关操作
    public void handleAudienceList(){

        audienceListBinding.audienceList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        // 设置adapter
        audienceListBinding.audienceList.setAdapter(audienceListAdapter);
        // 设置Item添加和移除的动画
        audienceListBinding.audienceList.setItemAnimator(new DefaultItemAnimator());

        // 对观众列表的点击事件监听
        audienceListAdapter.setOnItemClickListener(new AudienceListAdapter.OnItemClickListener() {

            @Override
            public void onItemClick(View view, int position, String audienceID) {

                // 该观众是否已连麦
                boolean isJoinedLive = false;
                if (ZGJoinLiveHelper.sharedInstance().getHasJoinedUsers().size() > 0) {
                    for (JoinLiveUserInfo userInfo : ZGJoinLiveHelper.sharedInstance().getHasJoinedUsers()) {
                        if (userInfo.userID.equals(audienceID)) {
                            isJoinedLive = true;
                            break;
                        }
                    }
                }

                if (!isJoinedLive) {
                    AppLogger.getInstance().i(JoinLiveAnchorUI.class, "邀请连麦, audienceID : %s", audienceID);

                    // 邀请连麦
                    ZGJoinLiveHelper.sharedInstance().inviteJoinLive(audienceID);
                    Toast.makeText(JoinLiveAnchorUI.this, getString(R.string.has_sended_invitation), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(JoinLiveAnchorUI.this, getString(R.string.this_audience_has_joined_live), Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 设置当前UI界面左上角的点击事件，点击之后结束当前Activity，退出房间，SDK内部在退出房间的时候会自动停止推拉流
        audienceListBinding.goBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                audienceListBinding.getRoot().setVisibility(View.GONE);
            }
        });
    }


    // 跳转到观众列表
    public void onCLickGetAudienceList(View view){

        // 邀请连麦
        if (ZGJoinLiveHelper.sharedInstance().getHasJoinedUsers().size() < ZGJoinLiveHelper.MaxJoinLiveNum) {
            audienceListBinding.getRoot().setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(JoinLiveAnchorUI.this, getString(R.string.join_live_count_overflow), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 供其他Activity调用，进入本专题的方法
     *
     * @param activity
     */
    public static void actionStart(Activity activity, String roomID) {
        Intent intent = new Intent(activity, JoinLiveAnchorUI.class);
        intent.putExtra("roomID",roomID);
        activity.startActivity(intent);
    }

    /**
     * 响应连麦请求
     */
    protected void handleJoinLiveRequest(final int seq, final String fromUserID) {
        // 有人请求连麦

        AlertDialog mDialogHandleRequestPublish = new AlertDialog.Builder(JoinLiveAnchorUI.this).setTitle(getString(R.string.hint))
                .setMessage(getString(R.string.someone_is_requesting_to_broadcast_allow, fromUserID))
                .setPositiveButton(getString(R.string.Allow), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 修改已连麦用户列表
                        if (ZGJoinLiveHelper.sharedInstance().getHasJoinedUsers().size() < ZGJoinLiveHelper.MaxJoinLiveNum) {
                            JoinLiveUserInfo userInfo = new JoinLiveUserInfo(fromUserID, "");
                            ZGJoinLiveHelper.sharedInstance().addJoinLiveAudience(userInfo);
                        }

                        // 同意连麦请求
                        ZGJoinLiveHelper.sharedInstance().respondJoinLiveRequest(seq, true);
                        AppLogger.getInstance().i(JoinLiveAnchorUI.class, "同意 %s 连麦", fromUserID);

                        dialog.dismiss();
                    }
                }).setNegativeButton(getString(R.string.Deny), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 拒绝连麦请求
                        ZGJoinLiveHelper.sharedInstance().respondJoinLiveRequest(seq, false);
                        AppLogger.getInstance().i(JoinLiveAnchorUI.class, "拒绝 %s 连麦", fromUserID);

                        dialog.dismiss();
                    }
                }).create();
        mDialogHandleRequestPublish.setCancelable(false);
        mDialogHandleRequestPublish.show();
    }

    public void initSDKCallback(){
        // 设置房间人数相关信息的回调监听
        ZGBaseHelper.sharedInstance().getZegoLiveRoom().setZegoIMCallback(new IZegoIMCallback() {
            @Override
            public void onUserUpdate(ZegoUserState[] listUser, int updateType) {
                AppLogger.getInstance().i(JoinLiveAnchorUI.class, "收到房间成员更新通知");

                // 房间成员更新回调
                for (ZegoUserState userInfo:listUser){

                    if (ZegoIM.UserUpdateFlag.Added == userInfo.updateFlag){
                        // 房间增加成员
                        ZGJoinLiveHelper.sharedInstance().addAudience(userInfo.userID);
                    } else if (ZegoIM.UserUpdateFlag.Deleted == userInfo.updateFlag){
                        // 成员退出房间
                        ZGJoinLiveHelper.sharedInstance().removeAudience(userInfo.userID);
                    }

                    // 为界面上的观众列表添加数据
                    audienceListAdapter.addAudienceInfo(ZGJoinLiveHelper.sharedInstance().getAudienceList());
                }

            }

            @Override
            public void onRecvRoomMessage(String s, ZegoRoomMessage[] zegoRoomMessages) {

            }

            @Override
            public void onRecvConversationMessage(String s, String s1, ZegoConversationMessage zegoConversationMessage) {

            }

            @Override
            public void onUpdateOnlineCount(String s, int i) {

            }

            @Override
            public void onRecvBigRoomMessage(String s, ZegoBigRoomMessage[] zegoBigRoomMessages) {

            }
        });

        // 设置房间回调监听
        ZGBaseHelper.sharedInstance().getZegoLiveRoom().setZegoRoomCallback(new IZegoRoomCallback() {
            @Override
            public void onKickOut(int reason, String roomID) {

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

                if (roomID.equals(mRoomID)){

                    for (ZegoStreamInfo streamInfo : zegoStreamInfos) {
                        // 当有流新增的时候，拉流
                        if (type == ZegoConstants.StreamUpdateType.Added) {
                            AppLogger.getInstance().i(JoinLiveAnchorUI.class, "房间: %s 内收到流新增通知. streamID : %s, userName : %s, extraInfo : %s", roomID, streamInfo.streamID, streamInfo.userName, streamInfo.extraInfo);

                            for (JoinLiveUserInfo userInfo:ZGJoinLiveHelper.sharedInstance().getHasJoinedUsers()) {
                                if (userInfo.userID.equals(streamInfo.userID)){
                                    // 设置连麦者的推流流名
                                    userInfo.streamID = streamInfo.streamID;
                                    // 修改连麦者的信息
                                    ZGJoinLiveHelper.sharedInstance().modifyJoinLiveUserInfo(userInfo);

                                    // 获取可用的视图
                                    JoinLiveView freeView = ZGJoinLiveHelper.sharedInstance().getFreeTextureView();

                                    if (freeView != null){
                                        // 拉流
                                        ZGPlayHelper.sharedInstance().startPlaying(streamInfo.streamID, freeView.textureView);
                                        ZGConfigHelper.sharedInstance().setPlayViewMode(ZegoVideoViewMode.ScaleAspectFill, streamInfo.streamID);

                                        // 修改视图信息
                                        freeView.streamID = streamInfo.streamID;
                                        ZGJoinLiveHelper.sharedInstance().modifyTextureViewInfo(freeView);
                                    } else {
                                        Toast.makeText(JoinLiveAnchorUI.this, getString(R.string.has_no_textureView), Toast.LENGTH_LONG).show();
                                    }

                                    break;
                                }
                            }
                        }
                        // 当有其他流关闭的时候，停止拉流
                        else if (type == ZegoConstants.StreamUpdateType.Deleted) {
                            AppLogger.getInstance().i(JoinLiveAnchorUI.class, "房间：%s 内收到流删除通知. streamID : %s, userName : %s, extraInfo : %s", roomID, streamInfo.streamID, streamInfo.userName, streamInfo.extraInfo);
                            for (JoinLiveUserInfo userInfo:ZGJoinLiveHelper.sharedInstance().getHasJoinedUsers()){
                                if (userInfo.userID.equals(streamInfo.userID)){
                                    // 停止拉流
                                    ZGPlayHelper.sharedInstance().stopPlaying(streamInfo.streamID);
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
        ZGPublishHelper.sharedInstance().setPublisherCallback(new IZegoLivePublisherCallback() {
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

                    // 设置视图为可用
                    ZGJoinLiveHelper.sharedInstance().setJoinLiveViewFree(streamID);
                }
            }

            @Override
            public void onJoinLiveRequest(int seq, String fromUserID, String fromUserName, String roomID) {
                /**
                 * 房间内有人申请加入连麦时会回调该方法
                 * 观众端通过 {@link com.zego.zegoliveroom.ZegoLiveRoom#requestJoinLive(IZegoResponseCallback)}
                 *  方法申请加入连麦
                 */
                AppLogger.getInstance().i(JoinLiveAnchorUI.class, "房间 %s 收到观众 %s 申请连麦请求", roomID, fromUserID);

                // 处理观众连麦请求
                handleJoinLiveRequest(seq, fromUserID);
            }

            @Override
            public void onPublishQualityUpdate(String s, ZegoPublishStreamQuality zegoPublishStreamQuality) {
                /**
                 * 推流质量更新, 回调频率默认3秒一次
                 * 可通过 {@link com.zego.zegoliveroom.ZegoLiveRoom#setPublishQualityMonitorCycle(long)} 修改回调频率
                 */
            }

            @Override
            public AuxData onAuxCallback(int i) {
                // aux混音，可以将外部音乐混进推流中。类似于直播中添加伴奏，掌声等音效
                // 另外还能用于ktv场景中的伴奏播放
                // 想深入了解可以进入进阶功能中的-mixing。
                // <a>https://doc.zego.im/CN/253.html</a> 文档中有说明
                return null;
            }

            @Override
            public void onCaptureVideoSizeChangedTo(int width, int height) {

            }

            @Override
            public void onMixStreamConfigUpdate(int i, String s, HashMap<String, Object> hashMap) {
                // 混流配置更新时会回调该方法。
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

        ZGPlayHelper.sharedInstance().setPlayerCallback(new IZegoLivePlayerCallback() {
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

                    // 解除视图占用
                    ZGJoinLiveHelper.sharedInstance().setJoinLiveViewFree(streamID);
                    // 移除连麦者
                    for (JoinLiveUserInfo userInfo: ZGJoinLiveHelper.sharedInstance().getHasJoinedUsers()) {
                        if (userInfo.streamID.equals(streamID)) {
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
    }

    // 去除SDK相关的回调监听
    public void releaseSDKCallback(){
        ZGPublishHelper.sharedInstance().releasePublisherCallback();
        ZGPlayHelper.sharedInstance().releasePlayerCallback();
        ZGBaseHelper.sharedInstance().releaseZegoRoomCallback();
         ZGBaseHelper.sharedInstance().getZegoLiveRoom().setZegoIMCallback(null);
    }

    // 设置连麦相关的回调监听
    public void initJoinLiveCallback(){
        ZGJoinLiveHelper.sharedInstance().setJoinLiveCallback(new ZGJoinLiveHelper.JoinLiveCallback() {
            @Override
            public void requestJoinLiveResult(boolean isSuccess, String fromUserID) {
                // 请求连麦者才会收到此回调
            }

            @Override
            public void inviteJoinLiveResult(boolean isSuccess, String fromUserID) {
                AppLogger.getInstance().i(JoinLiveAnchorUI.class, "邀请 %s 连麦结果：%d", fromUserID, isSuccess?1:0);

                // 邀请连麦者才会收到此回调
                if (isSuccess){
                    // 修改已连麦用户列表
                    if (ZGJoinLiveHelper.sharedInstance().getHasJoinedUsers().size() < ZGJoinLiveHelper.MaxJoinLiveNum) {
                        JoinLiveUserInfo userInfo = new JoinLiveUserInfo(fromUserID, "");
                        ZGJoinLiveHelper.sharedInstance().addJoinLiveAudience(userInfo);

                        audienceListBinding.getRoot().setVisibility(View.GONE);
                    } else {
                        Toast.makeText(JoinLiveAnchorUI.this, R.string.join_live_count_overflow, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(JoinLiveAnchorUI.this, R.string.audience_deny_join_live, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void endJoinLiveResult(boolean isSuccess, String userID, String roomID) {
                AppLogger.getInstance().i(JoinLiveAnchorUI.class, "结束房间：%s 内 %s 连麦的结果：%d", roomID, userID, isSuccess?1:0);

                // 主播端调用结束连麦才会收到此回调

                if (isSuccess && roomID.equals(mRoomID)){
                    // 停止拉该用户的流
                    for (JoinLiveUserInfo userInfo:ZGJoinLiveHelper.sharedInstance().getHasJoinedUsers()){
                        if (userInfo.userID.equals(userID)){
                            ZGPlayHelper.sharedInstance().stopPlaying(userInfo.streamID);
                            // 从已连麦列表移除此连麦者
                            ZGJoinLiveHelper.sharedInstance().removeJoinLiveAudience(userInfo);
                            // 修改视图信息
                            ZGJoinLiveHelper.sharedInstance().setJoinLiveViewFree(userInfo.streamID);

                            break;
                        }
                    }

                    // 修改button的标识为 邀请连麦
                    binding.btnInviteJoinLive.setText(getString(R.string.tx_invite_joinLive));
                }
            }
        });
    }

    // 去除连麦相关的回调监听
    public void releaseJoinLiveCallback(){
        ZGJoinLiveHelper.sharedInstance().setJoinLiveCallback(null);
    }
}
