package com.zego.joinlive.ui;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import com.zego.joinlive.constants.JoinLiveUserInfo;
import com.zego.joinlive.constants.JoinLiveView;
import com.zego.joinlive.databinding.ActivityJoinLiveAudienceBinding;
import com.zego.support.RoomInfo;
import com.zego.zegoliveroom.callback.IZegoLivePlayerCallback;
import com.zego.zegoliveroom.callback.IZegoLivePublisherCallback;
import com.zego.zegoliveroom.callback.IZegoLoginCompletionCallback;
import com.zego.zegoliveroom.callback.IZegoRoomCallback;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.constants.ZegoVideoViewMode;
import com.zego.zegoliveroom.entity.AuxData;
import com.zego.zegoliveroom.entity.ZegoPlayStreamQuality;
import com.zego.zegoliveroom.entity.ZegoPublishStreamQuality;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;

import java.util.ArrayList;
import java.util.HashMap;

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

        // 设置拉流的视图列表
        initViewList();
        // 设置SDK相关的回调监听
        initSDKCallback();
        // 设置连麦相关的回调监听
        initJoinLiveCallback();

        // 登录房间并拉流
        startPlay();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 停止正在拉的流
        if (mPlayStreamIDs.size() > 0) {
            for (String streamID : mPlayStreamIDs) {
                ZGPlayHelper.sharedInstance().stopPlaying(streamID);
            }
        }

        // 清空拉流列表
        mPlayStreamIDs.clear();

        // 停止推流
        if (isJoinedLive) {
            ZGPublishHelper.sharedInstance().stopPublishing();
            ZGPublishHelper.sharedInstance().stopPreviewView();
        }
        // 设置所有视图可用
        ZGJoinLiveHelper.sharedInstance().freeAllJoinLiveView();

        // 退出房间
        ZGBaseHelper.sharedInstance().loginOutRoom();
        // 去除SDK相关的回调监听
        releaseSDKCallback();
        // 去除连麦相关的回调监听
        releaseJoinLiveCallback();
    }

    // 设置拉流的视图列表
    protected void initViewList(){

        mBigView = new JoinLiveView(binding.playView, null, false, "");
        mBigView.setZegoLiveRoom(ZGBaseHelper.sharedInstance().getZegoLiveRoom());

        // 添加可用的连麦者视图
        ArrayList<JoinLiveView> mJoinLiveView = new ArrayList<>();
        final JoinLiveView view1 = new JoinLiveView(binding.audienceViewOne, null, false, "");
        view1.setZegoLiveRoom(ZGBaseHelper.sharedInstance().getZegoLiveRoom());
        final JoinLiveView view2 = new JoinLiveView(binding.audienceViewTwo, null, false, "");
        view2.setZegoLiveRoom(ZGBaseHelper.sharedInstance().getZegoLiveRoom());
        final JoinLiveView view3 = new JoinLiveView(binding.audienceViewThree, null, false, "");
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

    // 申请连麦
    public void onClickApplyJoinLive(View view){

        if (binding.btnApplyJoinLive.getText().toString().equals(getString(R.string.tx_apply_joinLive))){
            // 申请连麦

            // 当前已连麦者总数是否超过上限，此demo支持展示三人连麦
            boolean isJoinedLiveUsersCountOverFlow = false;
            // 达到连麦上限时的拉流总数 = 1条主播流 + 三条连麦者的流
            if (mPlayStreamIDs.size() == ZGJoinLiveHelper.MaxJoinLiveNum+1){
                isJoinedLiveUsersCountOverFlow = true;
            }

            if (isJoinedLive) {
                // 判断是否已连麦
                Toast.makeText(JoinLiveAudienceUI.this, getString(R.string.has_joined_live), Toast.LENGTH_SHORT).show();
            } else if (isJoinedLiveUsersCountOverFlow){
                // 判断连麦人数是否达到上限
                Toast.makeText(JoinLiveAudienceUI.this, getString(R.string.join_live_count_overflow), Toast.LENGTH_SHORT).show();
            } else {
                // 不满足上述两种情况则申请连麦
                ZGJoinLiveHelper.sharedInstance().requestJoinLive();
                Toast.makeText(JoinLiveAudienceUI.this, getString(R.string.has_sended_applation), Toast.LENGTH_SHORT).show();

                AppLogger.getInstance().i(JoinLiveAudienceUI.class, "观众发出连麦申请");
            }
        } else {
            // 结束连麦
            if (isJoinedLive){
                ZGPublishHelper.sharedInstance().stopPreviewView();
                ZGPublishHelper.sharedInstance().stopPublishing();
                isJoinedLive = false;
                // 设置视图可用
                ZGJoinLiveHelper.sharedInstance().setJoinLiveViewFree(mPublishStreamID);
                // 修改button的标识为申请连麦
                binding.btnApplyJoinLive.setText(getString(R.string.tx_apply_joinLive));

                AppLogger.getInstance().i(JoinLiveAudienceUI.class, "观众结束连麦");
            }
        }
    }

    // 登录房间并拉流
    public void startPlay(){
        AppLogger.getInstance().i(JoinLiveAudienceUI.class, "登录房间 %s", mRoomID);
        // 防止用户点击，弹出加载对话框
        CustomDialog.createDialog("登录房间中...", this).show();
        // 开始拉流前需要先登录房间，此处是观众登录主播所在的房间
        ZGBaseHelper.sharedInstance().loginRoom(mRoomID, ZegoConstants.RoomRole.Audience, new IZegoLoginCompletionCallback() {
            @Override
            public void onLoginCompletion(int errorCode, ZegoStreamInfo[] zegoStreamInfos) {
                CustomDialog.createDialog(JoinLiveAudienceUI.this).cancel();
                if (errorCode == 0) {
                    AppLogger.getInstance().i(JoinLiveAudienceUI.class, "登录房间成功 roomId : %s", mRoomID);

                    // 开始拉流
                    for (ZegoStreamInfo streamInfo:zegoStreamInfos){

                        // 拉主播流
                        if (streamInfo.userID.equals(mAnchorID)){
                            // 主播流采用全页面的视图
                            ZGPlayHelper.sharedInstance().startPlaying(streamInfo.streamID, mBigView.textureView);
                            ZGConfigHelper.sharedInstance().setPlayViewMode(ZegoVideoViewMode.ScaleAspectFill, streamInfo.streamID);

                            // 修改视图信息
                            mBigView.streamID = streamInfo.streamID;
                            ZGJoinLiveHelper.sharedInstance().modifyTextureViewInfo(mBigView);

                            break;
                        }
                    }

                    for (ZegoStreamInfo streamInfo:zegoStreamInfos) {

                        // 拉其它连麦者的流
                        if (!streamInfo.userID.equals(mAnchorID)){
                            // 获取可用的视图
                            JoinLiveView freeView = ZGJoinLiveHelper.sharedInstance().getFreeTextureView();
                            if (freeView != null) {
                                ZGPlayHelper.sharedInstance().startPlaying(streamInfo.streamID, freeView.textureView);
                                ZGConfigHelper.sharedInstance().setPlayViewMode(ZegoVideoViewMode.ScaleAspectFill, streamInfo.streamID);

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
     * 供其他Activity调用，进入本专题的方法
     *
     * @param activity
     */
    public static void actionStart(Activity activity, String roomID, String anchorID) {
        Intent intent = new Intent(activity, JoinLiveAudienceUI.class);
        intent.putExtra("roomID", roomID);
        intent.putExtra("anchorID", anchorID);
        activity.startActivity(intent);
    }

    /**
     * 响应主播邀请连麦请求
     */
    protected void handleInvitedJoinLiveRequest(final int seq, final String fromUserID) {
        // 主播邀请连麦

        AlertDialog mDialogHandleRequestPublish = new AlertDialog.Builder(JoinLiveAudienceUI.this).setTitle(getString(R.string.hint))
                .setMessage(getString(R.string.someone_is_invitting_to_broadcast_allow, fromUserID))
                .setPositiveButton(getString(R.string.Allow), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 同意主播邀请连麦的请求

                        ZGJoinLiveHelper.sharedInstance().respondInvitedJoinLiveRequest(seq, true);

                        AppLogger.getInstance().i(JoinLiveAudienceUI.class, "接受主播连麦邀请，开始推流，流名：%s", mPublishStreamID);
                        // 获取可用的视图
                        JoinLiveView freeView = ZGJoinLiveHelper.sharedInstance().getFreeTextureView();

                        if (freeView != null) {
                            // 设置预览视图模式
                            ZGBaseHelper.sharedInstance().getZegoLiveRoom().setPreviewViewMode(ZegoVideoViewMode.ScaleAspectFill);
                            // 调用SDK开始预览接口 设置view 并启用预览
                            ZGPublishHelper.sharedInstance().startPreview(freeView.textureView);
                            // 开始推流
                            ZGPublishHelper.sharedInstance().startPublishing(mPublishStreamID, "audienceJoinLive", ZegoConstants.PublishFlag.JoinPublish);

                            // 修改视图信息
                            freeView.streamID = mPublishStreamID;
                            freeView.isPublishView = true;
                            ZGJoinLiveHelper.sharedInstance().modifyTextureViewInfo(freeView);
                        } else {
                            Toast.makeText(JoinLiveAudienceUI.this, getString(R.string.has_no_textureView), Toast.LENGTH_SHORT).show();
                        }

                        dialog.dismiss();
                    }
                }).setNegativeButton(getString(R.string.Deny), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 拒绝主播邀请连麦的请求
                        ZGJoinLiveHelper.sharedInstance().respondInvitedJoinLiveRequest(seq, false);
                        AppLogger.getInstance().i(JoinLiveAudienceUI.class, "拒绝主播连麦邀请");
                        dialog.dismiss();
                    }
                }).create();
        mDialogHandleRequestPublish.setCancelable(false);
        mDialogHandleRequestPublish.show();
    }

    // 设置 SDK 相关回调的监听
    public void initSDKCallback(){
        // 设置房间回调监听
        ZGBaseHelper.sharedInstance().setZegoRoomCallback(new IZegoRoomCallback() {
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
                                    // 拉流
                                    ZGPlayHelper.sharedInstance().startPlaying(streamInfo.streamID, freeView.textureView);
                                    ZGConfigHelper.sharedInstance().setPlayViewMode(ZegoVideoViewMode.ScaleAspectFill, streamInfo.streamID);

                                    // 修改视图信息
                                    freeView.streamID = streamInfo.streamID;
                                    ZGJoinLiveHelper.sharedInstance().modifyTextureViewInfo(freeView);
                                } else {
                                    // 拉流
                                    ZGPlayHelper.sharedInstance().startPlaying(streamInfo.streamID, mBigView.textureView);
                                    ZGConfigHelper.sharedInstance().setPlayViewMode(ZegoVideoViewMode.ScaleAspectFill, streamInfo.streamID);

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
                                    ZGPlayHelper.sharedInstance().stopPlaying(streamInfo.streamID);
                                    mPlayStreamIDs.remove(streamInfo.streamID);

                                    // 修改视图信息
                                    ZGJoinLiveHelper.sharedInstance().setJoinLiveViewFree(streamInfo.streamID);

                                    if (streamInfo.userID.equals(mAnchorID) && isJoinedLive){

                                        // 主播停止推流，结束连麦
                                        ZGJoinLiveHelper.sharedInstance().handleEndJoinLiveCommand();
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
        ZGPlayHelper.sharedInstance().setPlayerCallback(new IZegoLivePlayerCallback() {
            @Override
            public void onPlayStateUpdate(int stateCode, String streamID) {
                // 拉流状态更新，errorCode 非0 则说明拉流失败
                // 拉流常见错误码请看文档: <a>https://doc.zego.im/CN/491.html</a>

                if (stateCode == 0) {
                    AppLogger.getInstance().i(JoinLiveAudienceUI.class, "拉流成功, streamID : %s", streamID);
                    Toast.makeText(JoinLiveAudienceUI.this, getString(com.zego.common.R.string.tx_play_success), Toast.LENGTH_SHORT).show();

                    // 向拉流流名列表中添加流名
                    mPlayStreamIDs.add(streamID);

                } else {
                    AppLogger.getInstance().i(JoinLiveAudienceUI.class, "拉流失败, streamID : %s, errorCode : %d", streamID, stateCode);
                    Toast.makeText(JoinLiveAudienceUI.this, getString(com.zego.common.R.string.tx_play_fail), Toast.LENGTH_SHORT).show();

                    // 解除视图占用
                    ZGJoinLiveHelper.sharedInstance().setJoinLiveViewFree(streamID);
                }
            }

            @Override
            public void onPlayQualityUpdate(String streamID, ZegoPlayStreamQuality zegoPlayStreamQuality) {

            }

            @Override
            public void onInviteJoinLiveRequest(int seq, String fromUserID, String fromUserName, String roomID) {
                // 收到主播邀请连麦的回调
                AppLogger.getInstance().i(JoinLiveAudienceUI.class, "所在 %s 房间收到主播 %s 的连麦邀请", roomID, fromUserID);
                // 处理主播的连麦邀请
                handleInvitedJoinLiveRequest(seq, fromUserID);
            }

            @Override
            public void onRecvEndJoinLiveCommand(String fromUserID, String fromUserName, String roomID ) {
                // 收到主播结束连麦的回调
                AppLogger.getInstance().i(JoinLiveAudienceUI.class, "所在 %s 房间收到主播 %s 结束连麦的指令，停止推流", roomID, fromUserID);

                if (fromUserID.equals(mAnchorID) && roomID.equals(mRoomID)) {

                    // 停止推流
                    ZGJoinLiveHelper.sharedInstance().handleEndJoinLiveCommand();

                    isJoinedLive = false;
                    // 解除视图占用
                    ZGJoinLiveHelper.sharedInstance().setJoinLiveViewFree(mPublishStreamID);
                    // 修改button的标识为申请连麦
                    binding.btnApplyJoinLive.setText(getString(R.string.tx_apply_joinLive));
                }
            }

            @Override
            public void onVideoSizeChangedTo(String streamID, int i, int i1) {

            }
        });

        // 设置推流回调监听
        ZGPublishHelper.sharedInstance().setPublisherCallback(new IZegoLivePublisherCallback() {
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

            }
        });
    }

    // 去除SDK相关的回调监听
    public void releaseSDKCallback(){
        ZGPublishHelper.sharedInstance().releasePublisherCallback();
        ZGPlayHelper.sharedInstance().releasePlayerCallback();
        ZGBaseHelper.sharedInstance().releaseZegoRoomCallback();
    }

    // 设置连麦相关的回调监听
    public void initJoinLiveCallback(){
        ZGJoinLiveHelper.sharedInstance().setJoinLiveCallback(new ZGJoinLiveHelper.JoinLiveCallback() {
            @Override
            public void requestJoinLiveResult(boolean isSuccess, String fromUserID) {
                // 请求连麦者才会收到此回调

                AppLogger.getInstance().i(JoinLiveAudienceUI.class, "主播 %s 同意连麦，开始推流", fromUserID);

                if (isSuccess && fromUserID.equals(mAnchorID)){
                    // 主播同意连麦，开始推流

                    Toast.makeText(JoinLiveAudienceUI.this, getString(R.string.request_join_live_success), Toast.LENGTH_LONG).show();

                    // 获取可用的视图
                    JoinLiveView freeView = ZGJoinLiveHelper.sharedInstance().getFreeTextureView();

                    if (freeView != null){
                        // 设置预览视图模式
                        ZGBaseHelper.sharedInstance().getZegoLiveRoom().setPreviewViewMode(ZegoVideoViewMode.ScaleAspectFill);
                        // 调用sdk 开始预览接口 设置view 并启用预览
                        ZGPublishHelper.sharedInstance().startPreview(freeView.textureView);
                        // 开始推流
                        ZGPublishHelper.sharedInstance().startPublishing(mPublishStreamID, "audienceJoinLive", ZegoConstants.PublishFlag.JoinPublish);

                        // 修改视图信息
                        freeView.streamID = mPublishStreamID;
                        freeView.isPublishView = true;
                        ZGJoinLiveHelper.sharedInstance().modifyTextureViewInfo(freeView);
                    } else {
                        Toast.makeText(JoinLiveAudienceUI.this, getString(R.string.has_no_textureView), Toast.LENGTH_LONG).show();
                    }
                } else {
                    AppLogger.getInstance().i(JoinLiveAudienceUI.class, "主播 %s 拒绝连麦，开始推流", fromUserID);
                    Toast.makeText(JoinLiveAudienceUI.this, getString(R.string.request_has_been_denied), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void inviteJoinLiveResult(boolean isSuccess, String fromUserID) {
                // 邀请连麦者才会收到此回调

            }

            @Override
            public void endJoinLiveResult(boolean isSuccess, String userID, String roomID) {
                // 主播端调用结束连麦才会收到此回调

            }
        });
    }

    // 去除连麦相关的回调监听
    public void releaseJoinLiveCallback(){
        ZGJoinLiveHelper.sharedInstance().setJoinLiveCallback(null);
    }
}
