package com.zego.sound.processing.ui;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.zego.common.ZGBaseHelper;
import com.zego.common.ZGConfigHelper;
import com.zego.common.ZGPlayHelper;
import com.zego.common.ZGPublishHelper;
import com.zego.common.entity.SDKConfigInfo;
import com.zego.common.entity.StreamQuality;
import com.zego.common.ui.BaseActivity;
import com.zego.common.ui.WebActivity;
import com.zego.common.util.AppLogger;
import com.zego.common.widgets.CustomDialog;
import com.zego.sound.processing.R;
import com.zego.sound.processing.adapter.SoundEffectViewAdapter;
import com.zego.sound.processing.databinding.ActivitySoundProcessPlayBinding;
import com.zego.sound.processing.databinding.ActivitySoundProcessPublishBinding;
import com.zego.sound.processing.databinding.InputRoomIdLayoutBinding;
import com.zego.sound.processing.view.SoundEffectDialog;
import com.zego.zegoavkit2.audioprocessing.ZegoAudioReverbMode;
import com.zego.zegoliveroom.callback.IZegoLivePlayerCallback;
import com.zego.zegoliveroom.callback.IZegoLivePublisherCallback;
import com.zego.zegoliveroom.callback.IZegoLoginCompletionCallback;
import com.zego.zegoliveroom.callback.IZegoResponseCallback;
import com.zego.zegoliveroom.callback.IZegoRoomCallback;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.entity.AuxData;
import com.zego.zegoliveroom.entity.ZegoPlayStreamQuality;
import com.zego.zegoliveroom.entity.ZegoPublishStreamQuality;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;

import java.util.HashMap;

/**
 * Created by zego on 2019/4/22.
 *
 * 变声/混响/立体声 拉流页面，方便开发者听到的变声效果
 */

public class SoundProcessPlayUI extends BaseActivity {


    private ActivitySoundProcessPlayBinding binding;

    private String mStreamID, mRoomID;
    private StreamQuality streamQuality = new StreamQuality();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sound_process_play);
        // 利用DataBinding 可以通过bean类驱动UI变化。
        // 方便快捷避免需要写一大堆 setText 等一大堆臃肿的代码。
        binding.setQuality(streamQuality);
        mStreamID = getIntent().getStringExtra("streamID");
        mRoomID =  getIntent().getStringExtra("roomID");

        streamQuality.setRoomID(String.format("RoomID : %s", mRoomID));

        ZGPlayHelper.sharedInstance().setPlayerCallback(new IZegoLivePlayerCallback() {
            @Override
            public void onPlayStateUpdate(int errorCode, String streamID) {
                // 拉流状态更新，errorCode 非0 则说明拉流失败
                // 拉流常见错误码请看文档: <a>https://doc.zego.im/CN/491.html</a>

                if (errorCode == 0) {
                    AppLogger.getInstance().i(ZGPublishHelper.class, "拉流成功, streamID : %s", streamID);
                    Toast.makeText(SoundProcessPlayUI.this, getString(com.zego.common.R.string.tx_play_success), Toast.LENGTH_SHORT).show();

                    // 修改标题状态拉流成功状态
                    binding.title.setTitleName(getString(com.zego.common.R.string.tx_playing));
                } else {
                    // 修改标题状态拉流失败状态
                    binding.title.setTitleName(getString(com.zego.common.R.string.tx_play_fail));

                    AppLogger.getInstance().i(ZGPublishHelper.class, "拉流失败, streamID : %s, errorCode : %d", streamID, errorCode);
                    Toast.makeText(SoundProcessPlayUI.this, getString(com.zego.common.R.string.tx_play_fail), Toast.LENGTH_SHORT).show();
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


        ZGBaseHelper.sharedInstance().setZegoRoomCallback(new IZegoRoomCallback() {
            @Override
            public void onKickOut(int reason, String roomID, String customReason) {

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
                            Toast.makeText(SoundProcessPlayUI.this, com.zego.common.R.string.tx_current_stream_delete, Toast.LENGTH_LONG).show();
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

        startPlay();

    }

    @Override
    protected void onDestroy() {

        // 清空代理设置
        ZGPlayHelper.sharedInstance().setPlayerCallback(null);

        // 停止所有的推流和拉流后，才能执行 logoutRoom
        if (mStreamID != null) {
            ZGPlayHelper.sharedInstance().stopPlaying(mStreamID);
        }

        // 当用户退出界面时退出登录房间
        ZGBaseHelper.sharedInstance().loginOutRoom();
        super.onDestroy();
    }

    /**
     * 开始拉流
     */
    public void startPlay() {

        CustomDialog.createDialog("登录房间中...", this).show();
        // 开始拉流前需要先登录房间
        ZGBaseHelper.sharedInstance().loginRoom(mRoomID, ZegoConstants.RoomRole.Audience, new IZegoLoginCompletionCallback() {
            @Override
            public void onLoginCompletion(int errorCode, ZegoStreamInfo[] zegoStreamInfos) {
                // 关闭dialog
                CustomDialog.createDialog(SoundProcessPlayUI.this).cancel();
                if (errorCode == 0) {
                    AppLogger.getInstance().i(SoundProcessPlayUI.class, "登陆房间成功 roomId : %s", mRoomID);

                    // 开始拉流
                    ZGPlayHelper.sharedInstance().startPlaying(mStreamID, binding.playView);
                } else {
                    AppLogger.getInstance().i(SoundProcessPlayUI.class, "登陆房间失败, errorCode : %d", errorCode);
                    Toast.makeText(SoundProcessPlayUI.this, getString(com.zego.common.R.string.tx_login_room_failure), Toast.LENGTH_SHORT).show();
                }
            }
        });

    }



    public static void actionStart(Activity activity, String roomID, String streamID) {
        Intent intent = new Intent(activity, SoundProcessPlayUI.class);
        intent.putExtra("roomID", roomID);
        intent.putExtra("streamID", streamID);
        activity.startActivity(intent);
    }
}
