package com.zego.audioplayer.ui;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.zego.audioplayer.R;
import com.zego.audioplayer.databinding.ActivityZgaudioPlayerPlayBinding;
import com.zego.common.ZGBaseHelper;
import com.zego.common.ui.BaseActivity;
import com.zego.common.util.AppLogger;
import com.zego.zegoliveroom.callback.IZegoLivePlayerCallback;
import com.zego.zegoliveroom.callback.IZegoLoginCompletionCallback;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.entity.ZegoPlayStreamQuality;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;

public class ZGAudioPlayerPlayUI extends BaseActivity {

    private ActivityZgaudioPlayerPlayBinding binding;

    private String mRoomID;
    private String mStreamID;

    private boolean isPlaying = false;

    private String showText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_zgaudio_player_play);

        mRoomID = getIntent().getStringExtra("roomID");
        mStreamID = getIntent().getStringExtra("streamID");

        binding.showRoomID.setText(getString(R.string.tx_show_roomID, mRoomID));
        binding.showStreamID.setText(getString(R.string.tx_show_streamID, mStreamID));

        // 返回键处理
        binding.goBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // 设置拉流回调监听
        ZGBaseHelper.sharedInstance().getZegoLiveRoom().setZegoLivePlayerCallback(playerCallback);

        showText += getString(R.string.tx_login_room) + "\n";
        binding.showTx.setText(showText);
        // 登录房间
        ZGBaseHelper.sharedInstance().getZegoLiveRoom().loginRoom(mRoomID, ZegoConstants.RoomRole.Audience, new IZegoLoginCompletionCallback() {
            @Override
            public void onLoginCompletion(int err, ZegoStreamInfo[] zegoStreamInfos) {
                if (err == 0) {

                    showText += getString(R.string.tx_login_room_success) + "\n";
                    binding.showTx.setText(showText);
                    // 开始拉流
                    ZGBaseHelper.sharedInstance().getZegoLiveRoom().startPlayingStream(zegoStreamInfos[0].streamID, null);

                } else {
                    showText += getString(R.string.tx_login_fail_hint, mRoomID, err) + "\n";
                    binding.showTx.setText(showText);
                    AppLogger.getInstance().e(ZGAudioPlayerPlayUI.class, getString(R.string.tx_login_fail_hint, mRoomID, err));
                }
            }
        });

    }

    @Override
    public void finish() {
        super.finish();
        ZGBaseHelper.sharedInstance().getZegoLiveRoom().setZegoLivePlayerCallback(null);
        if (isPlaying) {
            ZGBaseHelper.sharedInstance().getZegoLiveRoom().stopPlayingStream(mStreamID);
        }
        ZGBaseHelper.sharedInstance().loginOutRoom();

    }

    public static void actionStart(Activity activity, String roomID, String streamID) {
        Intent intent = new Intent(activity, ZGAudioPlayerPlayUI.class);
        intent.putExtra("roomID", roomID);
        intent.putExtra("streamID", streamID);
        activity.startActivity(intent);
    }

    private IZegoLivePlayerCallback playerCallback = new IZegoLivePlayerCallback() {
        @Override
        public void onPlayStateUpdate(int err, String streamID) {
            if (err != 0) {
                showText += getString(R.string.tx_play_fail_hint, streamID, err) + "\n";
                binding.showTx.setText(showText);
                AppLogger.getInstance().e(ZGAudioPlayerPlayUI.class, getString(R.string.tx_play_fail_hint, streamID, err));
            } else {
                isPlaying = true;
                binding.title.setTitleName(getString(R.string.tx_playing));
                showText += getString(R.string.tx_play_success) + "\n";
                binding.showTx.setText(showText);
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
    };
}
