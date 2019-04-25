package com.zego.play.ui;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.zego.common.ui.WebActivity;
import com.zego.common.widgets.TitleLayout;
import com.zego.play.R;
import com.zego.play.databinding.PlayInputStreamIdLayoutBinding;
import com.zego.common.entity.SDKConfigInfo;
import com.zego.common.entity.StreamQuality;
import com.zego.play.databinding.ActivityPlayBinding;
import com.zego.common.ZGBaseHelper;
import com.zego.common.ZGConfigHelper;
import com.zego.common.ZGPlayHelper;
import com.zego.common.ZGPublishHelper;
import com.zego.common.constants.ZGLiveRoomConstants;
import com.zego.common.ui.BaseActivity;
import com.zego.common.util.AppLogger;
import com.zego.zegoliveroom.callback.IZegoLivePlayerCallback;
import com.zego.zegoliveroom.entity.ZegoPlayStreamQuality;

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
                // 拉流状态更新，errorCode 非0 则说明拉流成功
                // 拉流常见错误码请看文档: <a>https://doc.zego.im/CN/491.html</a>

                if (errorCode == 0) {
                    mStreamID = streamID;
                    AppLogger.getInstance().i(ZGPublishHelper.class, "拉流成功, streamID : %s", streamID);
                    Toast.makeText(PlayActivityUI.this, getString(com.zego.common.R.string.tx_play_success), Toast.LENGTH_SHORT).show();

                    // 修改标题状态拉流成功状态
                    binding.title.setTitleName(getString(com.zego.common.R.string.tx_play_success));
                } else {
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
        if (!"".equals(streamID)) {
            // 隐藏输入StreamID布局
            hideInputStreamIDLayout();
            // 更新界面上流名
            streamQuality.setStreamID(String.format("StreamID : %s", streamID));

            // 开始拉流
            ZGPlayHelper.sharedInstance().startPlaying(streamID, binding.playView);

        } else {
                AppLogger.getInstance().i(PlayActivityUI.class, getString(com.zego.common.R.string.tx_stream_id_cannot_null));
                Toast.makeText(this, getString(com.zego.common.R.string.tx_stream_id_cannot_null), Toast.LENGTH_LONG).show();

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