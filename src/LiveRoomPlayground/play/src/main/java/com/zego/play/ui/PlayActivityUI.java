package com.zego.play.ui;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.zego.common.ui.WebActivity;
import com.zego.play.R;
import com.zego.play.databinding.PlayInputStreamIdLayoutBinding;
import com.zego.common.entity.SDKConfigInfo;
import com.zego.common.entity.StreamQuality;
import com.zego.play.databinding.ActivityPlayBinding;
import com.zego.common.ZGBaseHelper;
import com.zego.common.ZGConfigHelper;
import com.zego.common.ZGPlayHelper;
import com.zego.common.ZGPublishHelper;
import com.zego.common.ui.BaseActivity;
import com.zego.common.util.AppLogger;
import com.zego.zegoliveroom.callback.IZegoLivePlayerCallback;
import com.zego.zegoliveroom.callback.IZegoRoomCallback;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.entity.ZegoPlayStreamQuality;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;

public class PlayActivityUI extends BaseActivity {


    private ActivityPlayBinding binding;
    private PlayInputStreamIdLayoutBinding layoutBinding;
    private String mStreamID;
    private StreamQuality streamQuality = new StreamQuality();
    private SDKConfigInfo sdkConfigInfo = new SDKConfigInfo();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_play);
        layoutBinding = binding.layout;
        layoutBinding.startButton.setText(getString(com.zego.common.R.string.tx_start_play));
        // 利用DataBinding 可以通过bean类驱动UI变化。
        // 方便快捷避免需要写一大堆 setText 等一大堆臃肿的代码。
        binding.setQuality(streamQuality);
        binding.setConfig(sdkConfigInfo);

        streamQuality.setRoomID(String.format("RoomID : %s", getIntent().getStringExtra("roomID")));

        ZGPlayHelper.sharedInstance().setPlayerCallback(new IZegoLivePlayerCallback() {
            @Override
            public void onPlayStateUpdate(int errorCode, String streamID) {
                // 拉流状态更新，errorCode 非0 则说明拉流失败
                // 拉流常见错误码请看文档: <a>https://doc.zego.im/CN/491.html</a>

                if (errorCode == 0) {
                    mStreamID = streamID;
                    AppLogger.getInstance().i(ZGPublishHelper.class, "拉流成功, streamID : %s", streamID);
                    Toast.makeText(PlayActivityUI.this, getString(com.zego.common.R.string.tx_play_success), Toast.LENGTH_SHORT).show();

                    // 修改标题状态拉流成功状态
                    binding.title.setTitleName(getString(com.zego.common.R.string.tx_playing));
                } else {
                    // 当拉流失败 当前 mStreamID 初始化成 null 值
                    mStreamID = null;
                    // 修改标题状态拉流失败状态
                    binding.title.setTitleName(getString(com.zego.common.R.string.tx_play_fail));
                    AppLogger.getInstance().i(ZGPublishHelper.class, "拉流失败, streamID : %s, errorCode : %d", streamID, errorCode);
                    Toast.makeText(PlayActivityUI.this, getString(com.zego.common.R.string.tx_play_fail), Toast.LENGTH_SHORT).show();
                    // 当拉流失败时需要显示布局
                    showInputStreamIDLayout();
                }
            }

            @Override
            public void onPlayQualityUpdate(String s, ZegoPlayStreamQuality zegoPlayStreamQuality) {
                /**
                 * 拉流质量更新, 回调频率默认3秒一次
                 * 可通过 {@link com.zego.zegoliveroom.ZegoLiveRoom#setPlayQualityMonitorCycle(long)} 修改回调频率
                 */
                streamQuality.setFps(String.format("帧率: %f", zegoPlayStreamQuality.vdjFps));
                streamQuality.setBitrate(String.format("码率: %f kbs", zegoPlayStreamQuality.vkbps));
            }

            @Override
            public void onInviteJoinLiveRequest(int seq, String fromUserID, String fromUserName, String roomID) {
                // 观众收到主播的连麦邀请
                // fromUserID 为主播用户id
                // fromUserName 为主播昵称
                // roomID 为房间号。
                // 开发者想要深入了解连麦业务请参考文档: <a>https://doc.zego.im/CN/224.html</a>
            }

            @Override
            public void onRecvEndJoinLiveCommand(String fromUserID, String fromUserName, String roomID) {
                // 连麦观众收到主播的结束连麦信令。
                // fromUserID 为主播用户id
                // fromUserName 为主播昵称
                // roomID 为房间号。
                // 开发者想要深入了解连麦业务请参考文档: <a>https://doc.zego.im/CN/224.html</a>
            }

            @Override
            public void onVideoSizeChangedTo(String streamID, int width, int height) {
                // 视频宽高变化通知,startPlay后，如果视频宽度或者高度发生变化(首次的值也会)，则收到该通知.
                streamQuality.setResolution(String.format("分辨率: %dX%d", width, height));
            }
        });


        binding.swSpeaker.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.isPressed()) {
                    sdkConfigInfo.setSpeaker(isChecked);
                    ZGConfigHelper.sharedInstance().enableSpeaker(isChecked);
                }
            }
        });
        binding.swMirror.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.isPressed()) {
                    sdkConfigInfo.setEnableMirror(isChecked);
                    ZGConfigHelper.sharedInstance().enablePlayMirror(isChecked, mStreamID);
                }
            }
        });
        // 设置SDK 房间代理回调。业务侧希望检查当前房间有流更新了，会去自动重新拉流。
        ZGBaseHelper.sharedInstance().setZegoRoomCallback(new IZegoRoomCallback() {
            @Override
            public void onKickOut(int reason, String roomID, String customReason) {

            }

            @Override
            public void onDisconnect(int i, String s) {
                binding.title.setTitleName("房间与server断开连接");
            }

            @Override
            public void onReconnect(int i, String s) {

            }

            @Override
            public void onTempBroken(int i, String s) {

            }

            @Override
            public void onStreamUpdated(int type, ZegoStreamInfo[] zegoStreamInfos, String s) {
                // 当登陆房间成功后，如果房间内中途有人推流或停止推流。房间内其他人就能通过该回调收到流更新通知。
                for (ZegoStreamInfo streamInfo : zegoStreamInfos) {
                    if (streamInfo.streamID.equals(mStreamID)) {
                        if (type == ZegoConstants.StreamUpdateType.Added) {
                            // 当收到房间流新增的时候, 重新拉流
                            ZGPlayHelper.sharedInstance().startPlaying(mStreamID, binding.playView);
                        } else if (type == ZegoConstants.StreamUpdateType.Deleted) {
                            // 当收到房间流删除的时候停止拉流
                            ZGPlayHelper.sharedInstance().stopPlaying(mStreamID);
                            Toast.makeText(PlayActivityUI.this, com.zego.common.R.string.tx_current_stream_delete, Toast.LENGTH_LONG).show();
                            // 修改标题状态拉流成功状态
                            binding.title.setTitleName(getString(com.zego.common.R.string.tx_current_stream_delete));
                        }
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

    }

    /**
     * Button点击事件, 跳转官网示例代码链接
     *
     * @param view
     */
    public void goCodeDemo(View view) {
        WebActivity.actionStart(this, "https://doc.zego.im/CN/217.html", getString(com.zego.common.R.string.tx_play_guide));

    }

    @Override
    protected void onDestroy() {

        // 停止所有的推流和拉流后，才能执行 logoutRoom
        if (mStreamID != null) {
            ZGPlayHelper.sharedInstance().stopPlaying(mStreamID);
        }

        // 当用户退出界面时退出登录房间
        ZGBaseHelper.sharedInstance().loginOutRoom();
        super.onDestroy();
    }

    /**
     * button 点击事件触发
     * 开始拉流
     *
     * @param view
     */
    public void onStart(View view) {
        String streamID = layoutBinding.edStreamId.getText().toString();
        // 隐藏输入StreamID布局
        hideInputStreamIDLayout();
        // 更新界面上流名
        streamQuality.setStreamID(String.format("StreamID : %s", streamID));
        // 开始拉流
        boolean isPlaySuccess = ZGPlayHelper.sharedInstance().startPlaying(streamID, binding.playView);
        if (!isPlaySuccess) {
            AppLogger.getInstance().i(ZGPublishHelper.class, "拉流失败, streamID : %s", streamID);
            Toast.makeText(PlayActivityUI.this, getString(com.zego.common.R.string.tx_play_fail), Toast.LENGTH_SHORT).show();
            // 修改标题状态拉流失败状态
            binding.title.setTitleName(getString(com.zego.common.R.string.tx_play_fail));
        }
    }

    /**
     * 跳转到常用界面
     *
     * @param view
     */
    public void goSetting(View view) {
        PlaySettingActivityUI.actionStart(this, mStreamID);
    }


    private void hideInputStreamIDLayout() {
        // 隐藏InputStreamIDLayout布局
        layoutBinding.getRoot().setVisibility(View.GONE);
        binding.publishStateView.setVisibility(View.VISIBLE);
    }

    private void showInputStreamIDLayout() {
        // 显示InputStreamIDLayout布局
        layoutBinding.getRoot().setVisibility(View.VISIBLE);
        binding.publishStateView.setVisibility(View.GONE);
    }

    public static void actionStart(Activity activity, String roomID) {
        Intent intent = new Intent(activity, PlayActivityUI.class);
        intent.putExtra("roomID", roomID);
        activity.startActivity(intent);
    }
}
